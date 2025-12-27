# 安裝指南

本指南將幫助您設置和運行 Minecraft Forge 1.20.1 啟動器。

## 前置需求

### 1. Java 17+
✅ **已檢測到**: OpenJDK 21.0.7 (已滿足需求)

### 2. JavaFX SDK
❌ **需要安裝**

1. 前往 [OpenJFX 官網](https://openjfx.io/)
2. 下載 JavaFX SDK (建議版本 17+)
3. 解壓到 `C:\javafx\` 目錄
4. 確保路徑為 `C:\javafx\lib\`

### 3. Maven (推薦)
❌ **需要安裝**

1. 前往 [Maven 官網](https://maven.apache.org/download.cgi)
2. 下載 Binary zip archive
3. 解壓到 `C:\apache-maven\`
4. 添加 `C:\apache-maven\bin` 到系統 PATH
5. 重啟命令提示字元
6. 驗證安裝：`mvn -version`

## 快速開始

### 方法一：使用 Maven (推薦)

```bash
# 1. 編譯項目
mvn clean compile

# 2. 運行啟動器
mvn javafx:run

# 3. 打包為可執行檔
mvn clean package
```

### 方法二：手動編譯

```bash
# 1. 下載依賴庫
.\download-dependencies.bat

# 2. 編譯源碼
.\compile.bat

# 3. 運行啟動器
.\run.bat
```

## 詳細安裝步驟

### 步驟 1：安裝 JavaFX

1. **下載 JavaFX SDK**
   - 訪問 https://openjfx.io/
   - 選擇 "JavaFX 17" 或更高版本
   - 下載 Windows x64 SDK

2. **解壓和配置**
   ```
   解壓到：C:\javafx\
   確保存在：C:\javafx\lib\javafx.controls.jar
   ```

3. **驗證安裝**
   ```bash
   dir C:\javafx\lib
   # 應該看到 javafx.*.jar 文件
   ```

### 步驟 2：安裝 Maven (可選但推薦)

1. **下載 Maven**
   - 訪問 https://maven.apache.org/download.cgi
   - 下載 "Binary zip archive"

2. **解壓和配置**
   ```
   解壓到：C:\apache-maven\
   添加到 PATH：C:\apache-maven\bin
   ```

3. **驗證安裝**
   ```bash
   mvn -version
   # 應該顯示 Maven 版本信息
   ```

### 步驟 3：編譯和運行

#### 使用 Maven
```bash
# 進入項目目錄
cd C:\Users\qq037\OneDrive\Desktop\mcl

# 編譯項目
mvn clean compile

# 運行啟動器
mvn javafx:run
```

#### 手動編譯
```bash
# 1. 下載依賴
.\download-dependencies.bat

# 2. 編譯
.\compile.bat

# 3. 修改 run.bat 中的 JavaFX 路徑
# 編輯 run.bat，確保 JAVAFX_PATH 指向正確位置

# 4. 運行
.\run.bat
```

## 故障排除

### 問題 1：找不到 JavaFX 模組
**錯誤**: `Error: JavaFX runtime components are missing`

**解決方案**:
1. 確認 JavaFX SDK 已正確安裝
2. 檢查 `run.bat` 中的 `JAVAFX_PATH` 設定
3. 確保路徑不包含空格或特殊字符

### 問題 2：編譯錯誤
**錯誤**: `package does not exist`

**解決方案**:
1. 確認所有依賴庫都在 `lib/` 目錄中
2. 運行 `download-dependencies.bat`
3. 檢查網路連接

### 問題 3：Maven 命令不存在
**錯誤**: `'mvn' is not recognized`

**解決方案**:
1. 安裝 Maven
2. 添加 Maven bin 目錄到 PATH
3. 重啟命令提示字元
4. 或使用手動編譯方法

### 問題 4：記憶體不足
**錯誤**: `OutOfMemoryError`

**解決方案**:
1. 修改 `run.bat`，添加 JVM 參數：
   ```
   java -Xmx2G -Xms1G ...
   ```

## 項目結構說明

```
mcl/
├── src/main/java/          # Java 源碼
├── src/main/resources/     # 資源文件 (FXML, CSS, 圖標)
├── lib/                    # 依賴庫 (手動下載)
├── target/classes/         # 編譯輸出
├── pom.xml                 # Maven 配置
├── compile.bat             # 手動編譯腳本
├── run.bat                 # 運行腳本
├── download-dependencies.bat # 依賴下載腳本
└── README.md               # 項目說明
```

## 下一步

安裝完成後，您可以：

1. **配置啟動器**
   - 設定 Minecraft 安裝路徑
   - 配置 Java 參數
   - 設定記憶體分配

2. **管理模組**
   - 添加 Forge 1.20.1 模組
   - 使用模組插槽系統
   - 啟用/禁用模組

3. **Microsoft 登入**
   - 使用 Microsoft 帳戶登入
   - 自動保存登入狀態

4. **自動更新**
   - 配置 GitHub 存儲庫
   - 啟用自動更新檢查

## 支援

如果遇到問題，請：

1. 檢查本安裝指南
2. 查看 [README.md](README.md) 中的常見問題
3. 在 GitHub Issues 中報告問題

---

**注意**: 此啟動器需要有效的 Minecraft 授權和 Microsoft 帳戶。