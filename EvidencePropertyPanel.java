import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.Serializable;

/**
 * EvidencePropertyPanel 類別 - 證物標示專屬屬性面板
 * 繼承自 JPanel
 * 用於顯示和編輯證物標示的屬性（編號、補充說明）
 */
public class EvidencePropertyPanel extends JPanel {
    
    // 對主視窗和當前畫布的引用
    private MainWindow mainWindow;
    private Page currentPage;
    
    // UI 元件
    private JTextField numberField;       // 編號輸入欄位
    private JTextArea descriptionArea;    // 補充說明文字區域
    
    private boolean isUpdating = false;   // 防止循環更新的旗標
    
    /**
     * 建構子
     * @param window 對 MainWindow 的引用
     */
    public EvidencePropertyPanel(MainWindow window) {
        this.mainWindow = window;
        
        // 設定面板佈局和外觀
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setPreferredSize(new Dimension(200, 0)); // 寬度 200 像素
        this.setBackground(new Color(200, 200, 200)); // 灰色背景
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 內邊距
        
        // --- 建立標題 ---
        JLabel titleLabel = new JLabel("證物標示");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(titleLabel);
        this.add(Box.createVerticalStrut(15));
        
        // --- 建立編號輸入區 ---
        JPanel numberPanel = createFieldPanel("編號：");
        numberField = new JTextField();
        numberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        numberPanel.add(numberField);
        this.add(numberPanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立補充說明區 ---
        JLabel descLabel = new JLabel("補充說明：");
        descLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(descLabel);
        this.add(Box.createVerticalStrut(5));
        
        descriptionArea = new JTextArea(10, 15);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        
        this.add(scrollPane);
        this.add(Box.createVerticalGlue()); // 將剩餘空間推到底部
        
        // 初始狀態：清空所有欄位並停用
        clearFields();
        
        // 註冊欄位變更監聽器
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
        label.setPreferredSize(new Dimension(80, 25));
        
        panel.add(label);
        panel.add(Box.createHorizontalStrut(5));
        
        return panel;
    }
    
    /**
     * 設定當前的 Page（畫布）
     * @param page 新的畫布
     */
    public void setCurrentPage(Page page) {
        this.currentPage = page;
    }
    
    /**
     * 更新屬性面板，顯示選取證物標示的資訊
     * @param properties 證物標示的屬性資料
     */
    public void updateProperties(EvidenceProperties properties) {
        if (properties == null) {
            clearFields();
            return;
        }
        
        isUpdating = true; // 設定旗標，防止觸發監聽器
        
        // 顯示證物標示的各項屬性
        numberField.setText(properties.number != null ? properties.number : "1");
        descriptionArea.setText(properties.description != null ? properties.description : "");
        
        // 啟用欄位，讓使用者可以編輯
        enableFields(true);
        
        isUpdating = false; // 解除旗標
    }
    
    /**
     * 註冊欄位變更監聽器
     * 當使用者輸入時即時更新物件屬性
     */
    private void registerFieldListeners() {
        // 為所有文字欄位添加 DocumentListener
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
        
        numberField.getDocument().addDocumentListener(docListener);
        descriptionArea.getDocument().addDocumentListener(docListener);
    }
    
    /**
     * 延遲更新屬性
     */
    private void updatePropertiesDelayed() {
        if (isUpdating) return;
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updatePropertiesImmediate();
            }
        });
    }
    
    /**
     * 立即更新物件屬性
     */
    private void updatePropertiesImmediate() {
        if (isUpdating) return;
        if (currentPage == null) return;
        
        EvidenceProperties props = new EvidenceProperties();
        props.number = numberField.getText();
        props.description = descriptionArea.getText();
        
        currentPage.updateSelectedEvidenceProperties(props);
    }
    
    /**
     * 清空所有欄位並停用
     */
    public void clearFields() {
        isUpdating = true;
        
        numberField.setText("");
        descriptionArea.setText("");
        
        // 停用所有欄位
        enableFields(false);
        
        isUpdating = false;
    }
    
    /**
     * 啟用或停用所有欄位
     * @param enabled 是否啟用
     */
    private void enableFields(boolean enabled) {
        numberField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
    }
    
    /**
     * 內部類別：證物標示屬性資料結構
     */
    public static class EvidenceProperties implements Serializable {
        private static final long serialVersionUID = 5L;
        public String number;       // 編號
        public String description;  // 補充說明
    }
}