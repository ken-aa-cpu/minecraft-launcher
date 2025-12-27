# GitHub 自動更新啟用計畫 (Enable GitHub Auto-Update)

好的，Ken！我將使用您的名字來設定預設配置。但我仍需要您的 **倉庫名稱 (Repo Name)** 才能完成設定。
目前我會先將預設使用者設為 `ken`，倉庫名暫定為 `minecraft-launcher` (您可以隨時修改)。

這分為兩個部分：**啟動器自我更新 (EXE)** 與 **遊戲模組更新 (Mods)**。

## 功能實作

1. **啟動器自我更新 (Launcher Update)**:

   * 原理：啟動時檢查 GitHub Releases (發布頁面)。

   * 流程：若有新版本 (Tag 更高)，自動下載 EXE/JAR 並重啟。

   * **需要您做的事**：每次更新啟動器時，在 GitHub 建立一個 "Release"，標籤 (Tag) 設為 `v1.0.1` (比當前 `v1.0.0` 高)，並上傳新的 `MCL.exe`。

2. **遊戲模組更新 (Mod Sync)**:

   * 原理：啟動時檢查 GitHub 倉庫內的 `mods` 資料夾內容。

   * 流程：比對檔案雜湊值 (Hash)，自動下載新模組、刪除舊模組。

   * **需要您做的事**：直接將新模組 Commit & Push 到 GitHub 倉庫的 `mods` 資料夾即可 (就像您平常上傳程式碼一樣)。

## 執行步驟

### 1. 程式碼修改

* [ ] **LauncherConfig**: 將預設倉庫設為 `ken/minecraft-launcher` (假設名稱，可於設定中修改)。

* [ ] **LauncherController**:
    * 在 `initialize` 階段加入 `updateManager` 的初始化。
    * 在啟動時自動執行 `checkForUpdates()`。
    * 顯示更新提示框，讓玩家選擇是否更新。

* [ ] **UpdateManager**: 優化重啟腳本，確保它能正確關閉當前的 JavaFX 應用程式並啟動新版。

### 2. 建立更新教學文檔 (`GITHUB_UPDATE_GUIDE.md`)

我會為您寫一份詳細的操作手冊，教您：

* 如何在 GitHub 建立 Release (發布新版啟動器)。

* 如何上傳新模組 (更新遊戲內容)。

請問您是否同意執行此計畫？
