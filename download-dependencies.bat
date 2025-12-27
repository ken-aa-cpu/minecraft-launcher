@echo off
echo 下載 Minecraft Forge 1.20.1 啟動器依賴庫...

:: 創建 lib 目錄
if not exist "lib" mkdir "lib"

echo.
echo 正在下載依賴庫...
echo 注意：此腳本需要網路連接和 PowerShell
echo.

:: 下載 Jackson 核心庫
echo 下載 Jackson Core...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar' -OutFile 'lib\jackson-core-2.15.2.jar'"

echo 下載 Jackson Databind...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar' -OutFile 'lib\jackson-databind-2.15.2.jar'"

echo 下載 Jackson Annotations...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar' -OutFile 'lib\jackson-annotations-2.15.2.jar'"

:: 下載 SLF4J
echo 下載 SLF4J API...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar' -OutFile 'lib\slf4j-api-2.0.7.jar'"

echo 下載 Logback Classic...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.4.8/logback-classic-1.4.8.jar' -OutFile 'lib\logback-classic-1.4.8.jar'"

echo 下載 Logback Core...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ch/qos/logback/logback-core/1.4.8/logback-core-1.4.8.jar' -OutFile 'lib\logback-core-1.4.8.jar'"

:: 下載 Apache Commons Compress
echo 下載 Apache Commons Compress...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.23.0/commons-compress-1.23.0.jar' -OutFile 'lib\commons-compress-1.23.0.jar'"

:: 下載 Microsoft Graph SDK (簡化版本，實際可能需要更多依賴)
echo 下載 Microsoft Graph Core...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/microsoft/graph/microsoft-graph-core/3.0.0/microsoft-graph-core-3.0.0.jar' -OutFile 'lib\microsoft-graph-core-3.0.0.jar'"

echo.
echo 依賴庫下載完成！
echo.
echo 注意事項：
echo 1. 此腳本下載的是基本依賴庫
echo 2. JavaFX 需要單獨下載和配置
echo 3. 某些依賴可能需要額外的傳遞依賴
echo 4. 建議使用 Maven 進行完整的依賴管理
echo.
echo JavaFX 下載地址：
echo https://openjfx.io/
echo.
echo Maven 安裝指南：
echo https://maven.apache.org/install.html
echo.
pause