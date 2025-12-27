package com.mcserver.launcher.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Java 環境檢查器
 * 負責檢測系統 Java 版本是否滿足要求 (Java 21)
 */
public class JavaChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaChecker.class);
    private static final int REQUIRED_JAVA_VERSION = 21;
    private static final String JAVA_DOWNLOAD_URL = "https://www.oracle.com/tw/java/technologies/downloads/";
    
    /**
     * 檢查 Java 版本是否滿足要求
     * @return 檢查結果
     */
    public static CompletableFuture<Boolean> checkJavaVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 獲取當前 Java 版本
                String version = System.getProperty("java.specification.version");
                logger.info("檢測到 Java 版本: {}", version);
                
                int majorVersion;
                try {
                    majorVersion = Integer.parseInt(version);
                } catch (NumberFormatException e) {
                    // 處理 1.8 這種格式
                    if (version.startsWith("1.")) {
                        majorVersion = Integer.parseInt(version.substring(2));
                    } else {
                        majorVersion = 0;
                    }
                }
                
                if (majorVersion < REQUIRED_JAVA_VERSION) {
                    logger.warn("Java 版本過低: {} < {}", majorVersion, REQUIRED_JAVA_VERSION);
                    return false;
                }
                
                return true;
            } catch (Exception e) {
                logger.error("檢查 Java 版本失敗", e);
                return false;
            }
        });
    }
    
    /**
     * 顯示 Java 版本不符的警告視窗
     */
    public static void showJavaUpdateAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Java 版本不符");
            alert.setHeaderText("需要 Java " + REQUIRED_JAVA_VERSION + " 或更高版本");
            alert.setContentText("檢測到您的 Java 版本過舊，無法啟動 Minecraft 1.21.1。\n" +
                    "請點擊「下載」前往官網更新 Java。");
            
            ButtonType downloadBtn = new ButtonType("下載 Java " + REQUIRED_JAVA_VERSION);
            alert.getButtonTypes().setAll(downloadBtn, ButtonType.CANCEL);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == downloadBtn) {
                openDownloadPage();
            }
        });
    }
    
    private static void openDownloadPage() {
        try {
            Desktop.getDesktop().browse(URI.create(JAVA_DOWNLOAD_URL));
        } catch (IOException e) {
            logger.error("無法打開瀏覽器", e);
        }
    }
}
