package com.mcserver.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.mcserver.launcher.config.LauncherConfig;


/**
 * Minecraft Forge 1.20.1 啟動器主應用程序
 * 支持微軟帳號登入、GitHub 自動更新、模組管理
 */
public class LauncherApplication extends Application {
    

    private static final String APP_TITLE = "清藝世界 Minecraft 啟動器";
    private static final String VERSION = "1.0.0";
    
    private Stage primaryStage;
    private LauncherConfig config;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.config = new LauncherConfig();
        
        try {
            // 載入 FXML 文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/launcher.fxml"));
            Parent root = loader.load();
            
            // 獲取控制器並設置應用程序引用
            LauncherController controller = loader.getController();
            if (controller != null) {
                controller.setApplication(this);
            }
            
            // 創建場景 (16:9 比例)
            Scene scene = new Scene(root, 1280, 720);
            
            // 載入 CSS 樣式
            String cssPath = getClass().getResource("/launcher.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            
            stage.setTitle("清藝世界模組啟動器");
            stage.setScene(scene);
            stage.setResizable(false);
            
            // 設置固定大小為16:9比例，並確保視窗置頂顯示
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.setAlwaysOnTop(true);
            stage.show();
            stage.toFront();
            stage.requestFocus();
            Platform.runLater(() -> stage.setAlwaysOnTop(false));
            
            System.out.println("應用程序啟動成功");
            
        } catch (Exception e) {
            System.err.println("啟動應用程序時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public void openLink(String url) {
        getHostServices().showDocument(url);
    }
    
    public LauncherConfig getConfig() {
        return config;
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    public static void main(String[] args) {
        // 設置系統屬性
        System.setProperty("file.encoding", "UTF-8");
        
        // 啟動 JavaFX 應用程序
        launch(args);
    }
}