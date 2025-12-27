# Minecraft Forge 1.20.1 啟動器

一個專為 Minecraft 伺服器設計的現代化啟動器，支援自動更新、模組管理和 Microsoft 帳戶登入。

注意!!! 此啟動器目前開發中功能尚不完善請勿使用(目前卡在微軟azure xbox驗證階段)

## 功能特色

### 🚀 核心功能
- **Microsoft 帳戶登入**: 支援 OAuth 2.0 安全登入
- **自動更新**: 從 GitHub 自動檢查和下載更新
- **模組管理**: 預設模組插槽系統，方便添加/移除/替換模組
- **現代化 UI**: 基於 JavaFX 的美觀界面
- **一鍵啟動**: 簡化的遊戲啟動流程

### 🔧 模組系統
- **預設插槽**: 8 個預定義模組類別
  - 核心模組 (Core)
  - 優化模組 (Optimization)
  - 實用工具 (Utility)
  - 裝飾模組 (Decoration)
  - 科技模組 (Technology)
  - 魔法模組 (Magic)
  - 冒險模組 (Adventure)
  - 自定義模組 (Custom)
- **智能管理**: 自動掃描和分類模組
- **狀態控制**: 啟用/禁用模組功能

### 📦 自動更新
- **GitHub 整合**: 自動檢查 GitHub Releases
- **無縫更新**: 後台下載，重啟應用
- **版本管理**: 智能版本比較
- **回滾支援**: 保留舊版本備份

## 系統需求

- **作業系統**: Windows 10/11, macOS 10.14+, Linux
- **Java 版本**: Java 17 或更高版本
- **記憶體**: 最少 4GB RAM (推薦 8GB+)
- **儲存空間**: 至少 2GB 可用空間
- **網路連接**: 用於帳戶驗證和更新檢查

## 安裝說明

### 方法一：下載預編譯版本
1. 前往 [Releases](https://github.com/your-username/minecraft-launcher/releases) 頁面
2. 下載最新版本的 `.exe` 檔案
3. 執行安裝程式
4. 啟動啟動器

### 方法二：從原始碼編譯
```bash
# 克隆專案
git clone https://github.com/your-username/minecraft-launcher.git
cd minecraft-launcher

# 編譯專案
mvn clean compile

# 執行啟動器
mvn javafx:run

# 打包為可執行檔
mvn clean package
```

## 使用指南

### 首次設定
1. **啟動應用程式**: 執行啟動器
2. **Microsoft 登入**: 點擊「登入」按鈕，使用 Microsoft 帳戶登入
3. **設定遊戲路徑**: 在設定中指定 Minecraft 安裝路徑
4. **配置 Java**: 設定 Java 執行檔路徑和 JVM 參數

### 模組管理
1. **查看模組**: 在模組面板查看已安裝的模組
2. **添加模組**: 將 `.jar` 檔案放入對應的模組插槽資料夾
3. **啟用/禁用**: 使用切換按鈕控制模組狀態
4. **移除模組**: 從插槽中刪除不需要的模組

### 啟動遊戲
1. **確認登入**: 確保已成功登入 Microsoft 帳戶
2. **檢查模組**: 確認所需模組已啟用
3. **點擊啟動**: 點擊綠色「啟動遊戲」按鈕
4. **等待載入**: 啟動器會自動配置並啟動 Minecraft

## 專案結構

```
src/
├── main/
│   ├── java/com/mcserver/launcher/
│   │   ├── LauncherApplication.java      # 主應用程式類
│   │   ├── LauncherController.java       # UI 控制器
│   │   ├── auth/
│   │   │   ├── MicrosoftAuthenticator.java # Microsoft 認證
│   │   │   └── UserProfile.java           # 用戶資料
│   │   ├── update/
│   │   │   └── UpdateManager.java         # 更新管理
│   │   ├── mod/
│   │   │   ├── ModManager.java            # 模組管理
│   │   │   ├── ModSlot.java               # 模組插槽
│   │   │   └── ModInfo.java               # 模組資訊
│   │   ├── config/
│   │   │   └── LauncherConfig.java        # 配置管理
│   │   └── launcher/
│   │       └── MinecraftLauncher.java     # 遊戲啟動
│   └── resources/
│       ├── launcher.fxml                  # UI 佈局
│       ├── launcher.css                   # 樣式表
│       └── icon.svg                       # 應用程式圖標
└── test/                                  # 測試文件
```

## 技術棧

- **UI 框架**: JavaFX 17
- **HTTP 客戶端**: Java 11 HttpClient
- **JSON 處理**: Jackson
- **日誌系統**: SLF4J + Logback
- **認證**: Microsoft Graph API
- **壓縮**: Apache Commons Compress
- **構建工具**: Maven

- Chrome 60+
- Firefox 55+
- Safari 12+
- Edge 79+

## 開發說明

這個啟動器是一個前端展示項目，實際的 Minecraft 啟動功能需要額外的後端支援和 Minecraft 整合。

---

享受你的 Minecraft 遊戲體驗！ 🎮✨
