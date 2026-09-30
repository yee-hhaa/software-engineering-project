import javax.swing.*;
import java.awt.event.*;
import java.awt.Color;

/**
 * ToolBar 類別 - 頂部工具列
 * 繼承自 JMenuBar，包含所有主要功能按鈕
 * 提供繪圖工具、檔案操作等功能的按鈕
 */
public class ToolBar extends JMenuBar implements ActionListener {
    // 對主視窗的引用，用於呼叫主視窗的方法
    private MainWindow mainWindow;
    private JButton addObjectButton;
    private boolean isObjectToolBarVisible = false;
    /**
     * ToolBar 建構子
     * @param window 對 MainWindow 的引用
     */
    ToolBar(MainWindow window) {
        this.mainWindow = window;
        
        // 建立 File 選單
        JMenu fileMenu = new JMenu("檔案");
  
        // 建立所有按鈕
        addObjectButton = new JButton("顯示選單"); // 用於切換物件工具列
        JButton Pencil = new JButton("鉛筆");              // 鉛筆工具
        JButton Line = new JButton("直線");                // 直線工具
        JButton Arrow = new JButton("箭頭");               // 箭頭工具
        JButton Move = new JButton("移動");                // 移動/選取工具
        JButton Color = new JButton("顏色");               // 顏色選擇器
        JButton Clear = new JButton("清除畫布");           // 清除畫布
        
        // 建立 File 選單的子項目
        JMenuItem New = new JMenuItem("新增畫布");         // 新增畫布
        JMenuItem openFile = new JMenuItem("開啟舊檔");    // ⭐ 新增：開啟舊檔
        JMenuItem saveFile = new JMenuItem("儲存");        // 儲存檔案
        JMenuItem saveNew = new JMenuItem("另存新檔");     // 另存新檔
        
        // 將子項目加入 File 選單
        fileMenu.add(New);
        fileMenu.add(openFile);       // ⭐ 新增
        fileMenu.addSeparator();      // ⭐ 新增分隔線，讓選單更清楚
        fileMenu.add(saveFile);
        fileMenu.add(saveNew);

        // 註冊事件監聽器 - 當按鈕被點擊時會呼叫 actionPerformed 方法
        New.addActionListener(this);
        openFile.addActionListener(this); 
        saveFile.addActionListener(this);
        saveNew.addActionListener(this);
        addObjectButton.addActionListener(this);
        Pencil.addActionListener(this);
        Line.addActionListener(this);
        Arrow.addActionListener(this);
        Move.addActionListener(this);
        Color.addActionListener(this);
        Clear.addActionListener(this);
        
        // 將所有元件加入工具列（從左到右的順序）
        this.add(fileMenu);
        this.add(addObjectButton);
        this.add(Pencil);
        this.add(Line);
        this.add(Arrow);
        this.add(Move);
        this.add(Color);
        this.add(Clear);
        
        // 美化按鈕外觀，讓它們看起來和 File 選單一致
        JButton[] buttonsToFlatten = { addObjectButton, Pencil, Line, Arrow, Move, Color, Clear };
        for (JButton btn : buttonsToFlatten) {
            btn.setBorderPainted(false);      // 移除邊框
            btn.setContentAreaFilled(false);  // 移除背景填充
        }
    }
    
    /**
     * ⭐ 新增:切換按鈕文字
     * 在物件工具列顯示/隱藏時自動更新按鈕文字
     */
    private void updateButtonText() {
        if (isObjectToolBarVisible) {
            addObjectButton.setText("隱藏選單");
        } else {
            addObjectButton.setText("顯示選單");
        }
    }
    
    /**
     * ⭐ 新增:處理物件工具列的切換
     * 完全在 ToolBar 內部管理,不需要外部干預
     */
    private void handleToggleObjectToolBar() {
        // 呼叫 MainWindow 的切換方法
        mainWindow.toggleObjectToolBar();
        
        // 切換狀態
        isObjectToolBarVisible = !isObjectToolBarVisible;
        
        // 更新按鈕文字
        updateButtonText();
    }
    
    /**
     * ⭐ 新增:重置按鈕狀態
     * 當需要手動重置時使用(例如:載入新檔案時)
     */
    public void resetObjectToolBarState() {
        isObjectToolBarVisible = false;
        updateButtonText();
    }
    /**
     * 處理所有按鈕的點擊事件
     * 根據不同的按鈕命令執行對應的動作
     * @param e 動作事件
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand(); // 取得被點擊按鈕的文字

        // 確保 mainWindow 不是 null
        if (mainWindow == null) return;

        // 根據不同的命令執行對應的動作
        switch (command) {
            case "新增畫布":
                // 新增一個空白畫布
                mainWindow.addNewPage();
                break;
            case "開啟舊檔":  // ⭐ 新增：開啟舊檔功能
                // 開啟檔案選擇對話框，讀取之前的存檔
                mainWindow.load();
                break;
            case "儲存":
                // 儲存檔案到目前的路徑
                mainWindow.save();
                break;
            case "另存新檔":
                // 另存新檔到新的路徑
                mainWindow.saveAsNew();
                break;
            case "顯示選單":
            case "隱藏選單":
            	handleToggleObjectToolBar();
                break;
            case "鉛筆":
                // 切換到鉛筆繪圖模式
                mainWindow.setDrawingMode("鉛筆");
                break;
            case "直線":
                // 切換到直線繪圖模式
                mainWindow.setDrawingMode("直線");
                break;
            case "箭頭":
                // 切換到箭頭繪圖模式
                mainWindow.setDrawingMode("箭頭");
                break;
            case "移動":
                // 切換到移動/選取模式
                mainWindow.setDrawingMode("移動");
                break;
            case "顏色":
                // 彈出顏色選擇對話框
                Color chosenColor = JColorChooser.showDialog(
                    mainWindow,
                    "選擇顏色",
                    Color.BLACK
                );
                
                // 如果使用者選擇了顏色（沒有按取消）
                if (chosenColor != null) {
                    mainWindow.setDrawingColor(chosenColor);
                }
                break;
            case "清除畫布":
                // 清除畫布上的所有內容
                mainWindow.clearCanvas();
                break;
        }
    }
}