package com.mcserver.launcher.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcserver.launcher.LauncherApplication;
import com.mcserver.launcher.config.LauncherConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * GitHub 更新管理器
 * 負責檢查新版本、下載更新和應用更新
 */
public class UpdateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateManager.class);
    
    // GitHub API Base URL
    private static final String GITHUB_API_BASE = "https://api.github.com/repos";
    private static final String GITHUB_BASE_URL = "https://github.com";
    
    private final LauncherConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    public UpdateManager(LauncherConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * 檢查是否有新版本可用
     * @return 如果有新版本返回 true
     */
    public boolean checkForUpdates() throws Exception {
        logger.info("檢查 GitHub 更新...");
        
        GitHubRelease latestRelease = getLatestRelease();
        if (latestRelease == null) {
            logger.warn("無法獲取最新版本信息");
            return false;
        }
        
        String currentVersion = LauncherApplication.getVersion();
        String latestVersion = latestRelease.tagName.replace("v", "");
        
        logger.info("當前版本: {}, 最新版本: {}", currentVersion, latestVersion);
        
        return isNewerVersion(currentVersion, latestVersion);
    }
    
    /**
     * 獲取最新版本信息
     */
    public GitHubRelease getLatestRelease() throws Exception {
        String repo = config.getGithubRepo();
        if (repo == null || repo.contains("YOUR_USERNAME")) {
            logger.warn("GitHub 倉庫未配置");
            return null;
        }

        String apiUrl = String.format("%s/%s/releases/latest", GITHUB_API_BASE, repo);
        logger.debug("Checking for updates from: {}", apiUrl);

        HttpGet get = new HttpGet(apiUrl);
        get.setHeader("Accept", "application/vnd.github.v3+json");
        get.setHeader("User-Agent", "Minecraft-Launcher/" + LauncherApplication.getVersion());
        
        try (var response = httpClient.execute(get)) {
            if (response.getCode() != 200) {
                if (response.getCode() == 404) {
                    logger.warn("找不到發布版本 (404)");
                    return null;
                }
                throw new Exception("GitHub API 請求失敗: " + response.getCode());
            }
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            return parseGitHubRelease(json);
        }
    }
    
    private GitHubRelease parseGitHubRelease(JsonNode json) {
        String tagName = json.get("tag_name").asText();
        String name = json.get("name").asText();
        String body = json.get("body").asText();
        boolean prerelease = json.get("prerelease").asBoolean();
        
        String downloadUrl = null;
        JsonNode assets = json.get("assets");
        if (assets != null && assets.isArray()) {
            for (JsonNode asset : assets) {
                String assetName = asset.get("name").asText();
                // 尋找 .jar 或 .exe 文件
                if (assetName.endsWith(".jar") || assetName.endsWith(".exe")) {
                    downloadUrl = asset.get("browser_download_url").asText();
                    break;
                }
            }
        }
        
        return new GitHubRelease(tagName, name, body, downloadUrl, prerelease);
    }
    
    /**
     * 下載並應用更新
     * @param release 要下載的版本
     * @param progressCallback 進度回調
     */
    public CompletableFuture<Void> downloadAndApplyUpdate(GitHubRelease release, Consumer<Integer> progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("開始下載更新: {}", release.tagName);
                
                if (release.downloadUrl == null || release.downloadUrl.isEmpty()) {
                    throw new Exception("沒有可用的下載連結");
                }
                
                // 創建臨時下載目錄
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "launcher-update");
                Files.createDirectories(tempDir);
                
                // 確定文件名
                String fileName = release.downloadUrl.substring(release.downloadUrl.lastIndexOf('/') + 1);
                Path downloadPath = tempDir.resolve(fileName);
                
                // 下載文件
                downloadFile(release.downloadUrl, downloadPath, progressCallback);
                
                // 應用更新
                applyUpdate(downloadPath);
                
                logger.info("更新完成: {}", release.tagName);
                
            } catch (Exception e) {
                logger.error("更新失敗", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    private void downloadFile(String url, Path destination, Consumer<Integer> progressCallback) throws Exception {
        logger.info("下載文件: {} -> {}", url, destination);
        
        URL downloadUrl = new URL(url);
        try (ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
             FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            
            // 獲取文件大小
            long fileSize = downloadUrl.openConnection().getContentLengthLong();
            
            // 下載文件並報告進度
            long bytesTransferred = 0;
            long chunkSize = 8192;
            
            while (bytesTransferred < fileSize) {
                long transferred = fos.getChannel().transferFrom(rbc, bytesTransferred, chunkSize);
                if (transferred == 0) break;
                
                bytesTransferred += transferred;
                
                if (progressCallback != null && fileSize > 0) {
                    int progress = (int) ((bytesTransferred * 100) / fileSize);
                    progressCallback.accept(progress);
                }
            }
        }
        
        logger.info("文件下載完成: {}", destination);
    }
    
    private void applyUpdate(Path updateFile) throws Exception {
        logger.info("應用更新: {}", updateFile);
        
        // 獲取當前 JAR 文件路徑
        String currentJarPath = getCurrentJarPath();
        if (currentJarPath == null) {
            throw new Exception("無法確定當前 JAR 文件路徑");
        }
        
        Path currentJar = Paths.get(currentJarPath);
        Path backupJar = Paths.get(currentJarPath + ".backup");
        
        // 創建備份
        Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
        logger.info("已創建備份: {}", backupJar);
        
        // 替換文件
        Files.copy(updateFile, currentJar, StandardCopyOption.REPLACE_EXISTING);
        logger.info("已替換文件: {}", currentJar);
        
        // 創建重啟腳本
        createRestartScript(currentJarPath);
        
        // 標記需要重啟
        config.setUpdatePending(true);
        config.save();
    }
    
    private String getCurrentJarPath() {
        try {
            return new File(UpdateManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
        } catch (Exception e) {
            logger.error("無法獲取當前 JAR 路徑", e);
            return null;
        }
    }
    
    private void createRestartScript(String jarPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Path scriptPath;
        String scriptContent;
        
        if (os.contains("win")) {
            // Windows 批次檔
            scriptPath = Paths.get("restart-launcher.bat");
            // 使用 start 命令異步啟動新進程，確保當前進程能順利退出
            // 嘗試檢測是否有 MCL.exe 包裝器
            File exeFile = new File("MCL.exe");
            String launchCmd = exeFile.exists() ? "start \"\" \"MCL.exe\"" : "start \"\" javaw -jar \"" + jarPath + "\"";
            
            scriptContent = "@echo off\r\n" +
                    "echo Waiting for launcher to exit...\r\n" +
                    "timeout /t 2 /nobreak > nul\r\n" +
                    "echo Restarting launcher...\r\n" +
                    launchCmd + "\r\n" +
                    "del \"%~f0\"\r\n" + 
                    "exit\r\n";
        } else {
            // Unix/Linux shell 腳本
            scriptPath = Paths.get("restart-launcher.sh");
            scriptContent = "#!/bin/bash\n" +
                    "echo 'Waiting for launcher to exit...'\n" +
                    "sleep 2\n" +
                    "echo 'Restarting launcher...'\n" +
                    "java -jar \"" + jarPath + "\" &\n" +
                    "rm \"$0\"\n";
        }
        
        Files.write(scriptPath, scriptContent.getBytes());
        
        // 設置執行權限 (Unix/Linux)
        if (!os.contains("win")) {
            scriptPath.toFile().setExecutable(true);
        }
        
        logger.info("已創建重啟腳本: {}", scriptPath);
        
        // 執行重啟腳本
        if (os.contains("win")) {
             Runtime.getRuntime().exec("cmd /c start \"\" \"" + scriptPath.toString() + "\"");
        } else {
             Runtime.getRuntime().exec(new String[]{"/bin/bash", scriptPath.toString()});
        }
        
        // 退出當前應用程序
        System.exit(0);
    }
    
    /**
     * 比較版本號
     * @param current 當前版本
     * @param latest 最新版本
     * @return 如果最新版本更新返回 true
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            int maxLength = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false; // 版本相同
        } catch (NumberFormatException e) {
            logger.warn("版本號格式錯誤，無法比較: {} vs {}", current, latest);
            return false;
        }
    }
    
    /**
     * 檢查模組更新
     * @return 有可用模組更新時返回 true
     */
    public boolean checkModUpdates() {
        // TODO: 實現模組更新檢查邏輯
        logger.info("檢查模組更新 (功能開發中)");
        return false;
    }
    
    /**
     * 下載模組更新
     */
    public CompletableFuture<Void> downloadModUpdates() {
        return CompletableFuture.runAsync(() -> {
            // TODO: 實現模組更新下載邏輯
            logger.info("下載模組更新 (功能開發中)");
        });
    }
    
    /**
     * GitHub 版本信息類
     */
    public static class GitHubRelease {
        public final String tagName;
        public final String name;
        public final String body;
        public final String downloadUrl;
        public final boolean prerelease;
        
        public GitHubRelease(String tagName, String name, String body, String downloadUrl, boolean prerelease) {
            this.tagName = tagName;
            this.name = name;
            this.body = body;
            this.downloadUrl = downloadUrl;
            this.prerelease = prerelease;
        }
        
        @Override
        public String toString() {
            return String.format("GitHubRelease{tagName='%s', name='%s', prerelease=%s}", 
                    tagName, name, prerelease);
        }
    }
}