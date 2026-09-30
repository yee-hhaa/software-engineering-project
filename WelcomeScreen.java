import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * WelcomeScreen 類別 - 啟動畫面
 * 顯示「殺畫 CS:GO! Crime Scene Graph it Out!」
 * 提供 NEW 和 LOAD 兩個按鈕
 */
public class WelcomeScreen extends JFrame {
    
    /**
     * 建構子
     */
    public WelcomeScreen() {
        // 設定視窗基本屬性
        this.setTitle("殺畫 - Crime Scene Graph");
        this.setSize(500, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); // 視窗置中
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Color.WHITE);
        
        // --- 建立標題區域 ---
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        
        // 主標題：殺畫
        JLabel mainTitle = new JLabel("殺畫");
        mainTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 48));
        mainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 副標題：CS:GO!
        JLabel subTitle1 = new JLabel("CS:GO!");
        subTitle1.setFont(new Font("Arial", Font.BOLD, 32));
        subTitle1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 英文說明
        JLabel subTitle2 = new JLabel("Crime Scene Graph it Out!");
        subTitle2.setFont(new Font("Arial", Font.ITALIC, 16));
        subTitle2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        titlePanel.add(mainTitle);
        titlePanel.add(Box.createVerticalStrut(10));//建立間隔
        titlePanel.add(subTitle1);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(subTitle2);
        
        // --- 建立按鈕區域 ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 80, 0));
        
        // NEW 按鈕
        JButton newButton = createStyledButton("新增畫布");
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSceneSelection();
            }
        });
        
        // LOAD 按鈕
        JButton loadButton = createStyledButton("載入畫布");
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadExistingFile();
            }
        });
        
        buttonPanel.add(newButton);
        buttonPanel.add(loadButton);
        
        // 將元件加入視窗
        this.add(titlePanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
        
        this.setVisible(true);
    }
    
    /**
     * 建立樣式化按鈕
     * @param text 按鈕文字
     * @return 建立好的按鈕
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 120));
      //  button.setFont(new Font("Arial", Font.BOLD, 18)); //英文字型
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
        button.setBackground(new Color(240, 240, 240));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        
        // 加入滑鼠懸停效果，當滑鼠在按鈕上懸浮時會改變顏色
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(220, 220, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(240, 240, 240));
            }
        });
        
        return button;
    }
    
    /**
     * 開啟場景選擇視窗
     */
    private void openSceneSelection() {
        // 關閉當前視窗
        this.dispose();
        // 開啟場景選擇視窗
        new SceneSelectionScreen();
    }
    
    /**
     * 讀取已存在的檔案
     */
    private void loadExistingFile() {
        // 建立主視窗
        MainWindow mainWindow = new MainWindow();
        
        // 呼叫開啟舊檔功能並檢查結果
        boolean success = mainWindow.load();
        if (success) {
            // ⭐ 成功載入檔案，關閉歡迎畫面
        	mainWindow.resetToolBarState();
            this.dispose();
        } else {
            // ⭐ 使用者取消載入，關閉主視窗，保持歡迎畫面開啟
            mainWindow.dispose();
            // 歡迎畫面保持開啟，不做任何事
            System.out.println("返回歡迎畫面");
        }
    }
}