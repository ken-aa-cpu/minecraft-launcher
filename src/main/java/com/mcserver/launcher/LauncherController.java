package com.mcserver.launcher;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mcserver.launcher.auth.MicrosoftAuthenticator;
import com.mcserver.launcher.config.LauncherConfig;
import com.mcserver.launcher.download.DownloadManager;
import com.mcserver.launcher.github.ModSyncManager;
import com.mcserver.launcher.github.UpdateManager;
import com.mcserver.launcher.ui.SettingsController;
import com.mcserver.launcher.util.JavaChecker;

/**
 * 啟動器主控制器
 * 處理用戶界面邏輯和各種功能模組的協調
 */
public class LauncherController implements Initializable {
    
    // FXML 注入的 UI 元件
    @FXML private VBox rootContainer;
    @FXML private ImageView backgroundImage;
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button startGameButton;
    @FXML private Button downloadButton;
    @FXML private Button settingsButton;
    @FXML private Button modsButton;
    @FXML private Button discordButton;
    // 移除：@FXML private ComboBox<String> memorySelect;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea logArea;
    @FXML private ListView<String> modsList;
    @FXML private Label versionLabel;
    
    // 公告區域 UI
    @FXML private Label newsTitleLabel;
    @FXML private Label newsContentLabel;
    
    // 應用程序引用
    private LauncherApplication application;
    
    // 狀態變量
    private boolean isLoggedIn = false;
    private String currentUser = null;
    private MicrosoftAuthenticator authenticator;
    private ModSyncManager modSyncManager;
    private UpdateManager updateManager;
    private DownloadManager downloadManager;
    private boolean isGameRunning = false; // 遊戲運行狀態標記
    
    /**
     * 設置應用程序引用
     */
    public void setApplication(LauncherApplication application) {
        this.application = application;
        this.modSyncManager = new ModSyncManager(application.getConfig());
        this.updateManager = new UpdateManager(application.getConfig());
        this.downloadManager = new DownloadManager(application.getConfig());
        
        // 0. 檢查 GitHub 配置
        String repo = application.getConfig().getGithubRepo();
        if (repo == null || repo.contains("YOUR_USERNAME")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("配置警告");
                alert.setHeaderText("GitHub 倉庫未配置");
                alert.setContentText("請在設定中配置正確的 GitHub 倉庫 (用戶名/倉庫名)，否則無法檢查更新。");
                alert.showAndWait();
            });
        }
        
        // 1. 檢查 Java 環境
        checkJavaEnvironment();
        
        // 2. 檢查啟動器更新
        if (application.getConfig().isCheckUpdates()) {
            checkForLauncherUpdates();
        }
        
        // 3. 執行模組同步
        if (application.getConfig().isAutoLoadMods()) {
            performModSync();
        }
        
        // 初始化認證器
        try {
            this.authenticator = new MicrosoftAuthenticator(application.getConfig());
            
            // 檢查是否已經有保存的登入狀態
            if (authenticator.isLoggedIn()) {
                MicrosoftAuthenticator.MinecraftSession session = authenticator.getSession();
                isLoggedIn = true;
                currentUser = session.getUsername();
                loginButton.setText("登出");
                appendLog("已恢復上次會話: " + currentUser);
                updateUIState();
                
                // 嘗試在後台刷新 Token (自動登入)
                new Thread(() -> {
                    try {
                        appendLog("正在驗證登入狀態...");
                        String refreshedUser = authenticator.refreshAndAuthenticate();
                        Platform.runLater(() -> {
                            currentUser = refreshedUser;
                            appendLog("自動登入驗證成功: " + refreshedUser);
                            updateUIState();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            appendLog("自動登入失效，請重新登入: " + e.getMessage());
                            handleLogout();
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("認證服務初始化失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkJavaEnvironment() {
        JavaChecker.checkJavaVersion().thenAccept(isValid -> {
            if (!isValid) {
                JavaChecker.showJavaUpdateAlert();
            }
        });
    }
    
    private void checkForLauncherUpdates() {
        if (updateManager == null) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                if (updateManager.checkForUpdates()) {
                    UpdateManager.GitHubRelease release = updateManager.getLatestRelease();
                    Platform.runLater(() -> showUpdateDialog(release));
                }
            } catch (Exception e) {
                Platform.runLater(() -> appendLog("檢查更新失敗: " + e.getMessage()));
            }
        });
    }
    
    private void showUpdateDialog(UpdateManager.GitHubRelease release) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("發現新版本");
        alert.setHeaderText("發現新版本: " + release.tagName);
        alert.setContentText("更新內容:\n" + release.body + "\n\n是否立即更新？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performLauncherUpdate(release);
            }
        });
    }
    
    private void performLauncherUpdate(UpdateManager.GitHubRelease release) {
        lockUI(true, "正在更新啟動器...");
        updateManager.downloadAndApplyUpdate(release, progress -> {
            Platform.runLater(() -> {
                if (progressBar != null) progressBar.setProgress(progress / 100.0);
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                lockUI(false, "更新失敗");
                showError("更新失敗", e.getMessage());
            });
            return null;
        });
    }
    
    private void performModSync() {
        if (isGameRunning) {
            showError("無法更新", "遊戲正在運行中，請先關閉遊戲再重啟啟動器以進行更新。");
            return;
        }
        
        // 鎖定 UI
        lockUI(true, "正在檢查更新...");
        
        modSyncManager.syncAll(
            status -> Platform.runLater(() -> appendLog(status)),
            progress -> Platform.runLater(() -> {
                if (progressBar != null) progressBar.setProgress(progress / 100.0);
            })
        ).thenRun(() -> {
            Platform.runLater(() -> {
                appendLog("更新完成！");
                lockUI(false, "準備開始");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                appendLog("更新失敗: " + ex.getMessage());
                showError("更新失敗", "無法同步模組文件: " + ex.getMessage());
                lockUI(false, "更新失敗");
            });
            return null;
        });
    }
    
    private void lockUI(boolean locked, String statusText) {
        if (rootContainer != null) rootContainer.setDisable(locked);
        if (statusLabel != null) statusLabel.setText(statusText);
        
        // 確保進度條和日誌區域始終可見
        if (progressBar != null) {
            progressBar.setVisible(locked);
            progressBar.setDisable(false); // 進度條不應該被禁用
        }
        
        // 我們只禁用交互按鈕，而不是整個容器，以免看不到進度
        if (loginButton != null) loginButton.setDisable(locked);
        if (startGameButton != null) startGameButton.setDisable(locked || !isLoggedIn);
        if (downloadButton != null) downloadButton.setDisable(locked);
        if (settingsButton != null) settingsButton.setDisable(locked);
        if (modsButton != null) modsButton.setDisable(locked);
    }
    
    private void openUrl(String url) {
        try {
            if (application != null) {
                application.openLink(url);
            } else {
                // Fallback for when application reference is missing or fails
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                }
            }
        } catch (Exception e) {
            appendLog("無法打開連結: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupUI();
            setupEventHandlers();
            loadBackgroundImage();
            updateUIState();

            appendLog("啟動器初始化完成");
            appendLog("遊戲目錄: " + application.getConfig().getMinecraftDirectory());
        } catch (Exception e) {
            System.err.println("初始化失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupUI() {
        // 設置歡迎標籤
        if (welcomeLabel != null) {
            welcomeLabel.setText("歡迎來到 Minecraft 啟動器");
        }
        
        // 設置狀態標籤
        if (statusLabel != null) {
            statusLabel.setText("準備開始");
        }
        
        // 設置版本標籤
        if (versionLabel != null) {
            versionLabel.setText("版本 1.20.1");
        }
        
        // 設置記憶體配置下拉框
        // 移除：
        // if (memorySelect != null) {
        //     memorySelect.getItems().addAll("2GB", "4GB", "6GB", "8GB", "12GB", "16GB");
        //     memorySelect.setValue("4GB");
        // }
    }
    
    private void loadBackgroundImage() {
        if (backgroundImage != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/background.jpg"));
                backgroundImage.setImage(image);
                
                // 綁定圖片大小到父容器大小
                if (rootContainer != null) {
                    backgroundImage.fitWidthProperty().bind(rootContainer.widthProperty());
                    backgroundImage.fitHeightProperty().bind(rootContainer.heightProperty());
                }
            } catch (Exception e) {
                System.err.println("無法載入背景圖片: " + e.getMessage());
            }
        }
    }
    
    private void setupEventHandlers() {
        // 登入按鈕事件
        if (loginButton != null) {
            loginButton.setOnAction(e -> handleLogin());
        }
        
        // 啟動遊戲按鈕事件
        if (startGameButton != null) {
            startGameButton.setOnAction(e -> handleLaunchGame());
        }
        
        // 下載按鈕事件
        if (downloadButton != null) {
            downloadButton.setOnAction(e -> handleDownload());
        }
        
        // 設定按鈕事件
        if (settingsButton != null) {
            settingsButton.setOnAction(e -> handleSettings());
        }
        
        // 模組管理按鈕事件
        if (modsButton != null) {
            modsButton.setOnAction(e -> handleModsManagement());
        }
    }
    
    @FXML
    private void handleLogin() {
        if (!isLoggedIn) {
            if (authenticator == null) {
                showError("錯誤", "認證服務尚未初始化");
                return;
            }

            loginButton.setDisable(true);
            appendLog("正在啟動微軟登入...");
            
            // 在后台线程运行认证，避免阻塞 UI
            new Thread(() -> {
                try {
                    String userName = authenticator.authenticate();
                    
                    Platform.runLater(() -> {
                        isLoggedIn = true;
                        currentUser = userName;
                        loginButton.setText("登出");
                        loginButton.setDisable(false);
                        appendLog("登入成功: " + currentUser);
                        updateUIState();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        appendLog("登入失敗: " + e.getMessage());
                        showError("登入失敗", e.getMessage());
                        e.printStackTrace();
                    });
                }
            }).start();
        } else {
            handleLogout();
        }
    }
    
    private void handleLogout() {
        if (authenticator != null) {
            authenticator.logout();
        }
        isLoggedIn = false;
        currentUser = null;
        loginButton.setText("登入");
        appendLog("已登出");
        updateUIState();
    }
    
    @FXML
    private void handleLaunchGame() {
        if (!isLoggedIn) {
            showError("錯誤", "請先登入");
            return;
        }
        
        appendLog("正在啟動遊戲...");
        
        // 構建啟動參數 (模擬)
        if (authenticator != null && authenticator.getSession() != null) {
            MicrosoftAuthenticator.MinecraftSession session = authenticator.getSession();
            String launchArgs = String.format("--accessToken %s --uuid %s --username %s", 
                session.getAccessToken(), session.getUuid(), session.getUsername());
            
            appendLog("=== 遊戲啟動參數 (Launch Arguments) ===");
            appendLog(launchArgs);
            appendLog("=====================================");
            appendLog("注意：這證明我們已經拿到了所有需要的 Token！");
        }

        // 模擬遊戲啟動過程
        Platform.runLater(() -> {
            try {
                // 模擬啟動延遲
                Thread.sleep(2000);
                appendLog("遊戲啟動成功！(模擬)");
            } catch (InterruptedException e) {
                appendLog("遊戲啟動被中斷");
            }
        });
    }
    
    @FXML
    private void handleDownload() {
        if (downloadManager == null) {
            showError("錯誤", "下載管理器尚未初始化");
            return;
        }

        if (isGameRunning) {
            showError("錯誤", "遊戲正在運行中");
            return;
        }

        lockUI(true, "正在下載遊戲文件...");
        appendLog("開始下載遊戲文件 (1.21.1 / NeoForge)...");
        
        // 這些版本號應該與 MinecraftLauncher 中的保持一致
        String mcVersion = "1.21.1";
        String neoForgeVersion = "21.1.217"; // 使用與 MinecraftLauncher 一致的版本

        downloadManager.downloadAndInstallMinecraftForge(mcVersion, neoForgeVersion, (current, total) -> {
             Platform.runLater(() -> {
                 if (progressBar != null) {
                     progressBar.setProgress((double)current / total);
                 }
                 // 可以選擇是否在日誌中顯示詳細進度，這裡選擇不顯示以免刷屏
             });
        }).thenAccept(success -> {
             Platform.runLater(() -> {
                 lockUI(false, success ? "下載完成" : "下載失敗");
                 if (success) {
                     if (progressBar != null) progressBar.setProgress(1.0);
                     appendLog("下載與安裝完成！");
                     showInfo("成功", "Minecraft " + mcVersion + " 與 NeoForge 安裝完成！");
                 } else {
                     if (progressBar != null) progressBar.setProgress(0);
                     appendLog("下載或安裝失敗，請檢查日誌。");
                     showError("失敗", "下載或安裝過程中發生錯誤。");
                 }
             });
        });
    }
    
    @FXML
    private void handleSettings() {
        appendLog("打開設定面板");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/settings.fxml"));
            Parent dialogRoot = loader.load();
            
            SettingsController controller = loader.getController();
            if (controller != null && application != null) {
                LauncherConfig config = application.getConfig();
                controller.setConfig(config);
            }
            
            Stage dialogStage = new Stage();
            dialogStage.initOwner(application != null ? application.getPrimaryStage() : null);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setTitle("設定");
            
            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            // 將 Stage 傳入控制器以支持檔案選擇器
            if (controller != null) {
                controller.setDialogStage(dialogStage);
            }
            
            dialogStage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
            showError("設定錯誤", "無法開啟設定視窗: " + ex.getMessage());
        }
    }
    
    @FXML
    private void handleModsManagement() {
        appendLog("打開模組管理");
        showInfo("模組管理", "模組管理功能尚未實現");
    }

    @FXML
    private void handleDiscord() {
        appendLog("開啟 Discord 連結...");
        String discordUrl = "https://discord.gg/aQVjDNG5Fv";
        
        try {
            // Check if Desktop is supported (Standard Java feature)
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(discordUrl));
            } else {
                // Fallback for some Linux environments or if Desktop is not supported
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                     Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + discordUrl);
                } else {
                     Runtime.getRuntime().exec(new String[]{"xdg-open", discordUrl});
                }
            }
        } catch (Exception e) {
            appendLog("無法開啟 Discord 連結: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateUIState() {
        if (startGameButton != null) {
            startGameButton.setDisable(!isLoggedIn);
        }
        
        if (statusLabel != null) {
            if (isLoggedIn) {
                statusLabel.setText("已登入: " + currentUser);
            } else {
                statusLabel.setText("請登入");
            }
        }
    }
    
    private void appendLog(String message) {
        if (logArea != null) {
            Platform.runLater(() -> {
                logArea.appendText("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message + "\n");
            });
        }
        System.out.println(message);
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void shutdown() {
        appendLog("啟動器正在關閉...");
    }
}