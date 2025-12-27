package com.mcserver.launcher.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcserver.launcher.config.LauncherConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 模組同步管理器
 * 負責與 GitHub 倉庫進行增量更新 (Mods & ResourcePacks)
 */
public class ModSyncManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModSyncManager.class);
    private static final String GITHUB_API_BASE = "https://api.github.com/repos";
    
    private final LauncherConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    public ModSyncManager(LauncherConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * 執行完整同步 (Mods 和 ResourcePacks)
     * @param progressCallback 進度回調 (message, progress 0-100)
     */
    public CompletableFuture<Void> syncAll(Consumer<String> statusCallback, Consumer<Double> progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("開始同步模組與資源包...");
                
                // 同步 Mods
                syncDirectory("mods", config.getModsDirectory(), statusCallback, progressCallback, 0, 50);
                
                // 同步 ResourcePacks
                syncDirectory("resourcepacks", config.getResourcePacksDirectory(), statusCallback, progressCallback, 50, 100);
                
                statusCallback.accept("同步完成！");
                progressCallback.accept(100.0);
                
            } catch (Exception e) {
                logger.error("同步失敗", e);
                throw new RuntimeException("同步失敗: " + e.getMessage());
            }
        });
    }
    
    /**
     * 同步單個目錄
     */
    private void syncDirectory(String remoteDirName, Path localDir, 
                               Consumer<String> statusCallback, Consumer<Double> progressCallback,
                               double startProgress, double endProgress) throws Exception {
        
        statusCallback.accept("正在檢查 " + remoteDirName + "...");
        Files.createDirectories(localDir);
        
        // 1. 獲取遠端文件列表
        List<GitHubFile> remoteFiles = fetchRemoteFiles(remoteDirName);
        Map<String, GitHubFile> remoteFileMap = remoteFiles.stream()
                .collect(Collectors.toMap(f -> f.name, f -> f));
        
        // 2. 獲取本地文件列表
        Set<String> localFilenames;
        try (Stream<Path> stream = Files.list(localDir)) {
            localFilenames = stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());
        }
        
        // 3. 計算差異
        List<GitHubFile> toDownload = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();
        
        // 檢查需要下載/更新的文件
        for (GitHubFile remoteFile : remoteFiles) {
            Path localPath = localDir.resolve(remoteFile.name);
            if (!Files.exists(localPath) || Files.size(localPath) != remoteFile.size) {
                toDownload.add(remoteFile);
            }
        }
        
        // 檢查需要刪除的文件
        for (String filename : localFilenames) {
            if (!remoteFileMap.containsKey(filename)) {
                toDelete.add(filename);
            }
        }
        
        logger.info("[{}] 需下載: {}, 需刪除: {}", remoteDirName, toDownload.size(), toDelete.size());
        
        // 4. 執行刪除
        for (String filename : toDelete) {
            statusCallback.accept("刪除舊文件: " + filename);
            Files.deleteIfExists(localDir.resolve(filename));
        }
        
        // 5. 執行下載
        int totalOps = toDownload.size();
        for (int i = 0; i < totalOps; i++) {
            GitHubFile file = toDownload.get(i);
            statusCallback.accept("下載: " + file.name);
            
            // 下載文件
            downloadFile(file.downloadUrl, localDir.resolve(file.name));
            
            // 更新進度
            double currentProgress = startProgress + ((double)(i + 1) / totalOps) * (endProgress - startProgress);
            progressCallback.accept(currentProgress);
        }
    }
    
    private List<GitHubFile> fetchRemoteFiles(String path) throws Exception {
        String repo = config.getGithubRepo();
        String url = String.format("%s/%s/contents/%s", GITHUB_API_BASE, repo, path);
        
        HttpGet get = new HttpGet(url);
        if (config.getGithubToken() != null && !config.getGithubToken().isEmpty()) {
            get.setHeader("Authorization", "token " + config.getGithubToken());
        }
        
        try (var response = httpClient.execute(get)) {
            if (response.getCode() == 404) {
                logger.warn("遠端目錄不存在: {}", path);
                return Collections.emptyList();
            }
            
            if (response.getCode() != 200) {
                throw new Exception("GitHub API 錯誤: " + response.getCode());
            }
            
            String json = new String(response.getEntity().getContent().readAllBytes());
            JsonNode root = objectMapper.readTree(json);
            
            List<GitHubFile> files = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if ("file".equals(node.get("type").asText())) {
                        files.add(new GitHubFile(
                            node.get("name").asText(),
                            node.get("size").asLong(),
                            node.get("download_url").asText(),
                            node.get("sha").asText()
                        ));
                    }
                }
            }
            return files;
        }
    }
    
    private void downloadFile(String url, Path destination) throws Exception {
        URL downloadUrl = new URL(url);
        try (ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
             FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    private static class GitHubFile {
        String name;
        long size;
        String downloadUrl;
        String sha;
        
        GitHubFile(String name, long size, String downloadUrl, String sha) {
            this.name = name;
            this.size = size;
            this.downloadUrl = downloadUrl;
            this.sha = sha;
        }
    }
}
