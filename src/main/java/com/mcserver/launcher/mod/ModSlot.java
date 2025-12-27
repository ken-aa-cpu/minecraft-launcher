package com.mcserver.launcher.mod;

import java.util.*;

/**
 * 模組插口類
 * 代表一個可以安裝模組的插口位置
 */
public class ModSlot {
    
    private final String id;
    private final String name;
    private final String description;
    private final boolean required;
    private final Category category;
    private final List<ModInfo> mods;
    
    /**
     * 模組類別枚舉
     */
    public enum Category {
        CORE("核心", "必需的核心功能模組"),
        OPTIMIZATION("優化", "性能優化相關模組"),
        UTILITY("實用工具", "實用功能模組"),
        DECORATION("裝飾", "美化和裝飾相關模組"),
        TECHNOLOGY("科技", "科技和機械相關模組"),
        MAGIC("魔法", "魔法和神秘相關模組"),
        ADVENTURE("冒險", "冒險和探索相關模組"),
        CUSTOM("自定義", "可自由配置的模組插口");
        
        private final String displayName;
        private final String description;
        
        Category(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public ModSlot(String id, String name, String description, boolean required, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.required = required;
        this.category = category;
        this.mods = new ArrayList<>();
    }
    
    /**
     * 添加模組到插口
     */
    public boolean addMod(ModInfo mod) {
        if (mod == null) {
            return false;
        }
        
        // 檢查是否已存在相同的模組
        if (mods.stream().anyMatch(m -> m.getModId().equals(mod.getModId()))) {
            return false;
        }
        
        return mods.add(mod);
    }
    
    /**
     * 從插口移除模組
     */
    public boolean removeMod(String fileName) {
        return mods.removeIf(mod -> mod.getFileName().equals(fileName));
    }
    
    /**
     * 根據模組 ID 移除模組
     */
    public boolean removeModById(String modId) {
        return mods.removeIf(mod -> mod.getModId().equals(modId));
    }
    
    /**
     * 獲取指定文件名的模組
     */
    public ModInfo getMod(String fileName) {
        return mods.stream()
                .filter(mod -> mod.getFileName().equals(fileName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根據模組 ID 獲取模組
     */
    public ModInfo getModById(String modId) {
        return mods.stream()
                .filter(mod -> mod.getModId().equals(modId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 檢查插口是否為空
     */
    public boolean isEmpty() {
        return mods.isEmpty();
    }
    
    /**
     * 獲取插口中的模組數量
     */
    public int getModCount() {
        return mods.size();
    }
    
    /**
     * 獲取啟用的模組數量
     */
    public int getEnabledModCount() {
        return (int) mods.stream().filter(ModInfo::isEnabled).count();
    }
    
    /**
     * 檢查插口是否有啟用的模組
     */
    public boolean hasEnabledMods() {
        return mods.stream().anyMatch(ModInfo::isEnabled);
    }
    
    /**
     * 啟用插口中的所有模組
     */
    public void enableAllMods() {
        mods.forEach(mod -> mod.setEnabled(true));
    }
    
    /**
     * 禁用插口中的所有模組
     */
    public void disableAllMods() {
        mods.forEach(mod -> mod.setEnabled(false));
    }
    
    /**
     * 清空插口中的所有模組
     */
    public void clearMods() {
        mods.clear();
    }
    
    /**
     * 獲取插口狀態描述
     */
    public String getStatusDescription() {
        if (isEmpty()) {
            return "空插口";
        }
        
        int total = getModCount();
        int enabled = getEnabledModCount();
        
        if (enabled == 0) {
            return String.format("%d 個模組 (全部禁用)", total);
        } else if (enabled == total) {
            return String.format("%d 個模組 (全部啟用)", total);
        } else {
            return String.format("%d 個模組 (%d 啟用, %d 禁用)", total, enabled, total - enabled);
        }
    }
    
    /**
     * 檢查插口是否滿足要求
     * 對於必需插口，至少要有一個啟用的模組
     */
    public boolean isSatisfied() {
        if (!required) {
            return true;
        }
        
        return hasEnabledMods();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public List<ModInfo> getMods() {
        return new ArrayList<>(mods);
    }
    
    @Override
    public String toString() {
        return String.format("ModSlot{id='%s', name='%s', category=%s, mods=%d, enabled=%d, required=%s}",
                id, name, category, getModCount(), getEnabledModCount(), required);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModSlot modSlot = (ModSlot) o;
        return Objects.equals(id, modSlot.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}