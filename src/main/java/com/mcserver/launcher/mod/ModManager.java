package com.mcserver.launcher.mod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcserver.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 模組管理器
 * 提供模組插口功能，支持動態添加、移除和替換模組
 */
public class ModManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModManager.class);
    
    private final LauncherConfig config;
    private final Path modsDirectory;
    private final Path modSlotsConfig;
    private final ObjectMapper objectMapper;
    
    // 模組插口配置
    private Map<String, ModSlot> modSlots;
    
    public ModManager(LauncherConfig config) {
        this.config = config;
        this.modsDirectory = Paths.get(config.getMinecraftDirectory(), "mods");
        this.modSlotsConfig = Paths.get(config.getLauncherDirectory(), "mod-slots.json");
        this.objectMapper = new ObjectMapper();
        this.modSlots = new HashMap<>();
        
        initializeModSlots();
    }
    
    /**
     * 初始化模組插口系統
     */
    private void initializeModSlots() {
        try {
            // 創建 mods 目錄
            Files.createDirectories(modsDirectory);
            
            // 載入模組插口配置
            loadModSlots();
            
            // 掃描現有模組
            scanExistingMods();
            
            logger.info("模組管理器初始化完成，共 {} 個插口", modSlots.size());
            
        } catch (Exception e) {
            logger.error("模組管理器初始化失敗", e);
        }
    }
    
    /**
     * 載入模組插口配置
     */
    private void loadModSlots() throws IOException {
        if (Files.exists(modSlotsConfig)) {
            String json = Files.readString(modSlotsConfig);
            JsonNode root = objectMapper.readTree(json);
            
            JsonNode slotsNode = root.get("slots");
            if (slotsNode != null && slotsNode.isArray()) {
                for (JsonNode slotNode : slotsNode) {
                    ModSlot slot = parseModSlot(slotNode);
                    modSlots.put(slot.getId(), slot);
                }
            }
        } else {
            // 創建默認插口配置
            createDefaultModSlots();
            saveModSlots();
        }
    }
    
    /**
     * 創建默認模組插口
     */
    private void createDefaultModSlots() {
        // 核心模組插口
        modSlots.put("core", new ModSlot("core", "核心模組", "必需的核心功能模組", true, ModSlot.Category.CORE));
        
        // 優化模組插口
        modSlots.put("optimization", new ModSlot("optimization", "優化模組", "性能優化相關模組", false, ModSlot.Category.OPTIMIZATION));
        
        // 功能模組插口
        modSlots.put("utility", new ModSlot("utility", "實用工具", "實用功能模組", false, ModSlot.Category.UTILITY));
        
        // 裝飾模組插口
        modSlots.put("decoration", new ModSlot("decoration", "裝飾模組", "美化和裝飾相關模組", false, ModSlot.Category.DECORATION));
        
        // 技術模組插口
        modSlots.put("technology", new ModSlot("technology", "科技模組", "科技和機械相關模組", false, ModSlot.Category.TECHNOLOGY));
        
        // 魔法模組插口
        modSlots.put("magic", new ModSlot("magic", "魔法模組", "魔法和神秘相關模組", false, ModSlot.Category.MAGIC));
        
        // 冒險模組插口
        modSlots.put("adventure", new ModSlot("adventure", "冒險模組", "冒險和探索相關模組", false, ModSlot.Category.ADVENTURE));
        
        // 自定義插口 1-5
        for (int i = 1; i <= 5; i++) {
            String id = "custom" + i;
            modSlots.put(id, new ModSlot(id, "自定義插口 " + i, "可自由配置的模組插口", false, ModSlot.Category.CUSTOM));
        }
    }
    
    private ModSlot parseModSlot(JsonNode node) {
        String id = node.get("id").asText();
        String name = node.get("name").asText();
        String description = node.get("description").asText();
        boolean required = node.get("required").asBoolean();
        ModSlot.Category category = ModSlot.Category.valueOf(node.get("category").asText());
        
        ModSlot slot = new ModSlot(id, name, description, required, category);
        
        // 載入已安裝的模組
        JsonNode modsNode = node.get("mods");
        if (modsNode != null && modsNode.isArray()) {
            for (JsonNode modNode : modsNode) {
                ModInfo mod = parseModInfo(modNode);
                slot.addMod(mod);
            }
        }
        
        return slot;
    }
    
    private ModInfo parseModInfo(JsonNode node) {
        String fileName = node.get("fileName").asText();
        String modId = node.get("modId").asText();
        String name = node.get("name").asText();
        String version = node.get("version").asText();
        String description = node.has("description") ? node.get("description").asText() : "";
        boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : true;
        
        return new ModInfo(fileName, modId, name, version, description, enabled);
    }
    
    /**
     * 掃描現有模組文件
     */
    private void scanExistingMods() throws IOException {
        if (!Files.exists(modsDirectory)) {
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDirectory, "*.jar")) {
            for (Path modFile : stream) {
                try {
                    ModInfo modInfo = analyzeModFile(modFile);
                    if (modInfo != null) {
                        // 嘗試將模組分配到合適的插口
                        assignModToSlot(modInfo);
                    }
                } catch (Exception e) {
                    logger.warn("無法分析模組文件: {}", modFile.getFileName(), e);
                }
            }
        }
    }
    
    /**
     * 分析模組文件獲取信息
     */
    private ModInfo analyzeModFile(Path modFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(modFile.toFile())) {
            // 查找 mcmod.info 或 mods.toml
            ZipEntry mcmodInfo = zipFile.getEntry("mcmod.info");
            ZipEntry modsToml = zipFile.getEntry("META-INF/mods.toml");
            
            if (mcmodInfo != null) {
                return parseOldModInfo(zipFile, mcmodInfo, modFile.getFileName().toString());
            } else if (modsToml != null) {
                return parseNewModInfo(zipFile, modsToml, modFile.getFileName().toString());
            } else {
                // 無法識別的模組，創建基本信息
                String fileName = modFile.getFileName().toString();
                return new ModInfo(fileName, fileName.replace(".jar", ""), fileName, "unknown", "", true);
            }
        }
    }
    
    private ModInfo parseOldModInfo(ZipFile zipFile, ZipEntry entry, String fileName) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry)) {
            String content = new String(is.readAllBytes());
            JsonNode json = objectMapper.readTree(content);
            
            if (json.isArray() && json.size() > 0) {
                JsonNode modNode = json.get(0);
                String modId = modNode.get("modid").asText();
                String name = modNode.get("name").asText();
                String version = modNode.get("version").asText();
                String description = modNode.has("description") ? modNode.get("description").asText() : "";
                
                return new ModInfo(fileName, modId, name, version, description, true);
            }
        } catch (Exception e) {
            logger.warn("解析舊版模組信息失敗: {}", fileName, e);
        }
        
        return null;
    }
    
    private ModInfo parseNewModInfo(ZipFile zipFile, ZipEntry entry, String fileName) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry)) {
            String content = new String(is.readAllBytes());
            
            // 簡單的 TOML 解析 (這裡可以使用專門的 TOML 庫)
            String modId = extractTomlValue(content, "modId");
            String name = extractTomlValue(content, "displayName");
            String version = extractTomlValue(content, "version");
            String description = extractTomlValue(content, "description");
            
            if (modId != null && name != null) {
                return new ModInfo(fileName, modId, name, version != null ? version : "unknown", 
                        description != null ? description : "", true);
            }
        } catch (Exception e) {
            logger.warn("解析新版模組信息失敗: {}", fileName, e);
        }
        
        return null;
    }
    
    private String extractTomlValue(String content, String key) {
        String pattern = key + "\\s*=\\s*\"([^\"]*)\"|" + key + "\\s*=\\s*'([^']*)'";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(content);
        
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        
        return null;
    }
    
    /**
     * 將模組分配到合適的插口
     */
    private void assignModToSlot(ModInfo modInfo) {
        // 根據模組 ID 或名稱判斷類別
        String modId = modInfo.getModId().toLowerCase();
        String name = modInfo.getName().toLowerCase();
        
        ModSlot targetSlot = null;
        
        // 核心模組
        if (modId.contains("forge") || modId.contains("core") || modId.contains("api")) {
            targetSlot = modSlots.get("core");
        }
        // 優化模組
        else if (modId.contains("optifine") || modId.contains("performance") || 
                 name.contains("優化") || name.contains("fps")) {
            targetSlot = modSlots.get("optimization");
        }
        // 實用工具
        else if (modId.contains("jei") || modId.contains("waila") || modId.contains("utility") ||
                 name.contains("工具") || name.contains("實用")) {
            targetSlot = modSlots.get("utility");
        }
        // 科技模組
        else if (modId.contains("tech") || modId.contains("machine") || modId.contains("industrial") ||
                 name.contains("科技") || name.contains("機械")) {
            targetSlot = modSlots.get("technology");
        }
        // 魔法模組
        else if (modId.contains("magic") || modId.contains("thaumcraft") || modId.contains("botania") ||
                 name.contains("魔法") || name.contains("神秘")) {
            targetSlot = modSlots.get("magic");
        }
        // 冒險模組
        else if (modId.contains("adventure") || modId.contains("dungeon") || modId.contains("biome") ||
                 name.contains("冒險") || name.contains("探索")) {
            targetSlot = modSlots.get("adventure");
        }
        // 裝飾模組
        else if (modId.contains("decoration") || modId.contains("furniture") || modId.contains("chisel") ||
                 name.contains("裝飾") || name.contains("美化")) {
            targetSlot = modSlots.get("decoration");
        }
        
        // 如果沒有找到合適的插口，分配到第一個可用的自定義插口
        if (targetSlot == null) {
            for (int i = 1; i <= 5; i++) {
                ModSlot customSlot = modSlots.get("custom" + i);
                if (customSlot.getMods().isEmpty()) {
                    targetSlot = customSlot;
                    break;
                }
            }
        }
        
        if (targetSlot != null) {
            targetSlot.addMod(modInfo);
            logger.info("模組 {} 已分配到插口 {}", modInfo.getName(), targetSlot.getName());
        } else {
            logger.warn("無法為模組 {} 找到合適的插口", modInfo.getName());
        }
    }
    
    /**
     * 獲取所有模組插口
     */
    public Map<String, ModSlot> getModSlots() {
        return new HashMap<>(modSlots);
    }
    
    /**
     * 獲取指定插口
     */
    public ModSlot getModSlot(String slotId) {
        return modSlots.get(slotId);
    }
    
    /**
     * 添加模組到指定插口
     */
    public CompletableFuture<Boolean> addModToSlot(String slotId, Path modFile, Consumer<Integer> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ModSlot slot = modSlots.get(slotId);
                if (slot == null) {
                    logger.error("插口不存在: {}", slotId);
                    return false;
                }
                
                // 分析模組文件
                ModInfo modInfo = analyzeModFile(modFile);
                if (modInfo == null) {
                    logger.error("無法分析模組文件: {}", modFile);
                    return false;
                }
                
                // 複製模組文件到 mods 目錄
                Path targetPath = modsDirectory.resolve(modFile.getFileName());
                Files.copy(modFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // 更新模組信息中的文件名
                modInfo = new ModInfo(targetPath.getFileName().toString(), modInfo.getModId(), 
                        modInfo.getName(), modInfo.getVersion(), modInfo.getDescription(), true);
                
                // 添加到插口
                slot.addMod(modInfo);
                
                // 保存配置
                saveModSlots();
                
                logger.info("模組 {} 已添加到插口 {}", modInfo.getName(), slot.getName());
                return true;
                
            } catch (Exception e) {
                logger.error("添加模組失敗", e);
                return false;
            }
        });
    }
    
    /**
     * 從插口移除模組
     */
    public boolean removeModFromSlot(String slotId, String modFileName) {
        try {
            ModSlot slot = modSlots.get(slotId);
            if (slot == null) {
                logger.error("插口不存在: {}", slotId);
                return false;
            }
            
            // 從插口移除
            boolean removed = slot.removeMod(modFileName);
            if (!removed) {
                logger.warn("插口 {} 中沒有找到模組: {}", slotId, modFileName);
                return false;
            }
            
            // 刪除模組文件
            Path modFile = modsDirectory.resolve(modFileName);
            if (Files.exists(modFile)) {
                Files.delete(modFile);
                logger.info("已刪除模組文件: {}", modFile);
            }
            
            // 保存配置
            saveModSlots();
            
            logger.info("模組 {} 已從插口 {} 移除", modFileName, slot.getName());
            return true;
            
        } catch (Exception e) {
            logger.error("移除模組失敗", e);
            return false;
        }
    }
    
    /**
     * 啟用/禁用模組
     */
    public boolean toggleMod(String slotId, String modFileName, boolean enabled) {
        try {
            ModSlot slot = modSlots.get(slotId);
            if (slot == null) {
                return false;
            }
            
            ModInfo mod = slot.getMod(modFileName);
            if (mod == null) {
                return false;
            }
            
            // 更新模組狀態
            mod.setEnabled(enabled);
            
            // 重命名文件 (禁用時添加 .disabled 後綴)
            Path currentPath = modsDirectory.resolve(modFileName);
            Path newPath;
            
            if (enabled) {
                // 啟用：移除 .disabled 後綴
                String newFileName = modFileName.replace(".disabled", "");
                newPath = modsDirectory.resolve(newFileName);
                mod.setFileName(newFileName);
            } else {
                // 禁用：添加 .disabled 後綴
                String newFileName = modFileName + ".disabled";
                newPath = modsDirectory.resolve(newFileName);
                mod.setFileName(newFileName);
            }
            
            if (Files.exists(currentPath)) {
                Files.move(currentPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 保存配置
            saveModSlots();
            
            logger.info("模組 {} 已{}", mod.getName(), enabled ? "啟用" : "禁用");
            return true;
            
        } catch (Exception e) {
            logger.error("切換模組狀態失敗", e);
            return false;
        }
    }
    
    /**
     * 保存模組插口配置
     */
    private void saveModSlots() throws IOException {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        for (ModSlot slot : modSlots.values()) {
            Map<String, Object> slotData = new HashMap<>();
            slotData.put("id", slot.getId());
            slotData.put("name", slot.getName());
            slotData.put("description", slot.getDescription());
            slotData.put("required", slot.isRequired());
            slotData.put("category", slot.getCategory().name());
            
            List<Map<String, Object>> mods = new ArrayList<>();
            for (ModInfo mod : slot.getMods()) {
                Map<String, Object> modData = new HashMap<>();
                modData.put("fileName", mod.getFileName());
                modData.put("modId", mod.getModId());
                modData.put("name", mod.getName());
                modData.put("version", mod.getVersion());
                modData.put("description", mod.getDescription());
                modData.put("enabled", mod.isEnabled());
                mods.add(modData);
            }
            slotData.put("mods", mods);
            
            slots.add(slotData);
        }
        
        config.put("slots", slots);
        
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        Files.writeString(modSlotsConfig, json);
    }
    
    /**
     * 獲取所有已安裝的模組
     */
    public List<ModInfo> getAllMods() {
        List<ModInfo> allMods = new ArrayList<>();
        for (ModSlot slot : modSlots.values()) {
            allMods.addAll(slot.getMods());
        }
        return allMods;
    }
    
    /**
     * 獲取啟用的模組數量
     */
    public int getEnabledModCount() {
        return (int) getAllMods().stream().filter(ModInfo::isEnabled).count();
    }
    
    /**
     * 獲取模組總數
     */
    public int getTotalModCount() {
        return getAllMods().size();
    }
}