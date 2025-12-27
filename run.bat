@echo off
echo 啟動 Minecraft Forge 1.20.1 啟動器...

:: 檢查是否已編譯
if not exist "target\classes\com\mcserver\launcher\LauncherApplication.class" (
    echo 錯誤：找不到編譯後的類文件！
    echo 請先執行 compile.bat 編譯項目。
    pause
    exit /b 1
)

:: 設定類路徑
set CLASSPATH=lib\*;target\classes

:: 設定 JavaFX 模組路徑（需要根據實際 JavaFX 安裝路徑調整）
set JAVAFX_PATH=javafx-sdk-17.0.2\lib

:: 啟動應用程式
echo 正在啟動啟動器...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%CLASSPATH%" com.mcserver.launcher.LauncherApplication

if %ERRORLEVEL% neq 0 (
    echo.
    echo 啟動失敗！可能的原因：
    echo 1. JavaFX 未正確安裝或路徑設定錯誤
    echo 2. 缺少必要的依賴庫
    echo 3. Java 版本不相容
    echo.
    echo 解決方案：
    echo 1. 下載 JavaFX SDK: https://openjfx.io/
    echo 2. 修改 run.bat 中的 JAVAFX_PATH 變數
    echo 3. 確保所有依賴庫都在 lib 資料夾中
    echo.
)

pause