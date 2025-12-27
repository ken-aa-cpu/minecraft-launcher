@echo off
echo 正在編譯 Minecraft Forge 1.20.1 啟動器...

:: 創建輸出目錄
if not exist "target\classes" mkdir "target\classes"

:: 設定類路徑 (需要下載相關 JAR 檔案)
set CLASSPATH=lib\*;target\classes

:: 編譯 Java 源碼
echo 編譯 Java 源碼...
javac -cp "%CLASSPATH%" -d target\classes src\main\java\com\mcserver\launcher\*.java src\main\java\com\mcserver\launcher\auth\*.java src\main\java\com\mcserver\launcher\config\*.java src\main\java\com\mcserver\launcher\download\*.java src\main\java\com\mcserver\launcher\launcher\*.java src\main\java\com\mcserver\launcher\minecraft\*.java src\main\java\com\mcserver\launcher\mod\*.java src\main\java\com\mcserver\launcher\update\*.java src\main\java\com\mcserver\launcher\github\*.java

if %ERRORLEVEL% neq 0 (
    echo 編譯失敗！請檢查錯誤訊息。
    pause
    exit /b 1
)

:: 複製資源文件
echo 複製資源文件...
if not exist "target\classes" mkdir "target\classes"
xcopy "src\main\resources\*" "target\classes\" /E /Y

echo 編譯完成！
echo.
echo 注意：此項目需要以下依賴庫：
echo - JavaFX 17+
echo - Jackson (JSON 處理)
echo - SLF4J + Logback (日誌)
echo - Apache Commons Compress
echo - Microsoft Graph API
echo.
echo 建議安裝 Maven 來自動管理依賴：
echo 1. 下載 Maven: https://maven.apache.org/download.cgi
echo 2. 解壓並設定 PATH 環境變數
echo 3. 執行: mvn clean compile
echo.
pause