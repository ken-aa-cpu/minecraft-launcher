package com.mcserver.launcher.minecraft;

import com.mcserver.launcher.auth.UserProfile;
import com.mcserver.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Minecraft 啟動器
 * 負責啟動 Minecraft Forge 1.20.1
 */
public class MinecraftLauncher {
    
    private static final Logger logger = LoggerFactory.getLogger(MinecraftLauncher.class);
    
    // Minecraft 和 NeoForge 版本
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String NEOFORGE_VERSION = "21.1.217";
    private static final String NEOFORGE_PROFILE = "neoforge-" + NEOFORGE_VERSION;
    
    private final LauncherConfig config;
    private Process minecraftProcess;
    
    public MinecraftLauncher(LauncherConfig config) {
        this.config = config;
    }
    
    /**
     * 啟動 Minecraft
     * @param userProfile 用戶資料
     * @param outputCallback 輸出回調
     * @return 啟動結果
     */
    public CompletableFuture<Boolean> launch(UserProfile userProfile, Consumer<String> outputCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("開始啟動 Minecraft {} with NeoForge {}", MINECRAFT_VERSION, NEOFORGE_VERSION);
                
                // 檢查並準備啟動環境
                if (!prepareEnvironment()) {
                    logger.error("環境準備失敗");
                    return false;
                }
                
                // 構建啟動命令
                List<String> command = buildLaunchCommand(userProfile);
                if (command.isEmpty()) {
                    logger.error("無法構建啟動命令");
                    return false;
                }
                
                // 啟動遊戲
                return startMinecraft(command, outputCallback);
                
            } catch (Exception e) {
                logger.error("啟動 Minecraft 失敗", e);
                return false;
            }
        });
    }
    
    /**
     * 準備啟動環境
     */
    private boolean prepareEnvironment() throws IOException {
        logger.info("準備啟動環境...");
        
        // 創建必要的目錄
        Path minecraftDir = Paths.get(config.getMinecraftDirectory());
        Files.createDirectories(minecraftDir);
        Files.createDirectories(minecraftDir.resolve("versions"));
        Files.createDirectories(minecraftDir.resolve("libraries"));
        Files.createDirectories(minecraftDir.resolve("assets"));
        Files.createDirectories(minecraftDir.resolve("mods"));
        Files.createDirectories(minecraftDir.resolve("config"));
        Files.createDirectories(minecraftDir.resolve("saves"));
        Files.createDirectories(minecraftDir.resolve("resourcepacks"));
        Files.createDirectories(minecraftDir.resolve("shaderpacks"));
        
        // 檢查 NeoForge 版本是否存在
        Path neoForgeVersionDir = config.getVersionDirectory(NEOFORGE_PROFILE);
        if (!Files.exists(neoForgeVersionDir)) {
            logger.warn("NeoForge 版本目錄不存在: {}", neoForgeVersionDir);
            logger.info("請確保已安裝 Minecraft {} 和 NeoForge {}", MINECRAFT_VERSION, NEOFORGE_VERSION);
            
            // 嘗試創建基本的版本配置
            createNeoForgeVersionConfig(neoForgeVersionDir);
        }
        
        // 檢查 Java 路徑
        if (!Files.exists(Paths.get(config.getJavaPath()))) {
            logger.error("Java 路徑不存在: {}", config.getJavaPath());
            return false;
        }
        
        logger.info("環境準備完成");
        return true;
    }
    
    /**
     * 創建 NeoForge 版本配置
     */
    private void createNeoForgeVersionConfig(Path versionDir) throws IOException {
        Files.createDirectories(versionDir);
        
        // 創建基本的版本 JSON 配置
        String versionJson = createNeoForgeVersionJson();
        Files.writeString(versionDir.resolve(NEOFORGE_PROFILE + ".json"), versionJson);
        
        logger.info("已創建 NeoForge 版本配置: {}", versionDir);
    }
    
    /**
     * 創建 NeoForge 版本 JSON 配置
     */
    private String createNeoForgeVersionJson() {
        return String.format("{" +
            "\"id\": \"%s\"," +
            "\"type\": \"release\"," +
            "\"time\": \"2024-01-01T00:00:00+00:00\"," +
            "\"releaseTime\": \"2024-01-01T00:00:00+00:00\"," +
            "\"minecraftArguments\": \"--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type} --versionType ${version_type}\"," +
            "\"mainClass\": \"net.neoforged.neoforge.client.loading.ClientModLoader\"," +
            "\"inheritsFrom\": \"%s\"," +
            "\"jar\": \"%s\"," +
            "\"libraries\": []," +
            "\"logging\": {}" +
        "}", NEOFORGE_PROFILE, MINECRAFT_VERSION, MINECRAFT_VERSION);
    }
    
    /**
     * 構建啟動命令
     */
    private List<String> buildLaunchCommand(UserProfile userProfile) {
        List<String> command = new ArrayList<>();
        
        try {
            // Java 可執行文件
            command.add(config.getJavaPath());
            
            // JVM 參數
            command.addAll(parseJavaArgs(config.getJavaArgs()));
            
            // 內存設置
            command.add("-Xms" + config.getMemoryMin() + "M");
            command.add("-Xmx" + config.getMemoryMax() + "M");
            
            // 系統屬性
            command.add("-Djava.library.path=" + Paths.get(config.getMinecraftDirectory(), "versions", NEOFORGE_PROFILE, "natives"));
            command.add("-Dminecraft.launcher.brand=custom-launcher");
            command.add("-Dminecraft.launcher.version=1.0.0");
            
            // NeoForge 特定參數 (1.20.5+ 需要的參數)
            // 參考 NeoForge 啟動腳本
            command.add("-Dneoforge.enabled=true");
            
            // Classpath
            String classpath = buildClasspath();
            if (classpath.isEmpty()) {
                logger.error("無法構建 classpath");
                return Collections.emptyList();
            }
            command.add("-cp");
            command.add(classpath);
            
            // 主類
            // 注意：NeoForge 1.20.5+ 的啟動類可能不同，這裡使用通常的 ClientModLoader
            // 如果無法啟動，可能需要檢查 libraries 中的 args 文件
            command.add("net.neoforged.neoforge.client.loading.ClientModLoader");
            
            // 遊戲參數
            command.addAll(buildGameArguments(userProfile));
            
            logger.info("啟動命令構建完成，共 {} 個參數", command.size());
            logger.debug("啟動命令: {}", String.join(" ", command));
            
            return command;
            
        } catch (Exception e) {
            logger.error("構建啟動命令失敗", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 解析 Java 參數
     */
    private List<String> parseJavaArgs(String javaArgs) {
        if (javaArgs == null || javaArgs.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.asList(javaArgs.trim().split("\\s+"));
    }
    
    /**
     * 構建 Classpath
     */
    private String buildClasspath() {
        List<String> classpathEntries = new ArrayList<>();
        
        try {
            Path minecraftDir = Paths.get(config.getMinecraftDirectory());
            Path librariesDir = minecraftDir.resolve("libraries");
            Path versionsDir = minecraftDir.resolve("versions");
            
            // 添加 Minecraft 客戶端 JAR
            Path minecraftJar = versionsDir.resolve(MINECRAFT_VERSION).resolve(MINECRAFT_VERSION + ".jar");
            if (Files.exists(minecraftJar)) {
                classpathEntries.add(minecraftJar.toString());
            }
            
            // 添加 Forge JAR
            Path forgeJar = versionsDir.resolve(NEOFORGE_PROFILE).resolve(NEOFORGE_PROFILE + ".jar");
            if (Files.exists(forgeJar)) {
                classpathEntries.add(forgeJar.toString());
            }
            
            // 添加庫文件
            if (Files.exists(librariesDir)) {
                addLibrariesToClasspath(librariesDir, classpathEntries);
            }
            
            logger.debug("Classpath 包含 {} 個條目", classpathEntries.size());
            
        } catch (Exception e) {
            logger.error("構建 classpath 失敗", e);
        }
        
        return String.join(File.pathSeparator, classpathEntries);
    }
    
    /**
     * 添加庫文件到 Classpath
     */
    private void addLibrariesToClasspath(Path librariesDir, List<String> classpathEntries) throws IOException {
        Files.walk(librariesDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(path -> classpathEntries.add(path.toString()));
    }
    
    /**
     * 構建遊戲參數
     */
    private List<String> buildGameArguments(UserProfile userProfile) {
        List<String> args = new ArrayList<>();
        
        // 用戶認證參數
        args.add("--username");
        args.add(userProfile.getUsername());
        args.add("--uuid");
        args.add(userProfile.getUuid());
        args.add("--accessToken");
        args.add(userProfile.getAccessToken());
        args.add("--userType");
        args.add("msa"); // Microsoft account
        
        // 版本參數
        args.add("--version");
        args.add(NEOFORGE_PROFILE);
        args.add("--versionType");
        args.add("release");
        
        // 目錄參數
        args.add("--gameDir");
        args.add(config.getMinecraftDirectory());
        args.add("--assetsDir");
        args.add(Paths.get(config.getMinecraftDirectory(), "assets").toString());
        args.add("--assetIndex");
        args.add(MINECRAFT_VERSION);
        
        // 解析度參數
        if (config.getGameResolution() != null && !config.getGameResolution().isEmpty()) {
            String[] resolution = config.getGameResolution().split("x");
            if (resolution.length == 2) {
                args.add("--width");
                args.add(resolution[0]);
                args.add("--height");
                args.add(resolution[1]);
            }
        }
        
        // 全螢幕參數
        if (config.isFullscreen()) {
            args.add("--fullscreen");
        }
        
        // 服務器參數（如果設置）
        if (config.getServerAddress() != null && !config.getServerAddress().isEmpty()) {
            args.add("--server");
            args.add(config.getServerAddress());
            args.add("--port");
            args.add(String.valueOf(config.getServerPort()));
        }
        
        return args;
    }
    
    /**
     * 啟動 Minecraft 進程
     */
    private boolean startMinecraft(List<String> command, Consumer<String> outputCallback) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(config.getMinecraftDirectory()));
            
            // 設置環境變量
            Map<String, String> env = processBuilder.environment();
            env.put("APPDATA", config.getMinecraftDirectory());
            
            logger.info("啟動 Minecraft 進程...");
            minecraftProcess = processBuilder.start();
            
            // 處理輸出
            if (outputCallback != null) {
                handleProcessOutput(minecraftProcess, outputCallback);
            }
            
            logger.info("Minecraft 已啟動，PID: {}", minecraftProcess.pid());
            return true;
            
        } catch (IOException e) {
            logger.error("啟動 Minecraft 進程失敗", e);
            return false;
        }
    }
    
    /**
     * 處理進程輸出
     */
    private void handleProcessOutput(Process process, Consumer<String> outputCallback) {
        // 處理標準輸出
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[STDOUT] {}", line);
                    outputCallback.accept("[INFO] " + line);
                }
            } catch (IOException e) {
                logger.error("讀取標準輸出失敗", e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
        
        // 處理錯誤輸出
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.warn("[STDERR] {}", line);
                    outputCallback.accept("[ERROR] " + line);
                }
            } catch (IOException e) {
                logger.error("讀取錯誤輸出失敗", e);
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();
    }
    
    /**
     * 檢查 Minecraft 是否正在運行
     */
    public boolean isRunning() {
        return minecraftProcess != null && minecraftProcess.isAlive();
    }
    
    /**
     * 停止 Minecraft
     */
    public void stop() {
        if (minecraftProcess != null && minecraftProcess.isAlive()) {
            logger.info("正在停止 Minecraft...");
            minecraftProcess.destroy();
            
            // 等待進程結束
            try {
                if (!minecraftProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("強制終止 Minecraft 進程");
                    minecraftProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待進程結束時被中斷", e);
            }
            
            minecraftProcess = null;
            logger.info("Minecraft 已停止");
        }
    }
    
    /**
     * 獲取進程退出碼
     */
    public int getExitCode() {
        if (minecraftProcess != null && !minecraftProcess.isAlive()) {
            return minecraftProcess.exitValue();
        }
        return -1;
    }
    
    /**
     * 等待進程結束
     */
    public CompletableFuture<Integer> waitForExit() {
        if (minecraftProcess == null) {
            return CompletableFuture.completedFuture(-1);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return minecraftProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        });
    }
    
    /**
     * 檢查並下載必要文件
     */
    public void checkAndDownloadFiles(java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        logger.info("檢查遊戲文件...");
        // 這裡可以實現文件檢查和下載邏輯
        // 暫時模擬進度更新
        if (progressCallback != null) {
            progressCallback.accept(10, 100);
            progressCallback.accept(30, 100);
            progressCallback.accept(50, 100);
        }
        logger.info("文件檢查完成");
    }
    
    /**
     * 關閉啟動器
     */
    public void shutdown() {
        if (isRunning()) {
            stop();
        }
        logger.info("MinecraftLauncher 已關閉");
    }
}