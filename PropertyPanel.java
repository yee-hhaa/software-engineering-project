import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.Serializable;

/**
 * PropertyPanel 類別 - 右側屬性面板
 * 繼承自 JPanel
 * 用於顯示和編輯選取物件的屬性（名稱、長寬高、顏色、補充說明）
 */
public class PropertyPanel extends JPanel {
    
    // 對主視窗和當前畫布的引用
    private MainWindow mainWindow;
    private Page currentPage;
    
    // UI 元件
    private JTextField nameField;        // 名稱輸入欄位
    private JTextField lengthField;      // 長度輸入欄位
    private JTextField widthField;       // 寬度輸入欄位
    private JTextField heightField;      // 高度輸入欄位
    private JButton colorButton;         // 顏色選擇按鈕
    private JTextArea descriptionArea;   // 補充說明文字區域
    private Color selectedColor;         // 目前選擇的顏色
    
    private boolean isUpdating = false;  // 防止循環更新的旗標
    
    // ⭐ 新增：套用按鈕
    private JButton applyButton;
    
    /**
     * 建構子
     * @param window 對 MainWindow 的引用
     */
    public PropertyPanel(MainWindow window) {
        this.mainWindow = window;
        this.selectedColor = Color.YELLOW; // 預設顏色為黃色
        
        // 設定面板佈局和外觀
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setPreferredSize(new Dimension(200, 0)); // 寬度 200 像素
        this.setBackground(new Color(200, 200, 200)); // 灰色背景
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 內邊距
        
        // --- 建立名稱輸入區 ---
        JPanel namePanel = createFieldPanel("名稱：");
        nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        namePanel.add(nameField);
        this.add(namePanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立長度輸入區 ---
        JPanel lengthPanel = createFieldPanel("長(y)：");
        lengthField = new JTextField();
        lengthField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        lengthPanel.add(lengthField);
        this.add(lengthPanel);
        this.add(Box.createVerticalStrut(5));
        
        // --- 建立寬度輸入區 ---
        JPanel widthPanel = createFieldPanel("寬(x)：");
        widthField = new JTextField();
        widthField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        widthPanel.add(widthField);
        this.add(widthPanel);
        this.add(Box.createVerticalStrut(5));
        
        // --- 建立高度輸入區 ---
        JPanel heightPanel = createFieldPanel("高：");
        heightField = new JTextField();
        heightField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        heightPanel.add(heightField);
        this.add(heightPanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立顏色選擇區 ---
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));
        colorPanel.setOpaque(false);
        colorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel colorLabel = new JLabel("顏色");
        colorLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        
        colorButton = new JButton("👍");
        colorButton.setBackground(selectedColor);
        colorButton.setOpaque(true);
        colorButton.setPreferredSize(new Dimension(40, 25));
        colorButton.setMaximumSize(new Dimension(40, 25));
        colorButton.addActionListener(e -> chooseColor());
        
        colorPanel.add(colorLabel);
        colorPanel.add(Box.createHorizontalStrut(10));
        colorPanel.add(colorButton);
        colorPanel.add(Box.createHorizontalGlue());
        
        this.add(colorPanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- ⭐ 新增：套用按鈕 ---
        applyButton = new JButton("套用尺寸");
        applyButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyButton.addActionListener(e -> applyDimensionChanges());
        this.add(applyButton);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立補充說明區 ---
        JLabel descLabel = new JLabel("補充說明：");
        descLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); 	
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(descLabel);
        this.add(Box.createVerticalStrut(5));
        
        descriptionArea = new JTextArea(8, 15);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        this.add(scrollPane);
        this.add(Box.createVerticalGlue()); // 將剩餘空間推到底部
        
        // 初始狀態：清空所有欄位並停用
        clearFields();
        
        // 註冊欄位變更監聽器（不包含長寬高，只即時更新名稱和說明）
        registerFieldListeners();
    }
    
    /**
     * 輔助方法：建立帶標籤的欄位面板
     * @param labelText 標籤文字
     * @return 建立好的面板
     */
    private JPanel createFieldPanel(String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        label.setPreferredSize(new Dimension(50, 25));
        
        panel.add(label);
        panel.add(Box.createHorizontalStrut(5));
        
        return panel;
    }
    
    /**
     * 顏色選擇方法
     */
    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(
            this,
            "選擇顏色",
            selectedColor
        );
        
        if (newColor != null) {
            selectedColor = newColor;
            colorButton.setBackground(selectedColor);
            
            // 立即更新屬性並通知 Page
            updatePropertiesImmediate();
        }
    }
    
    /**
     * ⭐ 新增：套用尺寸變更（點擊按鈕時呼叫）
     */
    private void applyDimensionChanges() {
        if (currentPage == null) {
            JOptionPane.showMessageDialog(this, 
                "沒有可編輯的畫布！", 
                "錯誤", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 讀取使用者輸入的數值，移除 "m" 單位
            String lengthStr = lengthField.getText().replace("m", "").trim();
            String widthStr = widthField.getText().replace("m", "").trim();
            
            double newLength = Double.parseDouble(lengthStr);
            double newWidth = Double.parseDouble(widthStr);
            
            // 驗證數值
            if (newLength <= 0 || newWidth <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "長寬必須大於 0！", 
                    "錯誤", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // ⭐ 呼叫 Page 的方法來調整物件實際大小
            currentPage.resizeSelectedShapeByMeters(newLength, newWidth);
            
            JOptionPane.showMessageDialog(this, 
                "尺寸已套用！", 
                "成功", 
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "請輸入有效的數字！", 
                "錯誤", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 設定當前的 Page（畫布）
     * @param page 新的畫布
     */
    public void setCurrentPage(Page page) {
        this.currentPage = page;
    }
    
    /**
     * 更新屬性面板，顯示選取物件的資訊
     * @param properties 物件的屬性資料
     */
    public void updateProperties(ShapeProperties properties) {
        if (properties == null) {
            clearFields();
            return;
        }
        
        isUpdating = true; // 設定旗標，防止觸發監聽器
        
        // 顯示物件的各項屬性
        nameField.setText(properties.name != null ? properties.name : "桌子");
        lengthField.setText(properties.length != null ? properties.length : "0.8m");
        widthField.setText(properties.width != null ? properties.width : "1.2m");
        heightField.setText(properties.height != null ? properties.height : "0.6m");
        descriptionArea.setText(properties.description != null ? properties.description : "");
        
        // 更新顏色按鈕
        selectedColor = properties.color != null ? properties.color : Color.YELLOW;
        colorButton.setBackground(selectedColor);
        
        // 啟用欄位，讓使用者可以編輯
        enableFields(true);
        
        isUpdating = false; // 解除旗標
    }
    
    /**
     * 註冊欄位變更監聽器
     * ⭐ 修改：長寬高不即時更新，只有名稱和說明即時更新
     */
    private void registerFieldListeners() {
        // 為名稱和說明欄位添加 DocumentListener
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePropertiesDelayed();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePropertiesDelayed();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePropertiesDelayed();
            }
        };
        
        nameField.getDocument().addDocumentListener(docListener);
        descriptionArea.getDocument().addDocumentListener(docListener);
        
        // ⭐ 長寬高欄位不加監聽器，只在按下「套用」按鈕時更新
    }
    
    /**
     * 延遲更新屬性（避免每次按鍵都更新）
     */
    private void updatePropertiesDelayed() {
        if (isUpdating) return; // 如果正在更新中，不執行
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updatePropertiesImmediate();
            }
        });
    }
    
    /**
     * 立即更新物件屬性（不包含尺寸）
     */
    private void updatePropertiesImmediate() {
        if (isUpdating) return;
        if (currentPage == null) return;
        
        ShapeProperties props = new ShapeProperties();
        props.name = nameField.getText();
        props.length = lengthField.getText();  // 只儲存文字，不改變大小
        props.width = widthField.getText();
        props.height = heightField.getText();
        props.description = descriptionArea.getText();
        props.color = selectedColor;
        
        currentPage.updateSelectedShapeProperties(props);
    }
    
    /**
     * 清空所有欄位並停用
     */
    public void clearFields() {
        isUpdating = true;
        
        nameField.setText("");
        lengthField.setText("");
        widthField.setText("");
        heightField.setText("");
        descriptionArea.setText("");
        selectedColor = Color.YELLOW;
        colorButton.setBackground(selectedColor);
        
        // 停用所有欄位，防止誤操作
        enableFields(false);
        
        isUpdating = false;
    }
    
    /**
     * 啟用或停用所有欄位
     * @param enabled 是否啟用
     */
    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        lengthField.setEnabled(enabled);
        widthField.setEnabled(enabled);
        heightField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
        colorButton.setEnabled(enabled);
        applyButton.setEnabled(enabled);  // ⭐ 新增
    }
    
    /**
     * 內部類別：物件屬性資料結構
     */
    public static class ShapeProperties implements Serializable {
        private static final long serialVersionUID = 3L;
        public String name;
        public String length;
        public String width;
        public String height;
        public String description;
        public Color color;
    }
}