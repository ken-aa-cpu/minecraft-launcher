package com.mcserver.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcserver.launcher.config.LauncherConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 微軟帳號認證處理器
 * 實現 Device Code Flow 流程和 Minecraft 服務認證
 */
public class MicrosoftAuthenticator {
    
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftAuthenticator.class);
    
    // Microsoft Device Code Flow 端點
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/devicecode";
    private static final String MICROSOFT_TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    
    // Xbox Live 認證端點
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    
    // Minecraft 服務端點
    private static final String MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    
    // 應用程序配置
    private static final String CLIENT_ID = "031fb156-0927-4ff1-9e7e-0c5de9bfa474";
    // Modified Scope: Simplified to core Minecraft permissions to avoid conflict
    private static final String SCOPE = "XboxLive.Signin offline_access";
    
    private final LauncherConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    private MinecraftSession currentSession;
    
    public MicrosoftAuthenticator(LauncherConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
        
        // 嘗試從配置恢復會話
        if (config.hasValidSession()) {
            this.currentSession = new MinecraftSession(
                config.getSessionUsername(),
                config.getSessionUuid(),
                config.getSessionAccessToken(),
                config.getSessionRefreshToken()
            );
            logger.info("已恢復保存的會話: {}", currentSession.getUsername());
        }
    }
    
    /**
     * 嘗試自動登入 (刷新 Token)
     */
    public String refreshAndAuthenticate() throws Exception {
        if (!config.hasValidSession()) {
            throw new Exception("沒有保存的會話");
        }
        
        logger.info("嘗試使用 Refresh Token 自動登入...");
        String refreshToken = config.getSessionRefreshToken();
        
        // 1. 使用 Refresh Token 獲取新的 Microsoft Access Token
        TokenResponse tokenResponse = refreshMicrosoftToken(refreshToken);
        
        // 2. 繼續標準認證流程
        return completeAuthenticationFlow(tokenResponse);
    }

    private TokenResponse refreshMicrosoftToken(String refreshToken) throws Exception {
        HttpPost post = new HttpPost(MICROSOFT_TOKEN_URL);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", CLIENT_ID));
        params.add(new BasicNameValuePair("scope", SCOPE));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        
        try (var response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("access_token") && json.has("refresh_token")) {
                return new TokenResponse(
                    json.get("access_token").asText(),
                    json.get("refresh_token").asText()
                );
            } else {
                throw new Exception("Token 刷新失敗: " + responseBody);
            }
        }
    }

    /**
     * 執行完整的微軟帳號認證流程 (Device Code Flow)
     */
    public String authenticate() throws Exception {
        logger.info("=== 開始微軟帳號認證流程 (Device Code Flow) ===");
        
        // 步驟 1: 請求 Device Code
        DeviceCodeResponse deviceCode = requestDeviceCode();
        logger.info("取得 Device Code: " + deviceCode.userCode);
        
        // 步驟 2: 顯示給使用者並自動開啟瀏覽器
        CompletableFuture<Void> userNotified = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                // 嘗試開啟瀏覽器
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + deviceCode.verificationUri);
                } else {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(deviceCode.verificationUri));
                }
            } catch (Exception e) {
                logger.error("無法開啟瀏覽器", e);
            }

            // 顯示對話框
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("微軟登入");
            alert.setHeaderText("請完成登入驗證");
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            content.setAlignment(Pos.CENTER_LEFT);
            
            Label info = new Label("瀏覽器已開啟，請在網頁中輸入以下代碼：");
            
            TextField codeField = new TextField(deviceCode.userCode);
            codeField.setEditable(false);
            codeField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
            
            ButtonType copyButton = new ButtonType("複製代碼", ButtonBar.ButtonData.OTHER);
            alert.getButtonTypes().add(copyButton);
            
            content.getChildren().addAll(info, codeField);
            alert.getDialogPane().setContent(content);
            
            // 處理按鈕事件
            final javafx.scene.control.Button btn = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(copyButton);
            btn.setOnAction(event -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(deviceCode.userCode);
                clipboard.setContent(clipboardContent);
                btn.setText("已複製！");
            });

            // 不阻塞，顯示後立即返回，讓背景線程可以繼續 Poll
            alert.initModality(Modality.NONE); 
            alert.show();
            
            userNotified.complete(null);
        });
        
        // 等待 UI 顯示完成 (雖然是異步，但確保順序)
        userNotified.get();
        
        // 步驟 3: 輪詢 Token
        logger.info("等待用戶在瀏覽器完成登入...");
        TokenResponse tokenResponse = pollForToken(deviceCode);
        logger.info("Microsoft Access Token 獲取成功");
        
        // 步驟 4-6: 完成 Xbox/Minecraft 認證鏈
        return completeAuthenticationFlow(tokenResponse);
    }
    
    private DeviceCodeResponse requestDeviceCode() throws Exception {
        HttpPost post = new HttpPost(DEVICE_CODE_URL);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", CLIENT_ID));
        params.add(new BasicNameValuePair("scope", SCOPE));
        
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        
        try (var response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("device_code") && json.has("user_code") && json.has("verification_uri")) {
                return new DeviceCodeResponse(
                    json.get("device_code").asText(),
                    json.get("user_code").asText(),
                    json.get("verification_uri").asText(),
                    json.get("expires_in").asInt(),
                    json.get("interval").asInt()
                );
            } else {
                throw new Exception("無法獲取 Device Code: " + responseBody);
            }
        }
    }
    
    private TokenResponse pollForToken(DeviceCodeResponse deviceCode) throws Exception {
        long expireTime = System.currentTimeMillis() + (deviceCode.expiresIn * 1000L);
        int interval = deviceCode.interval; // seconds
        
        while (System.currentTimeMillis() < expireTime) {
            Thread.sleep(interval * 1000L);
            
            HttpPost post = new HttpPost(MICROSOFT_TOKEN_URL);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
            params.add(new BasicNameValuePair("client_id", CLIENT_ID));
            params.add(new BasicNameValuePair("device_code", deviceCode.deviceCode));
            // Note: SCOPE parameter MUST NOT be included here as it was already consented in step 1
            
            post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            
            try (var response = httpClient.execute(post)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                JsonNode json = objectMapper.readTree(responseBody);
                
                if (json.has("access_token")) {
                    String refreshToken = json.has("refresh_token") ? json.get("refresh_token").asText() : "";
                    return new TokenResponse(json.get("access_token").asText(), refreshToken);
                } else if (json.has("error")) {
                    String error = json.get("error").asText();
                    if ("authorization_pending".equals(error)) {
                        // Continue polling
                        continue;
                    } else if ("slow_down".equals(error)) {
                        // Increase interval
                        interval += 5;
                        continue;
                    } else {
                        throw new Exception("Token 輪詢錯誤: " + error);
                    }
                }
            }
        }
        throw new Exception("登入超時，請重試");
    }
    
    private String completeAuthenticationFlow(TokenResponse microsoftTokens) throws Exception {
        // 步驟 3: 使用 Microsoft 令牌進行 Xbox Live 認證
        logger.info("正在進行 Xbox Live 認證...");
        String xboxToken = authenticateWithXboxLive(microsoftTokens.accessToken);
        logger.info("步驟 3/5: Xbox Live 認證成功 (XBL Token Acquired)");
        
        // 步驟 4: 獲取 XSTS 令牌
        logger.info("正在獲取 XSTS 安全憑證...");
        XSTSResponse xstsResponse = getXSTSToken(xboxToken);
        logger.info("步驟 4/5: XSTS 憑證獲取成功");
        
        // 步驟 5: 使用 XSTS 令牌進行 Minecraft 認證
        logger.info("正在獲取 Minecraft Access Token...");
        String minecraftToken = authenticateWithMinecraft(xstsResponse);
        logger.info("步驟 5/5: Minecraft Access Token 獲取成功 (這是啟動遊戲的關鍵鑰匙!)");
        
        // 步驟 6: 獲取 Minecraft 用戶資料
        MinecraftProfile profile = getMinecraftProfile(minecraftToken);
        logger.info("用戶資料獲取成功: {}", profile.name);
        
        // 創建會話
        currentSession = new MinecraftSession(
            profile.name,
            profile.id,
            minecraftToken,
            microsoftTokens.refreshToken // 保存 Refresh Token 以便下次自動登入
        );
        
        // 保存會話到配置
        config.saveSession(currentSession);
        
        logger.info("=== 認證流程全部完成，已準備好啟動遊戲 ===");
        return profile.name;
    }

    // 內部輔助類
    private static class DeviceCodeResponse {
        final String deviceCode;
        final String userCode;
        final String verificationUri;
        final int expiresIn;
        final int interval;
        
        DeviceCodeResponse(String deviceCode, String userCode, String verificationUri, int expiresIn, int interval) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.expiresIn = expiresIn;
            this.interval = interval;
        }
    }
    
    private static class TokenResponse {
        final String accessToken;
        final String refreshToken;
        
        TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
    
    private String authenticateWithXboxLive(String microsoftToken) throws Exception {
        HttpPost post = new HttpPost(XBOX_AUTH_URL);
        
        String requestBody = "{" +
                "\"Properties\": {" +
                "\"AuthMethod\": \"RPS\"," +
                "\"SiteName\": \"user.auth.xboxlive.com\"," +
                "\"RpsTicket\": \"d=" + microsoftToken + "\"" +
                "}," +
                "\"RelyingParty\": \"http://auth.xboxlive.com\"," +
                "\"TokenType\": \"JWT\"" +
                "}";
        
        post.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        
        try (var response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("Token")) {
                return json.get("Token").asText();
            } else {
                throw new Exception("Xbox Live 認證失敗: " + responseBody);
            }
        }
    }
    
    private XSTSResponse getXSTSToken(String xboxToken) throws Exception {
        HttpPost post = new HttpPost(XBOX_XSTS_URL);
        
        String requestBody = "{" +
                "\"Properties\": {" +
                "\"SandboxId\": \"RETAIL\"," +
                "\"UserTokens\": [\"" + xboxToken + "\"]" +
                "}," +
                "\"RelyingParty\": \"rp://api.minecraftservices.com/\"," +
                "\"TokenType\": \"JWT\"" +
                "}";
        
        post.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        
        try (var response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("Token") && json.has("DisplayClaims")) {
                String token = json.get("Token").asText();
                String userHash = json.get("DisplayClaims").get("xui").get(0).get("uhs").asText();
                return new XSTSResponse(token, userHash);
            } else {
                throw new Exception("XSTS 認證失敗: " + responseBody);
            }
        }
    }
    
    private String authenticateWithMinecraft(XSTSResponse xstsResponse) throws Exception {
        HttpPost post = new HttpPost(MINECRAFT_AUTH_URL);
        
        String requestBody = "{" +
                "\"identityToken\": \"XBL3.0 x=" + xstsResponse.userHash + ";" + xstsResponse.token + "\"" +
                "}";
        
        post.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        
        try (var response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("access_token")) {
                return json.get("access_token").asText();
            } else {
                throw new Exception("Minecraft 認證失敗: " + responseBody);
            }
        }
    }
    
    private MinecraftProfile getMinecraftProfile(String minecraftToken) throws Exception {
        HttpGet get = new HttpGet(MINECRAFT_PROFILE_URL);
        get.setHeader("Authorization", "Bearer " + minecraftToken);
        
        try (var response = httpClient.execute(get)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode json = objectMapper.readTree(responseBody);
            
            if (json.has("name") && json.has("id")) {
                return new MinecraftProfile(
                    json.get("name").asText(),
                    json.get("id").asText()
                );
            } else {
                throw new Exception("無法獲取 Minecraft 用戶資料: " + responseBody);
            }
        }
    }
    
    public void logout() {
        currentSession = null;
        config.clearSession();
        logger.info("已登出微軟帳號");
    }
    
    public MinecraftSession getSession() {
        return currentSession;
    }
    
    public boolean isLoggedIn() {
        return currentSession != null;
    }
    
    // 內部類別
    private static class XSTSResponse {
        final String token;
        final String userHash;
        
        XSTSResponse(String token, String userHash) {
            this.token = token;
            this.userHash = userHash;
        }
    }
    
    private static class MinecraftProfile {
        final String name;
        final String id;
        
        MinecraftProfile(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
    
    public static class MinecraftSession {
        private final String username;
        private final String uuid;
        private final String accessToken;
        private final String refreshToken;
        
        public MinecraftSession(String username, String uuid, String accessToken, String refreshToken) {
            this.username = username;
            this.uuid = uuid;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
        
        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
