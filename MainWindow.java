import javax.swing.*;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;

/**
 * MainWindow 類別 - 主視窗
 * 繼承自 JFrame,管理整個應用程式的 UI 佈局和功能
 * 負責協調各個元件之間的互動
 */
public class MainWindow extends JFrame {
    // 成員變數
    private Page currentPage;               // 目前的畫布
    JFileChooser filechooser = new JFileChooser(); // 檔案選擇器
    private File currentFilePath = null;    // 追蹤目前檔案路徑
    private ObjectToolBar objectToolBar;    // 物件工具列(左側)
    private String sceneType = "室內";
    private PropertyPanel propertyPanel;        // 一般物件屬性面板
    private PersonPropertyPanel personPanel;    // ⭐ 新增
    private EvidencePropertyPanel evidencePanel;   // ⭐ 新增:證物標示屬性面板
    //用於固定畫布大小的容器面板
    private JPanel leftPlaceholder;         // 左側佔位面板
    private JPanel rightPlaceholder;        // 右側佔位面板
    private JPanel centerContainer;         // 中央容器(包含畫布)
    // 追蹤是否成功載入檔案
    private boolean fileLoaded = false;
    private ToolBar toolBar;
    //儲存自訂物件的資料結構
    private java.util.Map<String, CustomObject> customObjects = new java.util.HashMap<>();
    
    //自訂物件的資料結構
    public static class CustomObject {
        String filePath;  // SVG 檔案路徑
        double width;     // 物件寬度(公尺)
        double height;    // 物件高度(公尺)
        
        CustomObject(String filePath, double width, double height) {
            this.filePath = filePath;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * MainWindow 建構子
     * 初始化視窗的所有元件和設定
     */
    MainWindow() {
    	
        // 設定視窗基本屬性
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700); // 加大視窗以容納左右面板
        this.setTitle("CS_Painter");
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Color.GRAY);
        this.setLocationRelativeTo(null);//置中
        // 建立並設定頂部工具列
        toolBar = new ToolBar(this);
        this.setJMenuBar(toolBar);
        
        
        
        // 實體化物件工具列(左側)
    //    objectToolBar = new ObjectToolBar(this);
       
        
        // 實體化屬性面板(右側)
        propertyPanel = new PropertyPanel(this);
        propertyPanel.setPreferredSize(new Dimension(200, 0));
        
        personPanel = new PersonPropertyPanel(this);
        personPanel.setPreferredSize(new Dimension(200, 0));
        
        evidencePanel = new EvidencePropertyPanel(this);
        evidencePanel.setPreferredSize(new Dimension(200, 0));
        // 建立左側佔位面板(預留物件工具列的空間)
        leftPlaceholder = new JPanel();
        leftPlaceholder.setPreferredSize(new Dimension(150, 0));
        leftPlaceholder.setBackground(Color.GRAY);
        
        // 建立右側佔位面板(預留屬性面板的空間)
        rightPlaceholder = new JPanel();
        rightPlaceholder.setPreferredSize(new Dimension(200, 0));
        rightPlaceholder.setBackground(Color.GRAY);
        
        // 建立中央容器(使用 BorderLayout 來放置畫布)
        centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(Color.GRAY);
        
        // 建立底部面板(放置重選按鈕)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        bottomPanel.setBackground(Color.GRAY);
        
        // 建立重選按鈕
        JButton resetButton = new JButton("重選場景");
        resetButton.setFont(new java.awt.Font("Microsoft YaHei", java.awt.Font.BOLD, 14));
        resetButton.setPreferredSize(new Dimension(120, 35));
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> handleResetButton());
        bottomPanel.add(resetButton);
        
        // 將左右佔位面板和中央容器加入主視窗
        this.add(leftPlaceholder, BorderLayout.WEST);
        this.add(centerContainer, BorderLayout.CENTER);
        this.add(rightPlaceholder, BorderLayout.EAST);
        this.add(bottomPanel, BorderLayout.SOUTH);  // ⭐ 新增底部面板
        
        // 設定檔案選擇器的過濾器,只顯示 .csp 檔案
        filechooser.setFileFilter(new FileNameExtensionFilter("CS Painter Files (*.csp)", "csp"));
        
        this.setVisible(true);
    }
    /**
     * ⭐ 新增:設定場景類型並建立對應的工具列
     * @param sceneType 場景類型("室內" 或 "室外")
     */
    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
        
        // 建立對應場景的物件工具列
        objectToolBar = new ObjectToolBar(this, sceneType);
        objectToolBar.setPreferredSize(new Dimension(150, 0));
    }
    
    /**
     * ⭐ 新增:取得場景類型
     */
    public String getSceneType() {
        return sceneType;
    }
    /**
     * ⭐ 新增:處理重選按鈕點擊事件
     */
    private void handleResetButton() {
        // 詢問使用者是否要儲存檔案
        int response = JOptionPane.showConfirmDialog(
            this,
            "是否要儲存目前的檔案?",
            "儲存確認",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (response == JOptionPane.CANCEL_OPTION) {
            // 使用者取消,不做任何事
            return;
        }
        
        if (response == JOptionPane.YES_OPTION) {
            // 使用者選擇儲存
            save();
        }
        //選擇否則繼續以下動作
        // 關閉當前視窗
        this.dispose();
        
        // 開啟場景選擇畫面
        new SceneSelectionScreen();
    }
    
    /**
     * ⭐ 新增:取得所有自訂物件
     * @return 自訂物件的 Map
     */
    public java.util.Map<String, CustomObject> getCustomObjects() {
        return customObjects;
    }
    /**
     * 切換物件工具列的顯示/隱藏
     * 使用者點擊「新增物件」按鈕時會呼叫此方法
     */
    public void toggleObjectToolBar() {
        if (objectToolBar.getParent() != null) {
            // 如果正在顯示,就移除它,顯示佔位面板
            leftPlaceholder.removeAll();
            leftPlaceholder.setBackground(Color.GRAY);
        } else {
            // 如果是隱藏的,就把它加到左側佔位面板中
            leftPlaceholder.setLayout(new BorderLayout());
            leftPlaceholder.add(objectToolBar, BorderLayout.CENTER);
        }
        
        // 刷新視窗佈局
        leftPlaceholder.revalidate();
        leftPlaceholder.repaint();
    }
    
    public void resetToolBarState() {
    	if (toolBar !=null) {
    		toolBar.resetObjectToolBarState();
    	}
    }
    /**
     * 顯示屬性面板
     * 當使用者選取物件時會自動呼叫
     */
    public void showPropertyPanel() {
        // 檢查所有面板，如果有啟用則刪除
        if (personPanel.getParent() != null || evidencePanel.getParent() != null) {
            rightPlaceholder.removeAll();
        }
        
        if (propertyPanel.getParent() == null) {
            rightPlaceholder.setLayout(new BorderLayout());
            rightPlaceholder.add(propertyPanel, BorderLayout.CENTER);
            rightPlaceholder.revalidate();
            rightPlaceholder.repaint();
        }
    }
    
    public void showPersonPanel() {
        // ⭐ 修改:檢查所有面板
        if (propertyPanel.getParent() != null || evidencePanel.getParent() != null) {
            rightPlaceholder.removeAll();
        }
        
        if (personPanel.getParent() == null) {
            rightPlaceholder.setLayout(new BorderLayout());
            rightPlaceholder.add(personPanel, BorderLayout.CENTER);
            rightPlaceholder.revalidate();
            rightPlaceholder.repaint();
        }
    }
    public void showEvidencePanel() {
        if (propertyPanel.getParent() != null || personPanel.getParent() != null) {
            rightPlaceholder.removeAll();
        }
        
        if (evidencePanel.getParent() == null) {
            rightPlaceholder.setLayout(new BorderLayout());
            rightPlaceholder.add(evidencePanel, BorderLayout.CENTER);
            rightPlaceholder.revalidate();
            rightPlaceholder.repaint();
        }
    }
    /**
     * 隱藏屬性面板
     * 當使用者取消選取物件時會自動呼叫
     */
    public void hidePropertyPanel() {
        // ⭐ 修改:檢查所有三種面板
        if (propertyPanel.getParent() != null || 
            personPanel.getParent() != null || 
            evidencePanel.getParent() != null) {
            rightPlaceholder.removeAll();
            rightPlaceholder.setBackground(Color.GRAY);
            rightPlaceholder.revalidate();
            rightPlaceholder.repaint();
        }
    }
    
    /**
     * 更新屬性面板的內容
     * @param properties 物件的屬性資料
     */
    //呼叫propertyPanel.java裡的updateProperties
    public void updatePropertyPanel(PropertyPanel.ShapeProperties properties) {
        propertyPanel.updateProperties(properties);
    }
    
    public void updatePersonPanel(PersonPropertyPanel.PersonProperties properties) {
        personPanel.updateProperties(properties);
    }
    
    /**
     * ⭐ 新增:更新證物標示屬性面板的內容
     */
    //呼叫evidencePanel.java裡的updateProperties
    public void updateEvidencePanel(EvidencePropertyPanel.EvidenceProperties properties) {
        evidencePanel.updateProperties(properties);
    }
    /**
     * 新增一個新的畫布
     * 使用者點擊 File -> New 時會呼叫此方法
     */
    public void addNewPage() {
        // 如果已經有畫布,先移除舊的
        if (currentPage != null) {
            centerContainer.remove(currentPage);
        }
        
        // 建立新畫布,並傳入 MainWindow 的引用
        currentPage = new Page(this);
        currentPage.setPixelsPerMeter(100.0);//設定比例尺大小50像素1公尺
        propertyPanel.setCurrentPage(currentPage); // 設定屬性面板的當前畫布
        personPanel.setCurrentPage(currentPage);
        evidencePanel.setCurrentPage(currentPage);  // ⭐ 新增這行
        
        // 將畫布加入中央容器
        centerContainer.add(currentPage, BorderLayout.CENTER);

        centerContainer.revalidate();
        centerContainer.repaint();
    }
    
    /**
     * 設定繪圖模式
     * @param mode 模式名稱(例如:Pencil, Line, Move, 椅子, 桌子等)
     */
    //呼叫Page.java的setMode
    public void setDrawingMode(String mode) {
        if (currentPage != null) {
            currentPage.setMode(mode);
        } else {
            System.out.println("請先新增畫布 (New)!");
        }
    }
    
    /**
     * 設定繪圖顏色
     * @param color 顏色
     */
    //呼叫Page.java的setColor
    public void setDrawingColor(Color color) {
        if (currentPage != null) {
            currentPage.setColor(color);
        } else {
            System.out.println("請先新增畫布 (New)!");
        }
    }
    
    /**
     * 清除畫布上的所有內容
     */
    //呼叫Page.java的clear
    public void clearCanvas() {
        if (currentPage != null) {
            currentPage.clear();
        } else {
            System.out.println("沒有畫布可以清除!");
        }
    }
    
    /**
     * 儲存檔案
     * 如果之前沒有儲存過,會呼叫另存新檔
     */
    public void save() {
        if (currentFilePath == null) {
            saveAsNew(); // 如果沒有儲存過,就呼叫 "另存新檔"
        } else {
            performSave(currentFilePath); // 否則,直接儲存到目前的路徑
        }
    }
    
    /**
     * 另存新檔
     * 彈出檔案選擇對話框,讓使用者選擇儲存位置
     */
    public void saveAsNew() {
        // 顯示 "儲存檔案" 對話框
        int userSelection = filechooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = filechooser.getSelectedFile();
            
            // 確保副檔名是 .csp
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".csp")) {
                fileToSave = new File(filePath + ".csp");
            }
            
            performSave(fileToSave);
        } else {
            System.out.println("使用者取消儲存檔案。");
        }
    }
    
    /**
     * 核心儲存邏輯:將 Page 內容序列化到指定的 File
     * @param file 要儲存的檔案
     */
    private void performSave(File file) {
        if (currentPage == null) {
            JOptionPane.showMessageDialog(this, "沒有畫布內容可以儲存!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            // 呼叫 Page 的 getDrawingData() 來獲取要儲存的資料
            Object data = currentPage.getDrawingData();
            oos.writeObject(data);
            
            // 更新狀態
            currentFilePath = file;
            this.setTitle("CS_Painter - " + file.getName()); // 更新視窗標題
            JOptionPane.showMessageDialog(this, "檔案儲存成功!", "儲存", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "儲存檔案失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 讀取檔案
     * 彈出檔案選擇對話框,讓使用者選擇要開啟的檔案
     */
    public boolean load() {
        // 顯示 "開啟檔案" 對話框
        int userSelection = filechooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = filechooser.getSelectedFile();
            performLoad(fileToLoad);
            if (objectToolBar == null) {
            	setSceneType("室內");
            }
            fileLoaded = true;
            return true;
        } else {
            // 使用者取消選擇檔案
            System.out.println("使用者取消讀取檔案。");
            fileLoaded = false;
            return false;
        }
    }
    
    /**
     * ⭐ 新增:檢查是否已載入檔案
     */
    public boolean isFileLoaded() {
        return fileLoaded;
    }
    /**
     * 核心讀取邏輯:從指定的 File 讀取並更新 Page 內容
     * @param file 要讀取的檔案
     */
    private void performLoad(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // 讀取序列化的繪圖資料
            Object data = ois.readObject();
            
            // 確保有畫布存在
            if (currentPage == null) {
                addNewPage(); // 如果沒有畫布,則先新增一個
            }
            
            // 載入繪圖資料到 Page
            currentPage.loadDrawingData(data);
            
            // 更新狀態
            currentFilePath = file;
            this.setTitle("CS_Painter - " + file.getName()); // 更新視窗標題
            
            JOptionPane.showMessageDialog(this, "檔案讀取成功!", "讀取", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "讀取檔案失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * ⭐ 修改:加入自訂物件(支援長寬)
     * @param name 物件名稱
     * @param svgFilePath SVG 檔案路徑
     * @param width 物件寬度(公尺)
     * @param height 物件高度(公尺)
     */
    public void addCustomObject(String name, String svgFilePath, double width, double height) {
        // 儲存自訂物件資料
        customObjects.put(name, new CustomObject(svgFilePath, width, height));
        
        // 重新建立物件工具列以顯示新物件
        refreshObjectToolBar();
        
        System.out.println("已加入自訂物件:" + name + " (" + width + "m × " + height + "m)");
    }

    /**
     * ⭐ 新增:取得自訂物件
     * @param name 物件名稱
     * @return CustomObject 或 null
     */
    public CustomObject getCustomObject(String name) {
        return customObjects.get(name);
    }

    /**
     * ⭐ 新增:檢查是否為自訂物件
     * @param name 物件名稱
     * @return 是否為自訂物件
     */
    public boolean isCustomObject(String name) {
        return customObjects.containsKey(name);
    }

    /**
     * ⭐ 新增:重新建立物件工具列
     */
    private void refreshObjectToolBar() {
        // 移除舊的工具列
        leftPlaceholder.removeAll();
        
        // 建立新的物件工具列(包含自訂物件)
        objectToolBar = new ObjectToolBar(this, sceneType);
        objectToolBar.setPreferredSize(new Dimension(150, 0));
        
        // 顯示新的工具列
        leftPlaceholder.setLayout(new BorderLayout());
        leftPlaceholder.add(objectToolBar, BorderLayout.CENTER);
        
        // 刷新介面
        leftPlaceholder.revalidate();
        leftPlaceholder.repaint();
    }
}