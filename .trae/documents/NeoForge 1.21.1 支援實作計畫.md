# NeoForge 1.21.1 自動同步與更新計畫 (最終版)

## 1. 環境分析與配置 (Analysis)
- **讀取 PrismLauncher 實例**：
    - 讀取 `c:\Users\qq037\AppData\Roaming\PrismLauncher\instances\1.21.1(2)` 中的設定檔，提取 NeoForge 版本號。
    - 確定需要打包的檔案結構。

## 2. Java 21 環境檢查 (Java Check)
- **啟動前檢查**：檢測系統預設 Java 是否為版本 21。
- **處置**：若不符合，彈出警告視窗，引導玩家前往：
  `https://www.oracle.com/tw/java/technologies/downloads/`

## 3. GitHub 模組/資源包實時同步 (Live Sync)
- **同步機制**：
    - 啟動器啟動時，強制比對 GitHub 倉庫中的 `mods` 與 `resourcepacks` 資料夾。
    - 執行**增量更新**（下載新增、刪除多餘），確保客戶端 100% 同步。
- **強制鎖定**：更新過程中顯示進度視窗，並鎖定主介面操作。

## 4. 遊戲進程互斥 (Process Lock)
- **運行檢測**：若偵測到遊戲正在運行，禁止執行更新或啟動新遊戲，並彈出視窗要求關閉遊戲。

## 5. 執行順序
1. 分析本地實例檔案。
2. 實作 Java 21 檢查邏輯。
3. 實作 GitHub 同步管理器。
4. 整合至 `LauncherController`。
