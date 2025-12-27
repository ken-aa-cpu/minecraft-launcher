package com.mcserver.launcher.mod;

import java.util.Objects;

/**
 * 模組信息類
 * 存儲單個模組的詳細信息
 */
public class ModInfo {
    
    private String fileName;
    private final String modId;
    private final String name;
    private final String version;
    private final String description;
    private boolean enabled;
    
    // 可選的額外信息
    private String author;
    private String website;
    private String[] dependencies;
    private long fileSize;
    private String checksum;
    
    public ModInfo(String fileName, String modId, String name, String version, String description, boolean enabled) {
        this.fileName = fileName;
        this.modId = modId;
        this.name = name;
        this.version = version;
        this.description = description;
        this.enabled = enabled;
    }
    
    /**
     * 完整構造函數
     */
    public ModInfo(String fileName, String modId, String name, String version, String description, 
                   boolean enabled, String author, String website, String[] dependencies, 
                   long fileSize, String checksum) {
        this(fileName, modId, name, version, description, enabled);
        this.author = author;
        this.website = website;
        this.dependencies = dependencies;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }
    
    /**
     * 檢查模組是否有效
     */
    public boolean isValid() {
        return fileName != null && !fileName.isEmpty() &&
               modId != null && !modId.isEmpty() &&
               name != null && !name.isEmpty();
    }
    
    /**
     * 獲取模組顯示名稱
     * 如果有自定義名稱則使用自定義名稱，否則使用模組 ID
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty() && !name.equals(modId)) {
            return name;
        }
        return modId;
    }
    
    /**
     * 獲取模組完整描述
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("模組 ID: ").append(modId).append("\n");
        sb.append("版本: ").append(version).append("\n");
        
        if (author != null && !author.isEmpty()) {
            sb.append("作者: ").append(author).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            sb.append("描述: ").append(description).append("\n");
        }
        
        if (website != null && !website.isEmpty()) {
            sb.append("網站: ").append(website).append("\n");
        }
        
        if (dependencies != null && dependencies.length > 0) {
            sb.append("依賴: ").append(String.join(", ", dependencies)).append("\n");
        }
        
        sb.append("文件: ").append(fileName).append("\n");
        sb.append("狀態: ").append(enabled ? "啟用" : "禁用");
        
        return sb.toString();
    }
    
    /**
     * 獲取模組狀態圖標
     */
    public String getStatusIcon() {
        return enabled ? "✓" : "✗";
    }
    
    /**
     * 獲取模組狀態文字
     */
    public String getStatusText() {
        return enabled ? "啟用" : "禁用";
    }
    
    /**
     * 檢查是否為核心模組
     */
    public boolean isCoremod() {
        return modId.toLowerCase().contains("core") || 
               modId.toLowerCase().contains("api") ||
               fileName.toLowerCase().contains("core");
    }
    
    /**
     * 檢查是否為客戶端模組
     */
    public boolean isClientSide() {
        return modId.toLowerCase().contains("client") ||
               name.toLowerCase().contains("client") ||
               modId.toLowerCase().contains("optifine") ||
               modId.toLowerCase().contains("jei");
    }
    
    /**
     * 檢查是否為服務端模組
     */
    public boolean isServerSide() {
        return modId.toLowerCase().contains("server") ||
               name.toLowerCase().contains("server");
    }
    
    /**
     * 獲取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) {
            return "未知";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 創建模組的副本
     */
    public ModInfo copy() {
        return new ModInfo(fileName, modId, name, version, description, enabled, 
                          author, website, dependencies, fileSize, checksum);
    }
    
    // Getters and Setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public String[] getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(String[] dependencies) {
        this.dependencies = dependencies;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    @Override
    public String toString() {
        return String.format("ModInfo{fileName='%s', modId='%s', name='%s', version='%s', enabled=%s}",
                fileName, modId, name, version, enabled);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModInfo modInfo = (ModInfo) o;
        return Objects.equals(modId, modInfo.modId) && Objects.equals(version, modInfo.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(modId, version);
    }
}