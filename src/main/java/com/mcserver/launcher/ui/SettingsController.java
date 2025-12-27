package com.mcserver.launcher.ui;

import com.mcserver.launcher.config.LauncherConfig;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 設定對話框控制器
 * 處理啟動器的各種設定選項
 */
public class SettingsController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    
    // 遊戲設定
    @FXML private TextField minecraftDirField;
    @FXML private Button browseMcDirButton;
    @FXML private TextField javaPathField;
    @FXML private Button browseJavaButton;
    @FXML private TextField javaArgsField;
    @FXML private Spinner<Integer> memoryMinSpinner;
    @FXML private Spinner<Integer> memoryMaxSpinner;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private CheckBox fullscreenCheckBox;
    
    // 啟動器設定
    @FXML private CheckBox autoLoginCheckBox;
    @FXML private CheckBox checkUpdatesCheckBox;
    @FXML private CheckBox autoLoadModsCheckBox;
    @FXML private TextField githubRepoField;
    
    // 按鈕
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    private LauncherConfig config;
    private Stage dialogStage;
    private boolean okClicked = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化設定對話框");
        
        setupSpinners();
        setupComboBoxes();
        setupEventHandlers();
    }
    
    /**
     * 設置 Spinner 控件
     */
    private void setupSpinners() {
        // 記憶體設定
        memoryMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(512, 8192, 1024, 256));
        memoryMaxSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1024, 16384, 4096, 512));
    }
    
    /**
     * 設置 ComboBox 選項
     */
    private void setupComboBoxes() {
        // 解析度選項
        resolutionComboBox.setItems(FXCollections.observableArrayList(
            "854x480", "1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440", "3840x2160", "自動"
        ));
        resolutionComboBox.setValue("1920x1080");
    }
    
    /**
     * 設置事件處理器
     */
    private void setupEventHandlers() {
        browseMcDirButton.setOnAction(e -> handleBrowseMinecraftDir());
        browseJavaButton.setOnAction(e -> handleBrowseJavaPath());
        resetButton.setOnAction(e -> handleReset());
        cancelButton.setOnAction(e -> handleCancel());
        saveButton.setOnAction(e -> handleSave());
    }
    
    /**
     * 設置配置對象
     */
    public void setConfig(LauncherConfig config) {
        this.config = config;
        loadConfigValues();
    }
    
    /**
     * 設置對話框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * 載入配置值到 UI
     */
    private void loadConfigValues() {
        if (config == null) return;
        
        // 遊戲設定
        minecraftDirField.setText(config.getMinecraftDirectory());
        javaPathField.setText(config.getJavaPath());
        javaArgsField.setText(config.getJavaArgs());
        memoryMinSpinner.getValueFactory().setValue(config.getMemoryMin());
        memoryMaxSpinner.getValueFactory().setValue(config.getMemoryMax());
        resolutionComboBox.setValue(config.getGameResolution());
        fullscreenCheckBox.setSelected(config.isFullscreen());
        
        // 啟動器設定
        autoLoginCheckBox.setSelected(config.isAutoLogin());
        checkUpdatesCheckBox.setSelected(config.isCheckUpdates());
        autoLoadModsCheckBox.setSelected(config.isAutoLoadMods());
        githubRepoField.setText(config.getGithubRepo());
    }
    
    /**
     * 瀏覽 Minecraft 目錄
     */
    @FXML
    private void handleBrowseMinecraftDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("選擇 Minecraft 目錄");
        
        String currentDir = minecraftDirField.getText();
        if (currentDir != null && !currentDir.isEmpty()) {
            File initialDir = new File(currentDir);
            if (initialDir.exists()) {
                chooser.setInitialDirectory(initialDir);
            }
        }
        
        File selectedDir = chooser.showDialog(dialogStage);
        if (selectedDir != null) {
            minecraftDirField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    /**
     * 瀏覽 Java 路徑
     */
    @FXML
    private void handleBrowseJavaPath() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("選擇 Java 執行檔");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Java 執行檔", "java.exe", "javaw.exe")
        );
        
        String currentPath = javaPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File initialFile = new File(currentPath);
            if (initialFile.exists()) {
                chooser.setInitialDirectory(initialFile.getParentFile());
            }
        }
        
        File selectedFile = chooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            javaPathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * 重設為預設值
     */
    @FXML
    private void handleReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認重設");
        alert.setHeaderText("重設所有設定");
        alert.setContentText("確定要將所有設定重設為預設值嗎？");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            resetToDefaults();
        }
    }
    
    /**
     * 重設為預設值
     */
    private void resetToDefaults() {
        // 遊戲設定
        minecraftDirField.setText(System.getProperty("user.home") + "/.minecraft");
        javaPathField.setText("java");
        javaArgsField.setText("-Xmx4G -XX:+UseG1GC");
        memoryMinSpinner.getValueFactory().setValue(1024);
        memoryMaxSpinner.getValueFactory().setValue(4096);
        resolutionComboBox.setValue("1920x1080");
        fullscreenCheckBox.setSelected(false);
        
        // 啟動器設定
        autoLoginCheckBox.setSelected(false);
        checkUpdatesCheckBox.setSelected(true);
        autoLoadModsCheckBox.setSelected(true);
        githubRepoField.setText("ken-aa-cpu/minecraft-launcher");
    }
    
    /**
     * 取消設定
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
    
    /**
     * 儲存設定
     */
    @FXML
    private void handleSave() {
        if (validateInput()) {
            saveConfigValues();
            okClicked = true;
            dialogStage.close();
        }
    }
    
    /**
     * 驗證輸入
     */
    private boolean validateInput() {
        String errorMessage = "";
        
        // 檢查 Minecraft 目錄
        if (minecraftDirField.getText() == null || minecraftDirField.getText().trim().isEmpty()) {
            errorMessage += "請選擇 Minecraft 目錄！\n";
        }
        
        // 檢查 Java 路徑
        if (javaPathField.getText() == null || javaPathField.getText().trim().isEmpty()) {
            errorMessage += "請選擇 Java 路徑！\n";
        }
        
        // 檢查記憶體設定
        int minMemory = memoryMinSpinner.getValue();
        int maxMemory = memoryMaxSpinner.getValue();
        if (minMemory >= maxMemory) {
            errorMessage += "最大記憶體必須大於最小記憶體！\n";
        }
        
        if (errorMessage.length() == 0) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("設定錯誤");
            alert.setHeaderText("請修正以下錯誤：");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
    
    /**
     * 儲存配置值
     */
    private void saveConfigValues() {
        if (config == null) return;
        
        // 遊戲設定
        config.setMinecraftDirectory(minecraftDirField.getText().trim());
        config.setJavaPath(javaPathField.getText().trim());
        config.setJavaArgs(javaArgsField.getText().trim());
        config.setMemoryMin(memoryMinSpinner.getValue());
        config.setMemoryMax(memoryMaxSpinner.getValue());
        config.setGameResolution(resolutionComboBox.getValue());
        config.setFullscreen(fullscreenCheckBox.isSelected());
        
        // 啟動器設定
        config.setAutoLogin(autoLoginCheckBox.isSelected());
        config.setCheckUpdates(checkUpdatesCheckBox.isSelected());
        config.setAutoLoadMods(autoLoadModsCheckBox.isSelected());
        if (githubRepoField.getText() != null && !githubRepoField.getText().trim().isEmpty()) {
            config.setGithubRepo(githubRepoField.getText().trim());
        }
        
        // 儲存到文件
        config.save();
        
        logger.info("設定已儲存");
    }
    
    /**
     * 檢查是否點擊了確定按鈕
     */
    public boolean isOkClicked() {
        return okClicked;
    }
}