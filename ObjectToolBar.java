import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;  // ⭐ 新增這行
/**
 * ObjectToolBar 類別 - 左側物件工具列
 * 繼承自 JPanel，實作 ActionListener 來處理按鈕點擊
 * 提供預設物件的快速新增功能
 */
public class ObjectToolBar extends JPanel implements ActionListener {

    // 對主視窗的引用
    private MainWindow mainWindow;
    private String sceneType = "室內"; 
    /**
     * 建構子
     * @param window 對 MainWindow 的引用，以便工具列可以與主視窗通訊
     */
    public ObjectToolBar(MainWindow window, String sceneType) {
        this.mainWindow = window;
        this.sceneType = sceneType;
        
        // 設定此 JPanel 的佈局為垂直堆疊 (BoxLayout)
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // 設定一個合適的偏好寬度
        this.setPreferredSize(new Dimension(150, 0));
        // 設定淺灰色背景
        this.setBackground(new Color(230, 230, 230));

        // --- 建立並加入所有按鈕和標籤 ---

        // 1. 基本物件區
        add(createHeaderLabel("基本物件"));
        if(sceneType.equals("室內")){
        	add(createButtonGrid(new String[]{"椅子", "桌子", "窗戶", "門"}, 2));
        }
        else {
        	add(createButtonGrid(new String[]{"石頭", "車子", "樹", "樹枝"}, 2));
        }
        // 2. 標示物件區
        add(createHeaderLabel("標示"));
        add(createButtonGrid(new String[]{"證物標示", "人"}, 2));  // ⭐ 改為「證物標示」

        // 3. 武器物件區
        add(createHeaderLabel("武器"));
        add(createButtonGrid(new String[]{"刀", "槍"}, 2));

        
        // ⭐ 新增：4. 自訂物件區
        java.util.Map<String, MainWindow.CustomObject> customObjects = getCustomObjects();
        if (customObjects != null && !customObjects.isEmpty()) {
            add(createHeaderLabel("自訂物件"));
            
            // 將所有自訂物件名稱轉換為陣列
            String[] customNames = customObjects.keySet().toArray(new String[0]);
            add(createButtonGrid(customNames, 2));
        }
        
        // 5. 新增按鈕（用於匯入新的自訂物件）
        add(Box.createVerticalStrut(10)); // 加入一點垂直間距
        JButton addButton = new JButton("+");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT); // 置中
        addButton.addActionListener(this);
        this.add(addButton);
    }
    
    public String getSceneType() {
        return sceneType;
    }
    /**
     * ⭐ 新增：從 MainWindow 取得自訂物件列表
     */
    private java.util.Map<String, MainWindow.CustomObject> getCustomObjects() {
        if (mainWindow == null) return null;
        
        // 使用反射或提供 getter 方法來取得自訂物件
        // 這裡我們需要在 MainWindow 中加入 getter 方法
        return mainWindow.getCustomObjects();
    }
    /**
     * 輔助方法：建立一個置中的標題標籤
     * @param text 標籤文字
     * @return 建立好的標籤
     */
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        // 設定上下邊距
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        return label;
    }

    /**
     * 輔助方法：建立一個按鈕網格
     * @param buttons 按鈕文字陣列
     * @param columns 每行的欄數
     * @return 建立好的按鈕網格面板
     */
    private JPanel createButtonGrid(String[] buttons, int columns) {
        // 建立網格佈局：0 代表自動計算行數，5 是水平和垂直間距
        JPanel panel = new JPanel(new GridLayout(0, columns, 5, 5));
        panel.setOpaque(false); // 讓背景透明，顯示父面板的顏色
        
        // 為每個文字建立按鈕並加入面板
        for (String text : buttons) {
            JButton btn = new JButton(text);
            // 檢查是否為 "證物標示"
            if (text.equals("證物標示")) {
                // 如果是，設定按鈕的 "顯示文字" 為 HTML 換行格式
                btn = new JButton("<html>證物<br>標示</html>");
            } else {
                // 其他按鈕照常使用原始文字
                btn = new JButton(text);
            }
            btn.setActionCommand(text);
            btn.addActionListener(this); // 註冊事件監聽器
            panel.add(btn);
        }
        
        // 限制網格面板的最大寬度，使其不會水平撐開
        panel.setMaximumSize(new Dimension(this.getPreferredSize().width, 1000));
        return panel;
    }

    /**
     * 處理所有按鈕的點擊事件
     * @param e 動作事件
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand(); // 取得被點擊按鈕的文字
        if (mainWindow == null) return;

        // 根據按鈕的文字來設定 MainWindow 的繪圖模式
        switch (command) {
            case "椅子":
            case "桌子":
            case "窗戶":
            case "門":
            case "石頭":
            case "車子":
            case "樹":
            case "樹枝":
            case "證物標示":  // ⭐ 改為「證物標示」
            case "人":
            case "刀":
            case "槍":
                // 統一呼叫 setDrawingMode，傳入按鈕的文字作為模式名稱
                mainWindow.setDrawingMode(command);
                break;
            case "+":
            		// ⭐ 修改：「+」按鈕的功能 - 匯入自訂 SVG 物件
            		importCustomSVGWithPanel();
                break;
            default:
                // ⭐ 新增：處理自訂物件的點擊
                // 如果不是上面的預設按鈕，檢查是否為自訂物件
                if (mainWindow.isCustomObject(command)) {
                    mainWindow.setDrawingMode(command);
                } else {
                    System.out.println("未知的命令：" + command);
                }
                break;
        }
    }
    
    /**
     * ⭐ 優化版：使用面板同時輸入寬度和高度
     */
    private void importCustomSVGWithPanel() {
        JFileChooser fileChooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = 
            new javax.swing.filechooser.FileNameExtensionFilter("SVG 檔案 (*.svg)", "svg");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("選擇 SVG 檔案");
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 建立輸入面板
            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JTextField nameField = new JTextField(15);
            JTextField widthField = new JTextField("0.5", 15);
            JTextField heightField = new JTextField("0.5", 15);
            
            inputPanel.add(new JLabel("物件名稱："));
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("寬度（公尺）："));
            inputPanel.add(widthField);
            inputPanel.add(new JLabel("高度（公尺）："));
            inputPanel.add(heightField);
            
            int option = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "新增自訂物件",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (option == JOptionPane.OK_OPTION) {
                String objectName = nameField.getText().trim();
                
                if (objectName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "物件名稱不可為空白！", "錯誤", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    double objectWidth = Double.parseDouble(widthField.getText().trim());
                    double objectHeight = Double.parseDouble(heightField.getText().trim());
                    
                    if (objectWidth <= 0 || objectHeight <= 0) {
                        throw new NumberFormatException();
                    }
                    
                    mainWindow.addCustomObject(objectName, selectedFile.getAbsolutePath(), objectWidth, objectHeight);
                    
                    JOptionPane.showMessageDialog(
                        this,
                        "自訂物件「" + objectName + "」已加入！\n" +
                        "尺寸：" + objectWidth + "m × " + objectHeight + "m",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "請輸入有效的數字！", "錯誤", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}