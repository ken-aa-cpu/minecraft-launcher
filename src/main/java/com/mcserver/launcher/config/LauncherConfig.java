package com.mcserver.launcher.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 啟動器配置管理類
 * 負責載入、保存和管理啟動器的各種配置
 */
public class LauncherConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LauncherConfig.class);
    
    // 配置文件路径
    private final Path configFile;
    private final ObjectMapper objectMapper;
    
    // 配置項
    private String launcherDirectory;
    private String minecraftDirectory;
    private String javaPath;
    private String javaArgs;
    private int memoryMin;
    private int memoryMax;
    private String gameResolution;
    private boolean fullscreen;
    private boolean autoLogin;
    private boolean checkUpdates;
    private boolean updatePending;
    private String lastUsername;
    private String serverAddress;
    private int serverPort;
    private String theme;
    private String language;
    
    // GitHub 配置
    private String githubRepo;
    private String githubToken;
    private boolean autoUpdate;
    private String updateChannel; // stable, beta, dev
    
    // 模組配置
    private boolean autoLoadMods;
    private boolean enableModUpdates;
    private String modUpdateSource;
    
    // Session 配置
    private String sessionUsername;
    private String sessionUuid;
    private String sessionAccessToken;
    private String sessionRefreshToken;
    
    public LauncherConfig() {
        this.objectMapper = new ObjectMapper();
        this.configFile = Paths.get(System.getProperty("user.home"), ".minecraft-launcher", "config.json");
        
        // 設置默認值
        setDefaults();
        
        // 載入配置
        load();
    }
    
    /**
     * 設置默認配置值
     */
    private void setDefaults() {
        this.launcherDirectory = Paths.get(System.getProperty("user.home"), ".minecraft-launcher").toString();
        
        // 檢查當前目錄下是否有 game 或 data 目錄 (便攜模式)
        Path portableGameDir = Paths.get("game");
        if (Files.exists(portableGameDir)) {
            this.minecraftDirectory = portableGameDir.toAbsolutePath().toString();
            logger.info("檢測到便攜模式，使用遊戲目錄: {}", this.minecraftDirectory);
        } else {
            this.minecraftDirectory = Paths.get(System.getProperty("user.home"), ".minecraft").toString();
        }
        
        this.javaPath = System.getProperty("java.home") + "/bin/java";
        this.javaArgs = "-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC";
        this.memoryMin = 2048; // 2GB
        this.memoryMax = 4096; // 4GB
        this.gameResolution = "1920x1080";
        this.fullscreen = false;
        this.autoLogin = true;
        this.checkUpdates = true;
        this.updatePending = false;
        this.lastUsername = "";
        this.serverAddress = "";
        this.serverPort = 25565;
        this.theme = "dark";
        this.language = "zh_TW";
        
        // GitHub 默認配置
        this.githubRepo = "ken-aa-cpu/minecraft-launcher"; // Ken 的倉庫
        this.githubToken = ""; // 可選，用於私有倉庫
        this.autoUpdate = true;
        this.updateChannel = "stable";
        
        // 模組默認配置
        this.autoLoadMods = true;
        this.enableModUpdates = true;
        this.modUpdateSource = "github";
    }
    
    /**
     * 載入配置文件
     */
    public void load() {
        try {
            if (!Files.exists(configFile)) {
                // 創建配置目錄
                Files.createDirectories(configFile.getParent());
                // 保存默認配置
                save();
                logger.info("已創建默認配置文件: {}", configFile);
                return;
            }
            
            String json = Files.readString(configFile);
            JsonNode root = objectMapper.readTree(json);
            
            // 載入基本配置
            if (root.has("launcherDirectory")) {
                this.launcherDirectory = root.get("launcherDirectory").asText();
            }
            if (root.has("minecraftDirectory")) {
                this.minecraftDirectory = root.get("minecraftDirectory").asText();
            }
            if (root.has("javaPath")) {
                this.javaPath = root.get("javaPath").asText();
            }
            if (root.has("javaArgs")) {
                this.javaArgs = root.get("javaArgs").asText();
            }
            if (root.has("memoryMin")) {
                this.memoryMin = root.get("memoryMin").asInt();
            }
            if (root.has("memoryMax")) {
                this.memoryMax = root.get("memoryMax").asInt();
            }
            if (root.has("gameResolution")) {
                this.gameResolution = root.get("gameResolution").asText();
            }
            if (root.has("fullscreen")) {
                this.fullscreen = root.get("fullscreen").asBoolean();
            }
            if (root.has("autoLogin")) {
                this.autoLogin = root.get("autoLogin").asBoolean();
            }
            if (root.has("checkUpdates")) {
                this.checkUpdates = root.get("checkUpdates").asBoolean();
            }
            if (root.has("updatePending")) {
                this.updatePending = root.get("updatePending").asBoolean();
            }
            if (root.has("lastUsername")) {
                this.lastUsername = root.get("lastUsername").asText();
            }
            if (root.has("serverAddress")) {
                this.serverAddress = root.get("serverAddress").asText();
            }
            if (root.has("serverPort")) {
                this.serverPort = root.get("serverPort").asInt();
            }
            if (root.has("theme")) {
                this.theme = root.get("theme").asText();
            }
            if (root.has("language")) {
                this.language = root.get("language").asText();
            }
            
            // 載入 GitHub 配置
            if (root.has("github")) {
                JsonNode github = root.get("github");
                if (github.has("repo")) {
                    this.githubRepo = github.get("repo").asText();
                }
                if (github.has("token")) {
                    this.githubToken = github.get("token").asText();
                }
                if (github.has("autoUpdate")) {
                    this.autoUpdate = github.get("autoUpdate").asBoolean();
                }
                if (github.has("updateChannel")) {
                    this.updateChannel = github.get("updateChannel").asText();
                }
            }
            
            // 載入模組配置
            if (root.has("mods")) {
                JsonNode mods = root.get("mods");
                if (mods.has("autoLoad")) {
                    this.autoLoadMods = mods.get("autoLoad").asBoolean();
                }
                if (mods.has("enableUpdates")) {
                    this.enableModUpdates = mods.get("enableUpdates").asBoolean();
                }
                if (mods.has("updateSource")) {
                    this.modUpdateSource = mods.get("updateSource").asText();
                }
            }
            
            // 載入 Session 配置
            if (root.has("session")) {
                JsonNode session = root.get("session");
                if (session.has("username")) this.sessionUsername = session.get("username").asText();
                if (session.has("uuid")) this.sessionUuid = session.get("uuid").asText();
                if (session.has("accessToken")) this.sessionAccessToken = session.get("accessToken").asText();
                if (session.has("refreshToken")) this.sessionRefreshToken = session.get("refreshToken").asText();
            }
            
            logger.info("配置文件載入成功: {}", configFile);
            
        } catch (Exception e) {
            logger.error("載入配置文件失敗，使用默認配置", e);
            setDefaults();
        }
    }
    
    /**
     * 保存配置文件
     */
    public void save() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 基本配置
            config.put("launcherDirectory", launcherDirectory);
            config.put("minecraftDirectory", minecraftDirectory);
            config.put("javaPath", javaPath);
            config.put("javaArgs", javaArgs);
            config.put("memoryMin", memoryMin);
            config.put("memoryMax", memoryMax);
            config.put("gameResolution", gameResolution);
            config.put("fullscreen", fullscreen);
            config.put("autoLogin", autoLogin);
            config.put("checkUpdates", checkUpdates);
            config.put("updatePending", updatePending);
            config.put("lastUsername", lastUsername);
            config.put("serverAddress", serverAddress);
            config.put("serverPort", serverPort);
            config.put("theme", theme);
            config.put("language", language);
            
            // GitHub 配置
            Map<String, Object> github = new HashMap<>();
            github.put("repo", githubRepo);
            github.put("token", githubToken);
            github.put("autoUpdate", autoUpdate);
            github.put("updateChannel", updateChannel);
            config.put("github", github);
            
            // 模組配置
            Map<String, Object> mods = new HashMap<>();
            mods.put("autoLoad", autoLoadMods);
            mods.put("enableUpdates", enableModUpdates);
            mods.put("updateSource", modUpdateSource);
            config.put("mods", mods);
            
            // 確保目錄存在
            Files.createDirectories(configFile.getParent());
            
            // 寫入文件
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            Files.writeString(configFile, json);
            
            logger.debug("配置文件保存成功: {}", configFile);
            
        } catch (IOException e) {
            logger.error("保存配置文件失敗", e);
        }
    }
    
    /**
     * 重置為默認配置
     */
    public void reset() {
        setDefaults();
        save();
        logger.info("配置已重置為默認值");
    }
    
    /**
     * 驗證配置的有效性
     */
    public boolean validate() {
        boolean valid = true;
        
        // 檢查必需的目錄
        if (launcherDirectory == null || launcherDirectory.isEmpty()) {
            logger.error("啟動器目錄未設置");
            valid = false;
        }
        
        if (minecraftDirectory == null || minecraftDirectory.isEmpty()) {
            logger.error("Minecraft 目錄未設置");
            valid = false;
        }
        
        // 檢查 Java 路徑
        if (javaPath == null || javaPath.isEmpty()) {
            logger.error("Java 路徑未設置");
            valid = false;
        } else if (!Files.exists(Paths.get(javaPath))) {
            logger.warn("Java 路徑不存在: {}", javaPath);
        }
        
        // 檢查內存設置
        if (memoryMin <= 0 || memoryMax <= 0 || memoryMin > memoryMax) {
            logger.error("內存設置無效: min={}, max={}", memoryMin, memoryMax);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * 獲取 Minecraft 版本目錄
     */
    public Path getVersionDirectory(String version) {
        return Paths.get(minecraftDirectory, "versions", version);
    }
    
    /**
     * 獲取模組目錄
     */
    public Path getModsDirectory() {
        return Paths.get(minecraftDirectory, "mods");
    }
    
    /**
     * 獲取資源包目錄
     */
    public Path getResourcePacksDirectory() {
        return Paths.get(minecraftDirectory, "resourcepacks");
    }
    
    /**
     * 獲取存檔目錄
     */
    public Path getSavesDirectory() {
        return Paths.get(minecraftDirectory, "saves");
    }
    
    // Getters and Setters
    public String getLauncherDirectory() {
        return launcherDirectory;
    }
    
    public void setLauncherDirectory(String launcherDirectory) {
        this.launcherDirectory = launcherDirectory;
    }
    
    public String getMinecraftDirectory() {
        return minecraftDirectory;
    }
    
    public void setMinecraftDirectory(String minecraftDirectory) {
        this.minecraftDirectory = minecraftDirectory;
    }
    
    public String getJavaPath() {
        return javaPath;
    }
    
    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }
    
    public String getJavaArgs() {
        return javaArgs;
    }
    
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }
    
    public int getMemoryMin() {
        return memoryMin;
    }
    
    public void setMemoryMin(int memoryMin) {
        this.memoryMin = memoryMin;
    }
    
    public int getMemoryMax() {
        return memoryMax;
    }
    
    public void setMemoryMax(int memoryMax) {
        this.memoryMax = memoryMax;
    }
    
    public String getGameResolution() {
        return gameResolution;
    }
    
    public void setGameResolution(String gameResolution) {
        this.gameResolution = gameResolution;
    }
    
    public boolean isFullscreen() {
        return fullscreen;
    }
    
    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }
    
    public boolean isAutoLogin() {
        return autoLogin;
    }
    
    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }
    
    public boolean isCheckUpdates() {
        return checkUpdates;
    }
    
    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
    }
    
    public boolean isUpdatePending() {
        return updatePending;
    }
    
    public void setUpdatePending(boolean updatePending) {
        this.updatePending = updatePending;
    }
    
    public String getLastUsername() {
        return lastUsername;
    }
    
    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }
    
    public String getServerAddress() {
        return serverAddress;
    }
    
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getGithubRepo() {
        return githubRepo;
    }
    
    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }
    
    public String getGithubToken() {
        return githubToken;
    }
    
    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
    
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }
    
    public String getUpdateChannel() {
        return updateChannel;
    }
    
    public void setUpdateChannel(String updateChannel) {
        this.updateChannel = updateChannel;
    }
    
    public boolean isAutoLoadMods() {
        return autoLoadMods;
    }
    
    public void setAutoLoadMods(boolean autoLoadMods) {
        this.autoLoadMods = autoLoadMods;
    }
    
    public boolean isEnableModUpdates() {
        return enableModUpdates;
    }
    
    public void setEnableModUpdates(boolean enableModUpdates) {
        this.enableModUpdates = enableModUpdates;
    }
    
    public String getModUpdateSource() {
        return modUpdateSource;
    }
    
    public void setModUpdateSource(String modUpdateSource) {
        this.modUpdateSource = modUpdateSource;
    }
    
    /**
     * 保存用戶會話信息
     */
    public void saveSession(Object sessionObj) {
        try {
            // 使用反射获取字段，或者强制转换，这里假设我们知道它的结构
            // 为了解耦，最好让 MicrosoftAuthenticator 传递明确的参数，或者使用接口
            // 但这里为了快速修复，我们通过反射或者假设它是 MinecraftSession
            
            // 注意：由于 MinecraftSession 是内部类，且为了避免循环依赖问题
            // 我们最好通过 getter 方法获取数据，但这里参数是 Object
            
            // 更好的方式是传入具体参数，但为了不破坏现有签名（如果有），
            // 我们修改方法签名，接受具体参数，或者让 MicrosoftAuthenticator 直接设置 Config 字段
            
            // 让我们使用反射来获取数据，这样不需要引入 MicrosoftAuthenticator 类
            java.lang.reflect.Method getUsername = sessionObj.getClass().getMethod("getUsername");
            java.lang.reflect.Method getUuid = sessionObj.getClass().getMethod("getUuid");
            java.lang.reflect.Method getAccessToken = sessionObj.getClass().getMethod("getAccessToken");
            java.lang.reflect.Method getRefreshToken = sessionObj.getClass().getMethod("getRefreshToken");
            
            this.sessionUsername = (String) getUsername.invoke(sessionObj);
            this.sessionUuid = (String) getUuid.invoke(sessionObj);
            this.sessionAccessToken = (String) getAccessToken.invoke(sessionObj);
            this.sessionRefreshToken = (String) getRefreshToken.invoke(sessionObj);
            
            save();
            logger.info("Session saved for user: {}", sessionUsername);
        } catch (Exception e) {
            logger.error("Failed to save session", e);
        }
    }
    
    /**
     * 清除用戶會話信息
     */
    public void clearSession() {
        this.sessionUsername = "";
        this.sessionUuid = "";
        this.sessionAccessToken = "";
        this.sessionRefreshToken = "";
        save();
        logger.info("Session cleared");
    }
    
    /**
     * 檢查是否有有效會話
     */
    public boolean hasValidSession() {
        return sessionRefreshToken != null && !sessionRefreshToken.isEmpty();
    }
    
    public String getSessionUsername() { return sessionUsername; }
    public String getSessionUuid() { return sessionUuid; }
    public String getSessionAccessToken() { return sessionAccessToken; }
    public String getSessionRefreshToken() { return sessionRefreshToken; }
    
    /**
     * 獲取最後登入的用戶
     */
    public String getLastUser() {
        return lastUsername;
    }
}