import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.Serializable;

/**
 * PersonPropertyPanel 類別 - 人物專屬屬性面板
 * 繼承自 JPanel
 * 用於顯示和編輯人物的屬性（姓名、性別、身高、體重、年齡、補充說明）
 */
public class PersonPropertyPanel extends JPanel {
    
    // 對主視窗和當前畫布的引用
    private MainWindow mainWindow;
    private Page currentPage;
    
    // UI 元件
    private JTextField nameField;         // 姓名輸入欄位
    private JComboBox<String> genderBox;  // 性別下拉選單
    private JTextField heightField;       // 身高輸入欄位
    private JTextField weightField;       // 體重輸入欄位
    private JTextField ageField;          // 年齡輸入欄位
    private JTextField timeofdeathField;	 // 死亡時間輸入欄位
    private JTextArea descriptionArea;    // 補充說明文字區域
    
    private boolean isUpdating = false;   // 防止循環更新的旗標
    
    /**
     * 建構子
     * @param window 對 MainWindow 的引用
     */
    public PersonPropertyPanel(MainWindow window) {
        this.mainWindow = window;
        
        // 設定面板佈局和外觀
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setPreferredSize(new Dimension(200, 0)); // 寬度 200 像素
        this.setBackground(new Color(200, 200, 200)); // 灰色背景
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 內邊距
        
        // --- 建立姓名輸入區 ---
        JPanel namePanel = createFieldPanel("姓名：");
        nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        namePanel.add(nameField);
        this.add(namePanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立性別選擇區 ---
        JPanel genderPanel = createFieldPanel("性別：");
        genderBox = new JComboBox<>(new String[]{"男", "女", "其他"});
        genderBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        genderPanel.add(genderBox);
        this.add(genderPanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立身高輸入區 ---
        JPanel heightPanel = createFieldPanel("身高：");
        heightField = new JTextField();
        heightField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        heightPanel.add(heightField);
        this.add(heightPanel);
        this.add(Box.createVerticalStrut(5));
        
        // --- 建立體重輸入區 ---
        JPanel weightPanel = createFieldPanel("體重：");
        weightField = new JTextField();
        weightField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        weightPanel.add(weightField);
        this.add(weightPanel);
        this.add(Box.createVerticalStrut(5));
        
        // --- 建立年齡輸入區 ---
        JPanel agePanel = createFieldPanel("年齡：");
        ageField = new JTextField();
        ageField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        agePanel.add(ageField);
        this.add(agePanel);
        this.add(Box.createVerticalStrut(10));
        
        // --- 建立死亡時間輸入區 ---
        JPanel timeofdeathPanel = createFieldPanel("死亡時間：");
        timeofdeathField = new JTextField();
        timeofdeathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        timeofdeathPanel.add(timeofdeathField);
        this.add(timeofdeathPanel);
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
        label.setPreferredSize(new Dimension(80, 25));//標籤寬度
        
        panel.add(label);
        panel.add(Box.createHorizontalStrut(5));
        
        return panel;
    }
    
    /**
     * 設定當前的 Page（畫布）
     * 當新增畫布時會呼叫此方法
     * @param page 新的畫布
     */
    public void setCurrentPage(Page page) {
        this.currentPage = page;
    }
    
    /**
     * 更新屬性面板，顯示選取人物的資訊
     * @param properties 人物的屬性資料
     */
    public void updateProperties(PersonProperties properties) {
        if (properties == null) {
            clearFields();
            return;
        }
        
        isUpdating = true; // 設定旗標，防止觸發監聽器
        
        // 顯示人物的各項屬性
        nameField.setText(properties.name != null ? properties.name : "鄭子儀");
        genderBox.setSelectedItem(properties.gender != null ? properties.gender : "男");
        heightField.setText(properties.height != null ? properties.height : "174cm");
        weightField.setText(properties.weight != null ? properties.weight : "54kg");
        ageField.setText(properties.age != null ? properties.age : "23");
        timeofdeathField.setText(properties.timeofdeath != null ? properties.timeofdeath : "2200/10/31");
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
        // 為所有文字欄位添加 DocumentListener（即時監聽文字變更）
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
        heightField.getDocument().addDocumentListener(docListener);
        weightField.getDocument().addDocumentListener(docListener);
        ageField.getDocument().addDocumentListener(docListener);
        descriptionArea.getDocument().addDocumentListener(docListener);
        
        // 為性別下拉選單添加監聽器
        genderBox.addActionListener(e -> updatePropertiesDelayed());
    }
    
    /**
     * 延遲更新屬性（避免每次按鍵都更新）
     */
    private void updatePropertiesDelayed() {
        if (isUpdating) return; // 如果正在更新中，不執行
        
        // 使用 SwingUtilities.invokeLater 確保在 UI 更新後執行
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
        
        PersonProperties props = new PersonProperties();
        props.name = nameField.getText();
        props.gender = (String) genderBox.getSelectedItem();
        props.height = heightField.getText();
        props.weight = weightField.getText();
        props.age = ageField.getText();
        props.timeofdeath = timeofdeathField.getText();
        props.description = descriptionArea.getText();
        
        currentPage.updateSelectedPersonProperties(props);
    }
    
    /**
     * 清空所有欄位並停用
     * 當沒有物件被選取時會呼叫此方法
     */
    public void clearFields() {
        isUpdating = true;
        
        nameField.setText("");
        genderBox.setSelectedIndex(0);
        heightField.setText("");
        weightField.setText("");
        ageField.setText("");
        timeofdeathField.setText("");
        descriptionArea.setText("");
        
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
        genderBox.setEnabled(enabled);
        heightField.setEnabled(enabled);
        weightField.setEnabled(enabled);
        ageField.setEnabled(enabled);
        timeofdeathField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
    }
    
    /**
     * 內部類別：人物屬性資料結構
     */
    public static class PersonProperties implements Serializable {
        private static final long serialVersionUID = 4L;
        public String name;
        public String gender;
        public String height;
        public String weight;
        public String age;
        public String timeofdeath;
        public String description;
    }
}