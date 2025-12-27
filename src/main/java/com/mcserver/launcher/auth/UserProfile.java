package com.mcserver.launcher.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * 用戶資料類
 * 存儲 Microsoft 認證後的用戶信息
 */
public class UserProfile {
    
    private final String username;
    private final String uuid;
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;
    private final String email;
    private final String skinUrl;
    private final String capeUrl;
    
    public UserProfile(String username, String uuid, String accessToken, String refreshToken, 
                      Instant expiresAt, String email, String skinUrl, String capeUrl) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.email = email;
        this.skinUrl = skinUrl;
        this.capeUrl = capeUrl;
    }
    
    /**
     * 簡化構造函數
     */
    public UserProfile(String username, String uuid, String accessToken, String refreshToken, Instant expiresAt) {
        this(username, uuid, accessToken, refreshToken, expiresAt, null, null, null);
    }
    
    /**
     * 檢查訪問令牌是否已過期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // 如果沒有過期時間，假設未過期
        }
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * 檢查訪問令牌是否即將過期（5分鐘內）
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().plusSeconds(300).isAfter(expiresAt);
    }
    
    /**
     * 獲取剩餘有效時間（秒）
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) {
            return Long.MAX_VALUE;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
    
    /**
     * 檢查用戶資料是否有效
     */
    public boolean isValid() {
        return username != null && !username.isEmpty() &&
               uuid != null && !uuid.isEmpty() &&
               accessToken != null && !accessToken.isEmpty() &&
               !isExpired();
    }
    
    /**
     * 獲取格式化的 UUID（帶連字符）
     */
    public String getFormattedUuid() {
        if (uuid == null || uuid.length() != 32) {
            return uuid;
        }
        
        return String.format("%s-%s-%s-%s-%s",
                uuid.substring(0, 8),
                uuid.substring(8, 12),
                uuid.substring(12, 16),
                uuid.substring(16, 20),
                uuid.substring(20, 32));
    }
    
    /**
     * 獲取無連字符的 UUID
     */
    public String getPlainUuid() {
        if (uuid == null) {
            return null;
        }
        return uuid.replace("-", "");
    }
    
    /**
     * 檢查是否有皮膚
     */
    public boolean hasSkin() {
        return skinUrl != null && !skinUrl.isEmpty();
    }
    
    /**
     * 檢查是否有披風
     */
    public boolean hasCape() {
        return capeUrl != null && !capeUrl.isEmpty();
    }
    
    /**
     * 創建用戶資料的副本，但使用新的訪問令牌
     */
    public UserProfile withNewTokens(String newAccessToken, String newRefreshToken, Instant newExpiresAt) {
        return new UserProfile(username, uuid, newAccessToken, newRefreshToken, newExpiresAt, 
                              email, skinUrl, capeUrl);
    }
    
    /**
     * 創建用戶資料的副本，但使用新的皮膚和披風 URL
     */
    public UserProfile withSkinAndCape(String newSkinUrl, String newCapeUrl) {
        return new UserProfile(username, uuid, accessToken, refreshToken, expiresAt, 
                              email, newSkinUrl, newCapeUrl);
    }
    
    /**
     * 獲取用戶顯示信息
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("用戶名: ").append(username).append("\n");
        sb.append("UUID: ").append(getFormattedUuid()).append("\n");
        
        if (email != null && !email.isEmpty()) {
            sb.append("郵箱: ").append(email).append("\n");
        }
        
        if (expiresAt != null) {
            long remaining = getRemainingSeconds();
            if (remaining > 0) {
                sb.append("令牌剩餘時間: ").append(formatDuration(remaining)).append("\n");
            } else {
                sb.append("令牌狀態: 已過期\n");
            }
        }
        
        sb.append("皮膚: ").append(hasSkin() ? "有" : "無").append("\n");
        sb.append("披風: ").append(hasCape() ? "有" : "無");
        
        return sb.toString();
    }
    
    /**
     * 格式化持續時間
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + " 分鐘";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " 小時";
        } else {
            return (seconds / 86400) + " 天";
        }
    }
    
    // Getters
    public String getUsername() {
        return username;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getSkinUrl() {
        return skinUrl;
    }
    
    public String getCapeUrl() {
        return capeUrl;
    }
    
    @Override
    public String toString() {
        return String.format("UserProfile{username='%s', uuid='%s', expired=%s}", 
                username, getFormattedUuid(), isExpired());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(uuid, that.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}