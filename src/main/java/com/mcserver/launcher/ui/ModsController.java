package com.mcserver.launcher.ui;

import com.mcserver.launcher.mod.ModInfo;
import com.mcserver.launcher.mod.ModManager;
import com.mcserver.launcher.mod.ModSlot;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * 模組管理對話框控制器
 * 處理模組插槽和模組的管理
 */
public class ModsController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(ModsController.class);
    
    // 頂部控制按鈕
    @FXML private Label modCountLabel;
    @FXML private Button addModButton;
    @FXML private Button removeModButton;
    @FXML private Button enableAllButton;
    @FXML private Button disableAllButton;
    @FXML private Button refreshButton;
    
    // 左側插槽列表
    @FXML private ListView<ModSlot> slotsListView;
    
    // 右側插槽詳細信息
    @FXML private Label slotStatusLabel;
    @FXML private TitledPane slotInfoPane;
    @FXML private Label slotNameLabel;
    @FXML private Label slotCategoryLabel;
    @FXML private Label slotDescriptionLabel;
    @FXML private Label slotRequiredLabel;
    
    // 模組表格
    @FXML private TitledPane modsPane;
    @FXML private TableView<ModInfo> modsTableView;
    @FXML private TableColumn<ModInfo, Boolean> modEnabledColumn;
    @FXML private TableColumn<ModInfo, String> modNameColumn;
    @FXML private TableColumn<ModInfo, String> modVersionColumn;
    @FXML private TableColumn<ModInfo, String> modFileColumn;
    
    // 模組操作按鈕
    @FXML private Button enableModButton;
    @FXML private Button disableModButton;
    @FXML private Button removeFromSlotButton;
    @FXML private Button modInfoButton;
    
    // 底部按鈕
    @FXML private Label statusLabel;
    @FXML private Button applyButton;
    @FXML private Button closeButton;
    
    private ModManager modManager;
    private Stage dialogStage;
    private ObservableList<ModSlot> slotsList;
    private ObservableList<ModInfo> modsList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化模組管理對話框");
        
        setupSlotsListView();
        setupModsTableView();
        setupEventHandlers();
        
        slotsList = FXCollections.observableArrayList();
        modsList = FXCollections.observableArrayList();
        
        slotsListView.setItems(slotsList);
        modsTableView.setItems(modsList);
    }
    
    /**
     * 設置插槽列表視圖
     */
    private void setupSlotsListView() {
        slotsListView.setCellFactory(listView -> new ListCell<ModSlot>() {
            @Override
            protected void updateItem(ModSlot slot, boolean empty) {
                super.updateItem(slot, empty);
                if (empty || slot == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(slot.getName() + " (" + slot.getModCount() + ")");
                    
                    // 根據插槽狀態設置樣式
                    if (slot.isRequired() && !slot.isSatisfied()) {
                        setStyle("-fx-text-fill: #e74c3c;"); // 紅色：必需但未滿足
                    } else if (slot.hasEnabledMods()) {
                        setStyle("-fx-text-fill: #27ae60;"); // 綠色：有啟用的模組
                    } else if (!slot.isEmpty()) {
                        setStyle("-fx-text-fill: #f39c12;"); // 橙色：有模組但未啟用
                    } else {
                        setStyle("-fx-text-fill: #95a5a6;"); // 灰色：空插槽
                    }
                }
            }
        });
        
        slotsListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateSlotDetails(newValue)
        );
    }
    
    /**
     * 設置模組表格視圖
     */
    private void setupModsTableView() {
        // 啟用狀態列
        modEnabledColumn.setCellValueFactory(cellData -> 
            new SimpleBooleanProperty(cellData.getValue().isEnabled())
        );
        modEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(modEnabledColumn));
        modEnabledColumn.setEditable(true);
        
        // 模組名稱列
        modNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName())
        );
        
        // 版本列
        modVersionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getVersion())
        );
        
        // 檔案名稱列
        modFileColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFileName())
        );
        
        modsTableView.setEditable(true);
        modsTableView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateModButtons(newValue)
        );
    }
    
    /**
     * 設置事件處理器
     */
    private void setupEventHandlers() {
        addModButton.setOnAction(e -> handleAddMod());
        removeModButton.setOnAction(e -> handleRemoveMod());
        enableAllButton.setOnAction(e -> handleEnableAll());
        disableAllButton.setOnAction(e -> handleDisableAll());
        refreshButton.setOnAction(e -> handleRefresh());
        
        enableModButton.setOnAction(e -> handleEnableMod());
        disableModButton.setOnAction(e -> handleDisableMod());
        removeFromSlotButton.setOnAction(e -> handleRemoveFromSlot());
        modInfoButton.setOnAction(e -> handleShowModInfo());
        
        applyButton.setOnAction(e -> handleApply());
        closeButton.setOnAction(e -> handleClose());
    }
    
    /**
     * 設置模組管理器
     */
    public void setModManager(ModManager modManager) {
        this.modManager = modManager;
        loadModSlots();
    }
    
    /**
     * 設置對話框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * 載入模組插槽
     */
    private void loadModSlots() {
        if (modManager == null) return;
        
        Map<String, ModSlot> slots = modManager.getModSlots();
        slotsList.clear();
        slotsList.addAll(slots.values());
        
        updateModCount();
        
        // 選擇第一個插槽
        if (!slotsList.isEmpty()) {
            slotsListView.getSelectionModel().selectFirst();
        }
    }
    
    /**
     * 更新插槽詳細信息
     */
    private void updateSlotDetails(ModSlot slot) {
        if (slot == null) {
            clearSlotDetails();
            return;
        }
        
        slotNameLabel.setText(slot.getName());
        slotCategoryLabel.setText(slot.getCategory().getDisplayName());
        slotDescriptionLabel.setText(slot.getDescription());
        slotRequiredLabel.setText(slot.isRequired() ? "是" : "否");
        slotStatusLabel.setText(slot.getStatusDescription());
        
        // 更新模組列表
        modsList.clear();
        modsList.addAll(slot.getMods());
        
        modsPane.setText("插槽中的模組 (" + slot.getModCount() + ")");
    }
    
    /**
     * 清空插槽詳細信息
     */
    private void clearSlotDetails() {
        slotNameLabel.setText("");
        slotCategoryLabel.setText("");
        slotDescriptionLabel.setText("");
        slotRequiredLabel.setText("");
        slotStatusLabel.setText("");
        modsList.clear();
        modsPane.setText("插槽中的模組");
    }
    
    /**
     * 更新模組操作按鈕狀態
     */
    private void updateModButtons(ModInfo mod) {
        boolean hasSelection = mod != null;
        enableModButton.setDisable(!hasSelection || mod.isEnabled());
        disableModButton.setDisable(!hasSelection || !mod.isEnabled());
        removeFromSlotButton.setDisable(!hasSelection);
        modInfoButton.setDisable(!hasSelection);
    }
    
    /**
     * 更新模組計數
     */
    private void updateModCount() {
        if (modManager == null) {
            modCountLabel.setText("已載入: 0 個模組");
            return;
        }
        
        int totalMods = modManager.getTotalModCount();
        int enabledMods = modManager.getEnabledModCount();
        modCountLabel.setText(String.format("已載入: %d 個模組 (%d 啟用)", totalMods, enabledMods));
    }
    
    /**
     * 添加模組
     */
    @FXML
    private void handleAddMod() {
        ModSlot selectedSlot = slotsListView.getSelectionModel().getSelectedItem();
        if (selectedSlot == null) {
            showAlert("請先選擇一個插槽", Alert.AlertType.WARNING);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("選擇模組文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Minecraft 模組", "*.jar")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(dialogStage);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                addModToSlot(selectedSlot, file.toPath());
            }
        }
    }
    
    /**
     * 添加模組到插槽
     */
    private void addModToSlot(ModSlot slot, Path modFile) {
        statusLabel.setText("正在添加模組: " + modFile.getFileName());
        
        modManager.addModToSlot(slot.getId(), modFile, progress -> {
            // 進度回調（可以添加進度條）
        }).thenAccept(success -> {
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    statusLabel.setText("模組添加成功: " + modFile.getFileName());
                    refreshCurrentSlot();
                } else {
                    statusLabel.setText("模組添加失敗: " + modFile.getFileName());
                    showAlert("添加模組失敗，請檢查文件是否有效", Alert.AlertType.ERROR);
                }
            });
        });
    }
    
    /**
     * 移除模組
     */
    @FXML
    private void handleRemoveMod() {
        // 這裡可以實現從文件系統移除模組的功能
        showAlert("功能開發中", Alert.AlertType.INFORMATION);
    }
    
    /**
     * 啟用所有模組
     */
    @FXML
    private void handleEnableAll() {
        ModSlot selectedSlot = slotsListView.getSelectionModel().getSelectedItem();
        if (selectedSlot != null) {
            selectedSlot.enableAllMods();
            refreshCurrentSlot();
            statusLabel.setText("已啟用插槽中的所有模組");
        }
    }
    
    /**
     * 禁用所有模組
     */
    @FXML
    private void handleDisableAll() {
        ModSlot selectedSlot = slotsListView.getSelectionModel().getSelectedItem();
        if (selectedSlot != null) {
            selectedSlot.disableAllMods();
            refreshCurrentSlot();
            statusLabel.setText("已禁用插槽中的所有模組");
        }
    }
    
    /**
     * 重新整理
     */
    @FXML
    private void handleRefresh() {
        loadModSlots();
        statusLabel.setText("已重新整理模組列表");
    }
    
    /**
     * 啟用選中的模組
     */
    @FXML
    private void handleEnableMod() {
        ModInfo selectedMod = modsTableView.getSelectionModel().getSelectedItem();
        if (selectedMod != null) {
            selectedMod.setEnabled(true);
            modsTableView.refresh();
            updateModCount();
            statusLabel.setText("已啟用模組: " + selectedMod.getName());
        }
    }
    
    /**
     * 禁用選中的模組
     */
    @FXML
    private void handleDisableMod() {
        ModInfo selectedMod = modsTableView.getSelectionModel().getSelectedItem();
        if (selectedMod != null) {
            selectedMod.setEnabled(false);
            modsTableView.refresh();
            updateModCount();
            statusLabel.setText("已禁用模組: " + selectedMod.getName());
        }
    }
    
    /**
     * 從插槽移除模組
     */
    @FXML
    private void handleRemoveFromSlot() {
        ModInfo selectedMod = modsTableView.getSelectionModel().getSelectedItem();
        ModSlot selectedSlot = slotsListView.getSelectionModel().getSelectedItem();
        
        if (selectedMod != null && selectedSlot != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("確認移除");
            alert.setHeaderText("移除模組");
            alert.setContentText("確定要從插槽中移除模組 \"" + selectedMod.getName() + "\" 嗎？\n\n注意：這將刪除模組文件！");
            
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean success = modManager.removeModFromSlot(selectedSlot.getId(), selectedMod.getFileName());
                if (success) {
                    refreshCurrentSlot();
                    statusLabel.setText("已移除模組: " + selectedMod.getName());
                } else {
                    showAlert("移除模組失敗", Alert.AlertType.ERROR);
                }
            }
        }
    }
    
    /**
     * 顯示模組信息
     */
    @FXML
    private void handleShowModInfo() {
        ModInfo selectedMod = modsTableView.getSelectionModel().getSelectedItem();
        if (selectedMod != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("模組信息");
            alert.setHeaderText(selectedMod.getName());
            
            String content = String.format(
                "模組 ID: %s\n" +
                "版本: %s\n" +
                "檔案名稱: %s\n" +
                "狀態: %s\n" +
                "描述: %s",
                selectedMod.getModId(),
                selectedMod.getVersion(),
                selectedMod.getFileName(),
                selectedMod.getStatusText(),
                selectedMod.getDescription()
            );
            
            alert.setContentText(content);
            alert.showAndWait();
        }
    }
    
    /**
     * 套用變更
     */
    @FXML
    private void handleApply() {
        // 這裡可以實現套用模組變更的邏輯
        statusLabel.setText("變更已套用");
    }
    
    /**
     * 關閉對話框
     */
    @FXML
    private void handleClose() {
        dialogStage.close();
    }
    
    /**
     * 重新整理當前選中的插槽
     */
    private void refreshCurrentSlot() {
        ModSlot selectedSlot = slotsListView.getSelectionModel().getSelectedItem();
        if (selectedSlot != null) {
            updateSlotDetails(selectedSlot);
            slotsListView.refresh();
            updateModCount();
        }
    }
    
    /**
     * 顯示警告對話框
     */
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "錯誤" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}