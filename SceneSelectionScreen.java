// SceneSelectionScreen.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SceneSelectionScreen 類別 - 場景選擇畫面
 * 顯示「室內」和「室外」兩個選項
 */
public class SceneSelectionScreen extends JFrame {
    
    /**
     * 建構子
     */
    public SceneSelectionScreen() {
        // 設定視窗基本屬性
        this.setTitle("請選擇場景");
        this.setSize(500, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); // 視窗置中
        this.setLayout(new BorderLayout());
        
        // --- 建立左側「室內」區域 ---
        JPanel indoorPanel = new JPanel();
        indoorPanel.setLayout(new GridBagLayout());
        indoorPanel.setBackground(new Color(200, 215, 200)); // 淺綠色
        indoorPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel indoorLabel = new JLabel("室內");
        indoorLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
        indoorPanel.add(indoorLabel);
        
        // 加入點擊事件
        indoorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openMainWindow("室內");
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                indoorPanel.setBackground(new Color(180, 205, 180));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                indoorPanel.setBackground(new Color(200, 215, 200));
            }
        });
        
        // --- 建立右側「室外」區域 ---
        JPanel outdoorPanel = new JPanel();
        outdoorPanel.setLayout(new GridBagLayout());
        outdoorPanel.setBackground(new Color(215, 205, 220)); // 淺紫色
        outdoorPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel outdoorLabel = new JLabel("室外");
        outdoorLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
        outdoorPanel.add(outdoorLabel);
        
        // 加入點擊事件
        outdoorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openMainWindow("室外");
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                outdoorPanel.setBackground(new Color(205, 195, 210));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                outdoorPanel.setBackground(new Color(215, 205, 220));
            }
        });
        
        // --- 建立頂部標題區域 ---
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); //置中
        titlePanel.setBackground(new Color(240, 240, 240));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("請選擇場景");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        
        // --- 建立中央容器（左右分割）---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(indoorPanel);
        centerPanel.add(outdoorPanel);
        
        // 加入「Home」按鈕（左上角）
        JButton homeButton = new JButton("回首頁");
        homeButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        homeButton.setFocusPainted(false);
        homeButton.setBorderPainted(true);
        homeButton.setContentAreaFilled(false);
        homeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                backToHome();
            }
        });
        
        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topLeftPanel.setBackground(new Color(240, 240, 240));
        topLeftPanel.add(homeButton);
        
        // 加入一個空的右側面板來平衡佈局
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.setBackground(new Color(240, 240, 240));
        topRightPanel.add(Box.createHorizontalStrut(homeButton.getPreferredSize().width)); // 使用 Home 按鈕的寬度
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(topLeftPanel, BorderLayout.WEST);
        topPanel.add(titlePanel, BorderLayout.CENTER);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        
        // 將元件加入視窗
        this.add(topPanel, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        
        this.setVisible(true);
    }
    
    /**
     * 開啟主視窗並建立畫布
     * @param sceneType 場景類型（室內/室外）
     */
    private void openMainWindow(String sceneType) {
        // 關閉當前視窗
        this.dispose();
        
        // 開啟主視窗
        MainWindow mainWindow = new MainWindow();
        mainWindow.setSceneType(sceneType);
        // 自動建立新畫布
        mainWindow.addNewPage();
        
        // 可以根據場景類型設定不同的背景顏色或初始狀態
        if (sceneType.equals("室內")) {
            // 室內場景的特殊設定（如果需要的話）
            mainWindow.setTitle("CS_Painter - 室內場景");
        } else {
            // 室外場景的特殊設定
            mainWindow.setTitle("CS_Painter - 室外場景");
        }
    }
    
    /**
     * 返回啟動畫面
     */
    private void backToHome() {
        this.dispose();
        new WelcomeScreen();
    }
}