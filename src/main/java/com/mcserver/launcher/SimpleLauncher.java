package com.mcserver.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 簡化版Minecraft啟動器
 * 包含下載Minecraft和Forge的基本功能
 */
public class SimpleLauncher extends Application {
    
    private static final String MINECRAFT_VERSION = "1.20.1";
    private static final String FORGE_VERSION = "47.2.0"; // 1.20.1 推薦版本
    private static final String MINECRAFT_DIR = System.getProperty("user.home") + "/.minecraft";
    
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextArea logArea;
    private Button downloadButton;
    private Button launchButton;
    
    @Override
    public void start(Stage primaryStage) {
        // 創建UI組件
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("清藝世界 Minecraft 啟動器");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        statusLabel = new Label("就緒");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);
        
        // 按鈕區域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        downloadButton = new Button("下載遊戲");
        downloadButton.setPrefWidth(120);
        downloadButton.setPrefHeight(40);
        downloadButton.setOnAction(e -> handleDownload());
        
        launchButton = new Button("啟動遊戲");
        launchButton.setPrefWidth(120);
        launchButton.setPrefHeight(50);
        launchButton.setOnAction(e -> handleLaunch());
        
        buttonBox.getChildren().addAll(downloadButton, launchButton);
        
        // 日誌區域
        logArea = new TextArea();
        logArea.setPrefWidth(500);
        logArea.setPrefHeight(200);
        logArea.setEditable(false);
        logArea.setWrapText(true);
        
        root.getChildren().addAll(titleLabel, statusLabel, progressBar, buttonBox, logArea);
        
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("清藝世界 Minecraft 啟動器 - v1.0.0");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
        
        appendLog("啟動器已啟動");
        checkGameFiles();
    }
    
    private void handleDownload() {
        appendLog("開始下載Minecraft和Forge...");
        setUIEnabled(false);
        progressBar.setVisible(true);
        
        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                updateMessage("準備下載...");
                
                // 創建.minecraft目錄
                createMinecraftDirectory();
                updateProgress(5, 100);
                updateMessage("創建目錄完成");
                
                // 下載Minecraft客戶端
                updateMessage("下載Minecraft客戶端...");
                downloadMinecraftClient();
                updateProgress(40, 100);
                
                // 下載Forge安裝器
                updateMessage("下載Forge安裝器...");
                downloadForgeInstaller();
                updateProgress(70, 100);
                
                // 安裝Forge
                updateMessage("安裝Forge...");
                installForge();
                updateProgress(90, 100);
                
                // 創建啟動配置
                createLaunchProfile();
                updateProgress(100, 100);
                updateMessage("下載完成！");
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    appendLog("Minecraft和Forge下載完成！");
                    statusLabel.setText("下載完成");
                    setUIEnabled(true);
                    progressBar.setVisible(false);
                    checkGameFiles();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "未知錯誤";
                    appendLog("下載失敗: " + errorMsg);
                    statusLabel.setText("下載失敗");
                    setUIEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        };
        
        statusLabel.textProperty().bind(downloadTask.messageProperty());
        progressBar.progressProperty().bind(downloadTask.progressProperty());
        
        new Thread(downloadTask).start();
    }
    
    private void handleLaunch() {
        if (!isGameInstalled()) {
            appendLog("遊戲未安裝，請先下載遊戲文件");
            return;
        }
        
        appendLog("啟動Minecraft Forge " + MINECRAFT_VERSION + "...");
        
        Task<Void> launchTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("正在啟動遊戲...");
                
                String forgeProfile = MINECRAFT_VERSION + "-forge-" + FORGE_VERSION;
                String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                
                // 構建啟動命令
                ProcessBuilder pb = new ProcessBuilder(
                    javaPath,
                    "-Xmx2G",
                    "-Xms1G",
                    "-Djava.library.path=" + Paths.get(MINECRAFT_DIR, "versions", forgeProfile, "natives"),
                    "-cp", buildClasspath(forgeProfile),
                    "net.minecraftforge.fml.loading.FMLClientLaunchProvider",
                    "--username", "Player",
                    "--version", forgeProfile,
                    "--gameDir", MINECRAFT_DIR,
                    "--assetsDir", Paths.get(MINECRAFT_DIR, "assets").toString(),
                    "--assetIndex", "1.20",
                    "--uuid", "00000000-0000-0000-0000-000000000000",
                    "--accessToken", "0",
                    "--userType", "legacy"
                );
                
                pb.directory(new File(MINECRAFT_DIR));
                Process gameProcess = pb.start();
                
                // 讀取遊戲輸出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(gameProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && gameProcess.isAlive()) {
                        final String logLine = line;
                        Platform.runLater(() -> appendLog("[遊戲] " + logLine));
                    }
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    appendLog("遊戲啟動成功！");
                    statusLabel.setText("遊戲運行中");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "未知錯誤";
                    appendLog("遊戲啟動失敗: " + errorMsg);
                    statusLabel.setText("啟動失敗");
                });
            }
        };
        
        statusLabel.textProperty().bind(launchTask.messageProperty());
        new Thread(launchTask).start();
    }
    
    private String buildClasspath(String forgeProfile) {
        StringBuilder classpath = new StringBuilder();
        
        // 添加Minecraft客戶端JAR
        classpath.append(Paths.get(MINECRAFT_DIR, "versions", MINECRAFT_VERSION, MINECRAFT_VERSION + ".jar"));
        classpath.append(File.pathSeparator);
        
        // 添加Forge JAR
        classpath.append(Paths.get(MINECRAFT_DIR, "versions", forgeProfile, forgeProfile + ".jar"));
        classpath.append(File.pathSeparator);
        
        // 添加libraries目錄下的所有JAR文件
        Path librariesDir = Paths.get(MINECRAFT_DIR, "libraries");
        if (Files.exists(librariesDir)) {
            try {
                Files.walk(librariesDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(path -> {
                        classpath.append(path.toString());
                        classpath.append(File.pathSeparator);
                    });
            } catch (IOException e) {
                appendLog("構建classpath時出錯: " + e.getMessage());
            }
        }
        
        return classpath.toString();
    }
    
    private void createMinecraftDirectory() {
        try {
            Path minecraftPath = Paths.get(MINECRAFT_DIR);
            Files.createDirectories(minecraftPath);
            Files.createDirectories(minecraftPath.resolve("versions"));
            Files.createDirectories(minecraftPath.resolve("libraries"));
            Files.createDirectories(minecraftPath.resolve("assets"));
            appendLog("創建Minecraft目錄: " + MINECRAFT_DIR);
        } catch (IOException e) {
            appendLog("創建目錄失敗: " + e.getMessage());
        }
    }
    
    private void downloadMinecraftClient() throws Exception {
        String clientUrl = "https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar";
        Path clientPath = Paths.get(MINECRAFT_DIR, "versions", MINECRAFT_VERSION, MINECRAFT_VERSION + ".jar");
        Files.createDirectories(clientPath.getParent());
        
        downloadFile(clientUrl, clientPath.toFile());
        appendLog("Minecraft客戶端下載完成: " + clientPath);
        
        // 創建版本JSON
        String versionJson = "{\"id\":\"" + MINECRAFT_VERSION + "\",\"type\":\"release\",\"mainClass\":\"net.minecraft.client.main.Main\"}";
        Files.write(clientPath.getParent().resolve(MINECRAFT_VERSION + ".json"), versionJson.getBytes());
    }
    
    private void downloadForgeInstaller() throws Exception {
        String forgeUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + MINECRAFT_VERSION + "-" + FORGE_VERSION + "/forge-" + MINECRAFT_VERSION + "-" + FORGE_VERSION + "-installer.jar";
        Path installerPath = Paths.get(MINECRAFT_DIR, "forge-installer.jar");
        
        downloadFile(forgeUrl, installerPath.toFile());
        appendLog("Forge安裝器下載完成: " + installerPath);
    }
    
    private void installForge() throws Exception {
        Path installerPath = Paths.get(MINECRAFT_DIR, "forge-installer.jar");
        if (!Files.exists(installerPath)) {
            throw new IOException("Forge安裝器不存在");
        }
        
        // 執行Forge安裝器
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", installerPath.toString(), "--installClient", "--mcDir", MINECRAFT_DIR
        );
        pb.directory(new File(MINECRAFT_DIR));
        Process process = pb.start();
        
        // 等待安裝完成
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            appendLog("Forge安裝成功");
            // 刪除安裝器
            Files.deleteIfExists(installerPath);
        } else {
            throw new RuntimeException("Forge安裝失敗，退出代碼: " + exitCode);
        }
    }
    
    private void createLaunchProfile() throws Exception {
        Path profilesPath = Paths.get(MINECRAFT_DIR, "launcher_profiles.json");
        String forgeProfile = MINECRAFT_VERSION + "-forge-" + FORGE_VERSION;
        
        String profilesJson = "{\"profiles\":{\"forge\":{\"name\":\"Forge\",\"type\":\"custom\",\"lastVersionId\":\"" + forgeProfile + "\"}},\"selectedProfile\":\"forge\"}";
        Files.write(profilesPath, profilesJson.getBytes());
        appendLog("啟動配置創建完成");
    }
    
    private void downloadFile(String urlString, File destination) throws Exception {
        URL url = new URL(urlString);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(destination)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    private boolean isGameInstalled() {
        // 檢查Minecraft客戶端
        Path minecraftJar = Paths.get(MINECRAFT_DIR, "versions", MINECRAFT_VERSION, MINECRAFT_VERSION + ".jar");
        Path minecraftJson = Paths.get(MINECRAFT_DIR, "versions", MINECRAFT_VERSION, MINECRAFT_VERSION + ".json");
        
        // 檢查Forge版本
        String forgeProfile = MINECRAFT_VERSION + "-forge-" + FORGE_VERSION;
        Path forgeDir = Paths.get(MINECRAFT_DIR, "versions", forgeProfile);
        Path forgeJson = forgeDir.resolve(forgeProfile + ".json");
        
        return Files.exists(minecraftJar) && Files.exists(minecraftJson) && 
               Files.exists(forgeDir) && Files.exists(forgeJson);
    }
    
    private void checkGameFiles() {
        if (isGameInstalled()) {
            appendLog("檢測到已安裝的遊戲文件");
            statusLabel.setText("遊戲已安裝");
        } else {
            appendLog("未檢測到遊戲文件，請點擊下載按鈕");
            statusLabel.setText("需要下載遊戲");
        }
    }
    
    private void setUIEnabled(boolean enabled) {
        downloadButton.setDisable(!enabled);
        launchButton.setDisable(!enabled);
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}