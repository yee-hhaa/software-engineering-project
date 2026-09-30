// 匯入所有需要的 Java AWT 和 Swing 套件
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JOptionPane;
import java.awt.geom.RoundRectangle2D;

/**
 * Page 類別 - 畫布
 * 繼承自 JPanel，實作 Serializable 介面以支援檔案儲存
 * 這是整個應用程式的核心，處理所有繪圖和物件操作邏輯
 */
public class Page extends JPanel implements Serializable {

    // 序列化版本 ID，用於版本相容性
    private static final long serialVersionUID = 1L;
    private ShapeWithColor hoveredShape = null;  // 目前滑鼠懸停的物件
    private Point tooltipPosition = null;        // 提示框顯示位置
    // 在 stampObject 中自動遞增編號 
    private static int evidenceCounter = 1; 
    
    // 在成員變數區域加入
    private double pixelsPerMeter = 100.0;  // 100 像素 = 1 公尺
    private double zoomLevel = 1.0;          // 縮放等級

    
    /**
     * 內部類別 ShapeWithColor
     * 將圖形(Shape)和它的顏色(Color)綁定在一起
     * 也必須實作 Serializable 才能被儲存
     */
    private class ShapeWithColor implements Serializable {
        private static final long serialVersionUID = 2L;
        Shape shape;
        Color color;
        PropertyPanel.ShapeProperties properties;
        PersonPropertyPanel.PersonProperties personProperties;
        EvidencePropertyPanel.EvidenceProperties evidenceProperties;  // ⭐ 新增
        boolean isPerson;
        boolean isEvidence;  // ⭐ 新增：標記是否為證物標示
        
        
     // ⭐ 新增：人物部件系統
        boolean isPersonPart;           // 是否為人物部件
        String partType;                // 部件類型："leftArm", "rightArm", "leftLeg", "rightLeg", "torso"
        ShapeWithColor parentPerson;    // 父物件（軀幹）
        List<ShapeWithColor> bodyParts; // 子部件列表（只有軀幹有）
        
        ShapeWithColor(Shape shape, Color color) {
            this(shape, color, false, false);  // 預設都不是
        }
        
     // ⭐ 新增：支援指定類型的建構子
        ShapeWithColor(Shape shape, Color color, boolean isPerson, boolean isEvidence) {
            this.shape = shape;
            this.color = color;
            this.isPerson = isPerson;
            this.isEvidence = isEvidence;
            this.isPersonPart = false;          // ⭐ 新增初始化
            this.partType = null;               // ⭐ 新增初始化
            this.parentPerson = null;           // ⭐ 新增初始化
            this.bodyParts = new ArrayList<>(); // ⭐ 新增初始化
            
            if (isPerson) {
                // 初始化人物屬性
                this.personProperties = new PersonPropertyPanel.PersonProperties();
                this.personProperties.name = "鄭子儀";
                this.personProperties.gender = "男";
                this.personProperties.height = "174cm";
                this.personProperties.weight = "54kg";
                this.personProperties.age = "23";
                this.personProperties.description = "阿北團認可的男人";
            } else if (isEvidence) {
                // 初始化證物標示屬性
                this.evidenceProperties = new EvidencePropertyPanel.EvidenceProperties();
                this.evidenceProperties.number = "1";
                this.evidenceProperties.description = "";
            } else {
                // 初始化一般物件屬性
                this.properties = new PropertyPanel.ShapeProperties();
                this.properties.name = currentMode;
                this.properties.length = "0.8m";
                this.properties.width = "1.2m";
                this.properties.height = "0.6m";
                this.properties.description = "";
                this.properties.color = color;
            }
        }
     // ⭐ 新增：建立人物部件的建構子
        ShapeWithColor(Shape shape, Color color, String partType, ShapeWithColor parent) {
            this.shape = shape;
            this.color = color;
            this.isPerson = false;
            this.isEvidence = false;
            this.isPersonPart = true;
            this.partType = partType;
            this.parentPerson = parent;
            this.bodyParts = new ArrayList<>();
        }
    }

    // --- 核心資料與狀態變數 ---

    // 儲存畫布上所有已經畫好的圖形
    private List<ShapeWithColor> shapes = new ArrayList<>();

    // 記錄當前的操作模式 (例如 "Pencil", "Line", "Move")
    private String currentMode = "鉛筆";
    // 記錄當前的繪圖顏色
    private Color currentColor = Color.BLACK;
    // 繪製過程中，用來即時預覽的圖形
    private Shape currentShape;
    // 繪製時的起始點
    private Point startPoint;
    // 標記目前是否正在繪製中 (主要用於直線等需要兩次點擊的圖形)
    private boolean isDrawing = false;

    // --- 物件選取、移動、縮放、旋轉功能的相關變數 ---

    /**
     * 定義操作狀態的列舉
     * 讓邏輯更清晰易懂
     */
    private enum ActionState { 
        NONE,      // 無任何操作
        MOVING,    // 移動物件
        SCALING,   // 縮放物件
        ROTATING   // 旋轉物件
    }
    private ActionState currentAction = ActionState.NONE; // 預設為無任何操作

    // 儲存當前被選取的圖形物件
    private ShapeWithColor selectedShape = null;

    // 當開始進行縮放或旋轉時，暫存的狀態資訊
    private Shape originalShapeOnActionStart = null; // 操作開始前，圖形的原始樣貌
    private Point actionStartPoint = null;           // 操作開始時，滑鼠點擊的位置
    private Point2D shapeCenterOnActionStart = null; // 操作開始前，圖形的中心點
    private Point lastActionPoint = null;            // 上一次滑鼠拖曳事件的位置

    // --- 控制點相關變數 ---
    private static final int HANDLE_SIZE = 8;              // 控制點的像素大小
    private static final int ROTATION_HANDLE_INDEX = 4;    // 旋轉控制點的索引值
    private List<Rectangle2D> handles = new ArrayList<>(); // 儲存四個角落的「縮放」控制點
    private Shape rotationHandle = null;                   // 獨立儲存「旋轉」控制點
    private Line2D rotationHandleStick = null;             // 連接旋轉點與圖形邊框的指示線
    private int activeHandleIndex = -1;                    // 記錄目前滑鼠正在操作的是哪個控制點

    // transient 表示這些變數不需要被序列化（儲存到檔案中）
    private transient Cursor[] handleCursors;   // 儲存四個角落控制點對應的滑鼠樣式
    private transient Cursor rotationCursor;    // 儲存旋轉控制點的滑鼠樣式
    private transient MainWindow mainWindow;    // 儲存對 MainWindow 的引用
    
    // ⭐ 新增：儲存旋轉時部件的原始形狀
    private List<Shape> originalBodyParts = null;
    /**
     * Page 的建構子
     * @param window 對 MainWindow 的引用
     */
    public Page(MainWindow window) {
        this.mainWindow = window;
        this.setBackground(Color.WHITE); // 設定畫布背景為白色
        
        // 建立滑鼠事件處理器
        MouseHandler mouseHandler = new MouseHandler();
        this.addMouseListener(mouseHandler);       // 註冊滑鼠點擊、釋放等事件
        this.addMouseMotionListener(mouseHandler); // 註冊滑鼠移動、拖曳等事件
        
        
        // ⭐ 新增：建立鍵盤事件處理器
        KeyHandler keyHandler = new KeyHandler();
        this.addKeyListener(keyHandler);
        this.setFocusable(true); // 讓畫布可以接收鍵盤事件
        this.requestFocusInWindow(); // 請求焦點
        
        // 初始化四個角落縮放點的滑鼠游標樣式
        handleCursors = new Cursor[]{
            new Cursor(Cursor.NW_RESIZE_CURSOR), // 左上
            new Cursor(Cursor.NE_RESIZE_CURSOR), // 右上
            new Cursor(Cursor.SE_RESIZE_CURSOR), // 右下
            new Cursor(Cursor.SW_RESIZE_CURSOR)  // 左下
        };
        // 初始化旋轉點的滑鼠游標樣式
        rotationCursor = new Cursor(Cursor.HAND_CURSOR);
    }

    /**
     * 由 MainWindow 呼叫，用來設定目前的工具模式
     * @param mode 模式名稱
     */
    public void setMode(String mode) {
        this.currentMode = mode;
        // 切換模式時，重置所有狀態，避免前一個模式的狀態影響現在
        isDrawing = false;
        currentShape = null;
        startPoint = null;
        deselectAll(); // 取消所有物件的選取
    }

    /**
     * 由 MainWindow 呼叫，用來設定目前的繪圖顏色
     * @param color 顏色
     */
    public void setColor(Color color) {
        this.currentColor = color;
    }
    /**
     * paintComponent 方法
     * 這是 Swing 中最重要的繪圖方法，所有畫布上的內容都在這裡被畫出來
     * repaint() 方法被呼叫時，Swing 會自動觸發此方法
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 必須先呼叫父類別的方法來處理基本繪製
        Graphics2D g2d = (Graphics2D) g; // 轉換成功能更強的 Graphics2D
        
        // 開啟抗鋸齒，讓線條更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 迴圈遍歷所有已儲存的圖形，並一一畫出來
        for (ShapeWithColor swc : shapes) {
            g2d.setColor(swc.color);             // 設定成該圖形自己的顏色
            g2d.setStroke(new BasicStroke(2));   // 設定線條粗細為 2 像素
            g2d.draw(swc.shape);                 // 繪製圖形
            
            // ⭐ 新增：如果是人物部件，繪製部件標籤
            if (swc.isPersonPart && swc.partType != null) {
                drawPartLabel(g2d, swc);
            }
         // ⭐ 如果是證物標示，在中間繪製編號
            if (swc.isEvidence && swc.evidenceProperties != null) {
                drawEvidenceNumber(g2d, swc);
            }
        }

        // 如果正在繪製中，畫出即時預覽的圖形
        if (currentShape != null) {
            g2d.setColor(this.currentColor);
            g2d.draw(currentShape);
        }

        // 如果有物件被選取，就畫出指示框和控制點
        if (selectedShape != null) {
            Rectangle2D bounds = selectedShape.shape.getBounds2D(); // 取得物件的邊界框
            g2d.setColor(Color.CYAN); // 設定指示框顏色
            
            // 設定虛線樣式
            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                                           0, new float[]{9}, 0);
            g2d.setStroke(dashed);
            g2d.draw(bounds); // 畫出虛線指示框

            // 畫出四個角落的黑色方形（縮放控制點）
            g2d.setColor(Color.BLACK);
            for (Rectangle2D handle : handles) {
                g2d.fill(handle);
            }

            // 畫出頂部的旋轉控制點和其連接線
            if (rotationHandle != null && rotationHandleStick != null) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(new BasicStroke(1));
                g2d.draw(rotationHandleStick); // 畫連接線
                g2d.setColor(Color.BLACK);
                g2d.fill(rotationHandle);      // 畫旋轉點
            }
        }
        // 繪製滑鼠懸停提示框
        if (hoveredShape != null && tooltipPosition != null) {
            drawTooltip(g2d, hoveredShape, tooltipPosition);
        }
        // 繪製比例尺
        drawScaleRuler(g2d);

        // 繪製指北針
        drawCompass(g2d);
    }
    /**
     * ⭐ 新增：繪製人物部件標籤
     * @param g2d Graphics2D 物件
     * @param part 人物部件
     */
    private void drawPartLabel(Graphics2D g2d, ShapeWithColor part) {
        Rectangle2D bounds = part.shape.getBounds2D();
        
        // 根據部件類型顯示不同標籤
        String label = "";
        switch (part.partType) {
            case "leftArm":
                label = "L";
                break;
            case "rightArm":
                label = "R";
                break;
            case "leftLeg":
                label = "L";
                break;
            case "rightLeg":
                label = "R";
                break;
            default:
                return;
        }
        
        // 設定字體
        Font font = new Font("Arial", Font.BOLD, 10);
        g2d.setFont(font);
        
        // 計算文字位置（置中）
        FontMetrics fm = g2d.getFontMetrics(font);
        int textWidth = fm.stringWidth(label);
        int textX = (int)(bounds.getCenterX() - textWidth / 2);
        int textY = (int)(bounds.getCenterY() + fm.getAscent() / 2);
        
        // 繪製背景圓圈
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(textX - 3, textY - fm.getAscent(), textWidth + 6, fm.getHeight());
        
        // 繪製文字
        g2d.setColor(Color.BLUE);
        g2d.drawString(label, textX, textY);
    }
    /**
     * ⭐ 新增：在證物標示中間繪製編號
     * @param g2d Graphics2D 物件
     * @param evidence 證物標示物件
     */
    private void drawEvidenceNumber(Graphics2D g2d, ShapeWithColor evidence) {
        Rectangle2D bounds = evidence.shape.getBounds2D();
        String number = evidence.evidenceProperties.number;
        
        if (number == null || number.isEmpty()) {
            number = "?";
        }
        
        // 設定字體
        Font font = new Font("Arial", Font.BOLD, 18);
        g2d.setFont(font);
        
        // 計算文字位置（置中）
        FontMetrics fm = g2d.getFontMetrics(font);
        int textWidth = fm.stringWidth(number);
        int textHeight = fm.getHeight();
        
        int textX = (int)(bounds.getCenterX() - textWidth / 2);
        int textY = (int)(bounds.getCenterY() + fm.getAscent() / 2);
        
        // 繪製文字
        g2d.setColor(evidence.color);
        g2d.drawString(number, textX, textY);
    }
    
    /**
    * ⭐ 新增：繪製比例尺
    * @param g2d Graphics2D 物件
    */
    private void drawScaleRuler(Graphics2D g2d) {
       // 比例尺位置（左下角，距離邊緣 20 像素）
       int margin = 20;
       int rulerX = margin;
       int rulerY = getHeight() - margin - 40; // 40 是比例尺的高度
       
       // 計算比例尺長度（顯示 1 公尺）
       double oneMeterInPixels = pixelsPerMeter * zoomLevel;
       
       // 如果太長，改顯示 0.5 公尺或 50cm
       double displayMeters = 1.0;
       String unit = "100cm";
       
       if (oneMeterInPixels > 200) {
           displayMeters = 0.5;
           unit = "50cm";
           oneMeterInPixels = oneMeterInPixels * 0.5;
       } else if (oneMeterInPixels < 50) {
           displayMeters = 2.0;
           unit = "200cm";
           oneMeterInPixels = oneMeterInPixels * 2.0;
       }
       
       int rulerLength = (int) oneMeterInPixels;
       
       // 繪製比例尺底線
       g2d.setColor(Color.BLACK);
       g2d.setStroke(new BasicStroke(3));
       g2d.drawLine(rulerX, rulerY, rulerX + rulerLength, rulerY);
       
       // 繪製左右兩端的刻度線
       g2d.drawLine(rulerX, rulerY - 10, rulerX, rulerY + 10);
       g2d.drawLine(rulerX + rulerLength, rulerY - 10, rulerX + rulerLength, rulerY + 10);
       
       // 繪製中間刻度（50cm 位置）
       int halfLength = rulerLength / 2;
       g2d.setStroke(new BasicStroke(2));
       g2d.drawLine(rulerX + halfLength, rulerY - 5, rulerX + halfLength, rulerY + 5);
       
       // 繪製刻度標籤
       g2d.setFont(new Font("Arial", Font.PLAIN, 11));
       FontMetrics fm = g2d.getFontMetrics();
       
       // 左端標籤 "0"
       String leftLabel = "0";
       g2d.drawString(leftLabel, rulerX - fm.stringWidth(leftLabel) / 2, rulerY + 20);
       
       // 中間標籤 "50cm"
       String midLabel = (int)(displayMeters * 50) + "cm";
       g2d.drawString(midLabel, rulerX + halfLength - fm.stringWidth(midLabel) / 2, rulerY + 20);
       
       // 右端標籤 "100cm"
       g2d.drawString(unit, rulerX + rulerLength - fm.stringWidth(unit) / 2, rulerY + 20);
   }
    /**
     * ⭐ 新增：繪製指北針
     * @param g2d Graphics2D 物件
     */
    private void drawCompass(Graphics2D g2d) {
        // 指北針位置（右上角）
        int compassX = getWidth() - 50;
        int compassY = 50;
        int compassSize = 30;
        
        // 儲存原本的轉換
        AffineTransform oldTransform = g2d.getTransform();
        
        // 繪製圓形背景
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(compassX - compassSize/2, compassY - compassSize/2, compassSize, compassSize);
        
        // 繪製外框
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(compassX - compassSize/2, compassY - compassSize/2, compassSize, compassSize);
        
        // 繪製指北箭頭
        int arrowLength = compassSize / 2 - 5;
        int[] xPoints = {compassX, compassX - 5, compassX + 5};
        int[] yPoints = {compassY - arrowLength, compassY - arrowLength + 10, compassY - arrowLength + 10};
        
        g2d.setColor(Color.RED);
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        // 繪製 "N" 字
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String n = "N";
        g2d.drawString(n, compassX - fm.stringWidth(n)/2, compassY - arrowLength - 5);
        
        // 恢復轉換
        g2d.setTransform(oldTransform);
    }
    
    /**
     * ⭐ 新增：根據實際尺寸（公尺）計算像素大小
     * @param meters 實際尺寸（公尺）
     * @return 像素大小
     */
    public double metersToPixels(double meters) {
        return meters * pixelsPerMeter * zoomLevel;
    }

    /**
     * ⭐ 新增：根據像素大小計算實際尺寸（公尺）
     * @param pixels 像素大小
     * @return 實際尺寸（公尺）
     */
    public double pixelsToMeters(double pixels) {
        return pixels / (pixelsPerMeter * zoomLevel);
    }

    /**
     * ⭐ 新增：設定比例（多少像素代表 1 公尺）
     * @param pixelsPerMeter 像素/公尺
     */
    public void setPixelsPerMeter(double pixelsPerMeter) {
        this.pixelsPerMeter = pixelsPerMeter;
        repaint();
    }
    
    /**
     * ⭐ 根據實際尺寸（公尺）調整物件大小
     * @param lengthMeters 長度（公尺）
     * @param widthMeters 寬度（公尺）
     */
    public void resizeSelectedShapeByMeters(double lengthMeters, double widthMeters) {
        if (selectedShape == null) return;
        if (selectedShape.isPerson || selectedShape.isEvidence) return;  // 人物和證物不調整
        
        Rectangle2D bounds = selectedShape.shape.getBounds2D();
        
        // 將公尺轉換為像素
        double newWidthPixels = metersToPixels(widthMeters);
        double newHeightPixels = metersToPixels(lengthMeters);
        
        double oldWidth = bounds.getWidth();
        double oldHeight = bounds.getHeight();
        
        if (oldWidth <= 0 || oldHeight <= 0) return;
        
        // 計算縮放比例
        double scaleX = newWidthPixels / oldWidth;
        double scaleY = newHeightPixels / oldHeight;
        
        System.out.println("調整尺寸:");
        System.out.println("  輸入: " + lengthMeters + "m × " + widthMeters + "m");
        System.out.println("  轉換: " + newHeightPixels + "px × " + newWidthPixels + "px");
        System.out.println("  原大小: " + oldHeight + "px × " + oldWidth + "px");
        System.out.println("  縮放比例: " + scaleY + " × " + scaleX);
        
        // 建立縮放轉換（以物件中心為基準）
        AffineTransform at = new AffineTransform();
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        
        at.translate(center.getX(), center.getY());
        at.scale(scaleX, scaleY);
        at.translate(-center.getX(), -center.getY());
        
        // 套用轉換
        selectedShape.shape = at.createTransformedShape(selectedShape.shape);
        
        // 更新屬性中的尺寸文字
        selectedShape.properties.length = String.format("%.2fm", lengthMeters);
        selectedShape.properties.width = String.format("%.2fm", widthMeters);
        
        // 更新控制點和屬性面板
        updateHandles();
        updatePropertyPanel();
        repaint();
        
        System.out.println("  調整完成！");
    }
    /**
     * 內部類別 MouseHandler
     * 專門用來集中處理所有滑鼠相關的事件
     */
    private class MouseHandler extends MouseAdapter {

        /**
         * 當滑鼠按鍵被按下的時候觸發
         */
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();

            if (hoveredShape != null) {
                hoveredShape = null;
                tooltipPosition = null;
            }
            
            // 檢查是否為物件蓋章模式
            if (isObjectStampMode(currentMode)) {
                stampObject(e.getPoint());
                return;
            }

            // 如果不是在 "Move" 模式，就執行傳統的繪圖邏輯
            if (!currentMode.equals("移動")) {
                handleDrawingPress(e);
                return;
            }

            // --- 以下為 "Move" 模式下的邏輯 ---
            actionStartPoint = e.getPoint();
            lastActionPoint = e.getPoint();

            // 1. 優先判斷：如果已經有物件被選取
            if (selectedShape != null) {
                // 1a. 檢查是否點到控制點 (縮放或旋轉)
                activeHandleIndex = getHandleAt(actionStartPoint);
                if (activeHandleIndex != -1) {
                    // ⭐ 新增：如果是人物部件，不允許縮放，只允許旋轉
                    if (selectedShape.isPersonPart && activeHandleIndex != ROTATION_HANDLE_INDEX) {
                        // 部件不能縮放，只能旋轉
                        return;
                    }
                    
                    // 準備進行操作，先儲存原始狀態
                    originalShapeOnActionStart = selectedShape.shape;
                    Rectangle2D bounds = originalShapeOnActionStart.getBounds2D();
                    shapeCenterOnActionStart = new Point2D.Double(
                        bounds.getCenterX(), 
                        bounds.getCenterY()
                    );

                    // 判斷點到的是旋轉點還是縮放點
                    if (activeHandleIndex == ROTATION_HANDLE_INDEX) {
                        currentAction = ActionState.ROTATING;
                        
                        // ⭐ 如果是軀幹，儲存所有部件的原始形狀
                        if (selectedShape.isPerson && selectedShape.bodyParts != null && !selectedShape.bodyParts.isEmpty()) {
                            originalBodyParts = new ArrayList<>();
                            for (ShapeWithColor part : selectedShape.bodyParts) {
                                originalBodyParts.add(part.shape);
                            }
                        }
                        
                        // ⭐ 新增：如果是部件，只儲存該部件的原始形狀
                        if (selectedShape.isPersonPart) {
                            originalBodyParts = new ArrayList<>();
                            originalBodyParts.add(selectedShape.shape);
                        }
                    } else {
                        currentAction = ActionState.SCALING;
                        
                        // ⭐ 新增：如果是軀幹，儲存所有部件的原始形狀用於縮放
                        if (selectedShape.isPerson && selectedShape.bodyParts != null && !selectedShape.bodyParts.isEmpty()) {
                            originalBodyParts = new ArrayList<>();
                            for (ShapeWithColor part : selectedShape.bodyParts) {
                                originalBodyParts.add(part.shape);
                            }
                        }
                    }
                    return; // 找到操作目標，結束此次 mousePressed 的判斷
                }

                // 1b. 檢查是否點在物件的指示框 (Bounding Box) 內部
                Rectangle2D bounds = selectedShape.shape.getBounds2D();
                if (bounds.contains(actionStartPoint)) {
                    currentAction = ActionState.MOVING;
                    return;
                }
            }

            // 2. 嘗試選取一個新物件
            ShapeWithColor clickedShape = null;
            
            for (int i = shapes.size() - 1; i >= 0; i--) {
                ShapeWithColor swc = shapes.get(i);
                
                BasicStroke hitStroke = new BasicStroke(5.0f);
                Shape hitArea = hitStroke.createStrokedShape(swc.shape);
                
                if (hitArea.contains(actionStartPoint)) {
                    clickedShape = swc;
                    break;
                }
            }

            // 3. 根據選取結果更新狀態
            if (clickedShape != null) {
                selectShape(clickedShape);
                currentAction = ActionState.MOVING;
            } else {
                deselectAll();
            }
            repaint();
        }
        /**
         * 當按住滑鼠按鍵並移動（拖曳）時觸發
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            // 如果是 Move 模式，就執行物件變換邏輯
            if (currentMode.equals("移動")) {
                Point currentPoint = e.getPoint();
                AffineTransform at = new AffineTransform();

                // 根據當前的操作狀態執行不同邏輯
                switch (currentAction) {
                    case MOVING:
                        // 移動物件
                        if (selectedShape != null) {
                            double dx = currentPoint.x - lastActionPoint.x;
                            double dy = currentPoint.y - lastActionPoint.y;
                            
                            at = AffineTransform.getTranslateInstance(dx, dy);
                            selectedShape.shape = at.createTransformedShape(selectedShape.shape);
                            
                            if (selectedShape.isPerson && selectedShape.bodyParts != null) {
                                for (ShapeWithColor part : selectedShape.bodyParts) {
                                    part.shape = at.createTransformedShape(part.shape);
                                }
                            }
                            
                            lastActionPoint = currentPoint;
                        }
                        break;

                    case SCALING:
                        // 縮放物件
                        if(originalShapeOnActionStart == null) return;
                        
                        double startDist = shapeCenterOnActionStart.distance(actionStartPoint);
                        if (startDist < 1) return;
                        
                        double currentDist = shapeCenterOnActionStart.distance(currentPoint);
                        double scale = currentDist / startDist;
                        
                        at.translate(shapeCenterOnActionStart.getX(), shapeCenterOnActionStart.getY());
                        at.scale(scale, scale);
                        at.translate(-shapeCenterOnActionStart.getX(), -shapeCenterOnActionStart.getY());
                        
                        selectedShape.shape = at.createTransformedShape(originalShapeOnActionStart);
                        
                        if (selectedShape.isPerson && selectedShape.bodyParts != null && 
                            originalBodyParts != null && !originalBodyParts.isEmpty()) {
                            
                            for (int i = 0; i < selectedShape.bodyParts.size() && i < originalBodyParts.size(); i++) {
                                selectedShape.bodyParts.get(i).shape = 
                                    at.createTransformedShape(originalBodyParts.get(i));
                            }
                        }
                        break;

                    case ROTATING:
                        // 旋轉物件
                        if(originalShapeOnActionStart == null) return;
                        /*
                        **Math.atan2(y, x)** - 反正切函數
                        - 計算從原點到座標 (x, y) 的角度
                        - 返回值範圍: -π 到 π (即 -180° 到 180°)
                        - 自動處理所有象限的角度
					   */
                        double startAngle = Math.atan2(
                            actionStartPoint.y - shapeCenterOnActionStart.getY(), 
                            actionStartPoint.x - shapeCenterOnActionStart.getX()
                        );
                        double currentAngle = Math.atan2(
                            currentPoint.y - shapeCenterOnActionStart.getY(), 
                            currentPoint.x - shapeCenterOnActionStart.getX()
                        );
                        double rotationAngle = currentAngle - startAngle;
                        
                        at.rotate(
                            rotationAngle, 
                            shapeCenterOnActionStart.getX(), 
                            shapeCenterOnActionStart.getY()
                        );
                        
                        selectedShape.shape = at.createTransformedShape(originalShapeOnActionStart);
                        
                        if (originalBodyParts != null && !originalBodyParts.isEmpty()) {
                            if (selectedShape.isPerson && selectedShape.bodyParts != null) {
                                for (int i = 0; i < selectedShape.bodyParts.size() && i < originalBodyParts.size(); i++) {
                                    selectedShape.bodyParts.get(i).shape = 
                                        at.createTransformedShape(originalBodyParts.get(i));
                                }
                            } else if (selectedShape.isPersonPart) {
                                selectedShape.shape = at.createTransformedShape(originalBodyParts.get(0));
                            }
                        }
                        break;
                        
                    default:
                        return;
                }
                
                updateHandles();        // ⭐ 每次變換後都要更新控制點的位置
                updatePropertyPanel();  // ⭐ 更新屬性面板的數值（已加入空值檢查）
                repaint();             // ⭐ 立即重繪畫布，提供即時預覽

            } else {
                // 如果是繪圖模式，就執行繪圖的拖曳邏輯
                handleDrawingDrag(e);
            }
        }

        /**
         * 當滑鼠按鍵被釋放時觸發
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (currentMode.equals("移動")) {
                // 在 Move 模式下，放開滑鼠代表一次操作結束
                // 重設所有操作狀態
                currentAction = ActionState.NONE;
                originalShapeOnActionStart = null;
                actionStartPoint = null;
                shapeCenterOnActionStart = null;
                lastActionPoint = null;
                activeHandleIndex = -1;
                originalBodyParts = null; // ⭐ 新增：清除部件原始形狀
            } else {
                // 在繪圖模式下，放開滑鼠代表圖形繪製完成
                handleDrawingRelease(e);
            }
        }

        /**
         * 當滑鼠在畫布上移動（沒有按住按鍵）時觸發
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            if (currentMode.equals("移動")) {
                // 在 Move 模式下，用來根據滑鼠位置改變游標樣式
                if (selectedShape != null) {
                    int handleIdx = getHandleAt(e.getPoint()); // 檢查滑鼠是否在某個控制點上
                    
                    if (handleIdx == ROTATION_HANDLE_INDEX) {
                        // 在旋轉控制點上
                        setCursor(rotationCursor);
                    } else if (handleIdx != -1) {
                        // 在縮放控制點上
                        setCursor(handleCursors[handleIdx]);
                    } else {
                        // 檢查是否在指示框內
                        if (selectedShape.shape.getBounds2D().contains(e.getPoint())) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        } else {
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
                checkHoveredShape(e.getPoint());
            } else {
                // 在繪圖模式下，用來顯示繪製預覽
                handleDrawingMove(e);
                checkHoveredShape(e.getPoint());
            }
        }
        /**
         * 當滑鼠離開畫布時觸發
         */
        @Override
        public void mouseExited(MouseEvent e) {
            // 清除懸停狀態
            if (hoveredShape != null) {
                hoveredShape = null;
                tooltipPosition = null;
                repaint();
            }
        }
     // --- 以下是將原本的繪圖邏輯整理到獨立的方法中 ---

        /**
         * 處理繪圖模式下的滑鼠點擊
         */
        private void handleDrawingPress(MouseEvent e) {
            if (currentMode.equals("鉛筆")) {
                // 鉛筆模式：開始繪製路徑
                startPoint = e.getPoint();
                Path2D path = new Path2D.Double();
                path.moveTo(startPoint.x, startPoint.y);
                currentShape = path;
                repaint();
                
            } else if (currentMode.equals("直線") || currentMode.equals("箭頭")) {  // ⭐ 改這裡
                // 直線和箭頭模式：需要兩次點擊
                if (!isDrawing) {
                    // 第一次點擊：記錄起始點
                    startPoint = e.getPoint();
                    isDrawing = true;
                } else {
                    // 第二次點擊：完成繪製
                    Point endPoint = e.getPoint();
                    Shape finalShape = null;
                    
                    if (currentMode.equals("直線")) {
                        finalShape = new Line2D.Double(startPoint, endPoint);
                    } else if (currentMode.equals("箭頭")) {  // ⭐ 新增這段
                        finalShape = createArrowShape(startPoint, endPoint);
                    }
                    
                    if (finalShape != null) {
                        shapes.add(new ShapeWithColor(finalShape, currentColor));
                    }
                    
                    // 重設狀態
                    isDrawing = false;
                    startPoint = null;
                    currentShape = null;
                    repaint();
                }
            }
        }

        /**
         * 處理鉛筆工具的拖曳
         */
        private void handleDrawingDrag(MouseEvent e) {
        		//檢查currentShape 是否是 Path2D 類型的實例
            if (currentMode.equals("鉛筆") && currentShape instanceof Path2D) {
                // 在鉛筆模式下，持續將點加入路徑
                ((Path2D) currentShape).lineTo(e.getX(), e.getY());
                repaint();
            }
        }

        /**
         * 處理鉛筆工具的釋放
         */
        private void handleDrawingRelease(MouseEvent e) {
            if (currentMode.equals("鉛筆") && currentShape != null) {
                // 鉛筆模式下，釋放滑鼠時將路徑加入到 shapes 列表
                shapes.add(new ShapeWithColor(currentShape, currentColor));
                currentShape = null;
                startPoint = null;
                repaint();
            }
        }

        /**
         * 處理直線的繪製預覽
         */
        private void handleDrawingMove(MouseEvent e) {
            if (isDrawing && startPoint != null) {
                // 在直線或箭頭模式下，顯示即時預覽
                Point currentPoint = e.getPoint();
                
                if (currentMode.equals("直線")) {
                    currentShape = new Line2D.Double(startPoint, currentPoint);
                } else if (currentMode.equals("箭頭")) {  // ⭐ 新增這段
                    currentShape = createArrowShape(startPoint, currentPoint);
                }
                
                repaint();
            }
        }
    } // -- MouseHandler 內部類別結束 --
 // --- 物件蓋章相關方法 ---
    /**
     * 建立箭頭形狀
     * @param start 箭頭起點
     * @param end 箭頭終點
     * @return 箭頭的 Shape 物件
     */
    private Shape createArrowShape(Point start, Point end) {
        Path2D arrow = new Path2D.Double();
        
        // 計算箭頭的方向和長度
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) {
            // 如果長度為 0，返回一個點
            arrow.moveTo(start.x, start.y);
            arrow.lineTo(start.x, start.y);
            return arrow;
        }
        
        // 計算單位向量
        double unitX = dx / length;
        double unitY = dy / length;
        
        // 箭頭參數設定
        double arrowHeadLength = Math.min(20, length * 0.3); // 箭頭長度
        double arrowHeadWidth = arrowHeadLength * 0.6;        // 箭頭寬度
        
        // 繪製箭頭主線（從起點到終點）
        arrow.moveTo(start.x, start.y);
        arrow.lineTo(end.x, end.y);
        
        // 計算箭頭底部的中心點
        double baseX = end.x - arrowHeadLength * unitX;
        double baseY = end.y - arrowHeadLength * unitY;
        
        // 計算垂直於箭頭方向的單位向量
        double perpX = -unitY;
        double perpY = unitX;
        
        // 箭頭左側的點
        double leftX = baseX + arrowHeadWidth * perpX;
        double leftY = baseY + arrowHeadWidth * perpY;
        
        // 箭頭右側的點
        double rightX = baseX - arrowHeadWidth * perpX;
        double rightY = baseY - arrowHeadWidth * perpY;
        
        // 繪製箭頭的頭部（三角形）
        arrow.moveTo(end.x, end.y);      // 箭頭尖端
        arrow.lineTo(leftX, leftY);       // 左側點
        arrow.lineTo(rightX, rightY);     // 右側點
        arrow.closePath();                // 閉合三角形
        
        return arrow;
    }
    /**
     * 繪製滑鼠懸停提示框
     * @param g2d Graphics2D 物件
     * @param shape 懸停的物件
     * @param position 提示框位置
     */
    private void drawTooltip(Graphics2D g2d, ShapeWithColor shape, Point position) {
        // 取得物件名稱
        String name = getShapeName(shape);
        
        if (name == null || name.isEmpty()) {
            return;
        }
        
        // 設定字體
        Font font = new Font("Microsoft YaHei", Font.PLAIN, 12);
        g2d.setFont(font);
        
        // 計算文字尺寸
        FontMetrics fm = g2d.getFontMetrics(font);
        int textWidth = fm.stringWidth(name);
        int textHeight = fm.getHeight();
        
        // 提示框的內邊距
        int padding = 6;
        int boxWidth = textWidth + padding * 2;
        int boxHeight = textHeight + padding;
        
        // 提示框位置（滑鼠右下方，偏移 10 像素）
        int tooltipX = position.x + 10;
        int tooltipY = position.y + 10;
        
        // 確保提示框不超出畫布邊界
        if (tooltipX + boxWidth > getWidth()) {
            tooltipX = position.x - boxWidth - 10;
        }
        if (tooltipY + boxHeight > getHeight()) {
            tooltipY = position.y - boxHeight - 10;
        }
        
        // 繪製提示框背景（淺黃色）
        g2d.setColor(new Color(255, 255, 220, 230));
        g2d.fillRoundRect(tooltipX, tooltipY, boxWidth, boxHeight, 5, 5);
        
        // 繪製提示框邊框
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(tooltipX, tooltipY, boxWidth, boxHeight, 5, 5);
        
        // 繪製文字
        g2d.setColor(Color.BLACK);
        int textX = tooltipX + padding;
        int textY = tooltipY + padding + fm.getAscent();
        g2d.drawString(name, textX, textY);
    }
    /**
     * 取得物件的名稱
     * @param shape 物件
     * @return 物件名稱
     */
    private String getShapeName(ShapeWithColor shape) {
        if (shape.isPerson) {
            return shape.personProperties != null ? shape.personProperties.name : "未命名";
        } else if (shape.isEvidence) {  // ⭐ 新增
            // 證物標示顯示：編號 + 說明
            if (shape.evidenceProperties != null) {
                String number = shape.evidenceProperties.number;
                String desc = shape.evidenceProperties.description;
                
                if (desc != null && !desc.isEmpty()) {
                    return "編號 " + number + ": " + desc;
                } else {
                    return "編號 " + number;
                }
            }
            return "證物標示";
        }  else if (shape.isPersonPart) {  // ⭐ 新增:處理人物部件
            // 根據部件類型返回對應的中文名稱
            if (shape.partType != null) {
                switch (shape.partType) {
                    case "leftArm":
                        return shape.parentPerson.personProperties.name+"的左手";
                    case "rightArm":
                        return shape.parentPerson.personProperties.name+"的右手";
                    case "leftLeg":
                        return shape.parentPerson.personProperties.name+"的左腳";
                    case "rightLeg":
                        return shape.parentPerson.personProperties.name+"的右腳";
                    case "torso":
                        // 如果單獨選到軀幹,顯示人物名稱
                        if (shape.parentPerson != null && shape.parentPerson.personProperties != null) {
                            return shape.parentPerson.personProperties.name;
                        }
                        return "軀幹";
                    default:
                        return "部件";
                }
            }
            // 如果有父物件,嘗試顯示父物件名稱
            if (shape.parentPerson != null && shape.parentPerson.personProperties != null) {
                return shape.parentPerson.personProperties.name + " - 部件";
            }
            return "部件";
        	}
        else {
            return shape.properties != null ? shape.properties.name : "未命名";
        }
    }
    /**
     * 檢查滑鼠是否懸停在某個物件上
     * @param point 滑鼠位置
     */
    private void checkHoveredShape(Point point) {
        ShapeWithColor newHoveredShape = null;
        
        // 從最上層的圖形開始檢查
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeWithColor swc = shapes.get(i);
            
            // 跳過已選取的物件
            if (swc == selectedShape) {
                continue;
            }
            
            // 使用較寬的筆觸來增加可懸停區域
            BasicStroke hitStroke = new BasicStroke(8.0f);
            Shape hitArea = hitStroke.createStrokedShape(swc.shape);
            
            if (hitArea.contains(point)) {
                newHoveredShape = swc;
                break;
            }
        }
        
        // 如果懸停物件改變了，更新並重繪
        if (newHoveredShape != hoveredShape) {
            hoveredShape = newHoveredShape;
            tooltipPosition = (hoveredShape != null) ? point : null;
            repaint();
        } else if (hoveredShape != null) {
            // 如果還在同一個物件上，更新提示框位置
            tooltipPosition = point;
            repaint();
        }
    }
    /**
     * 判斷是否為物件蓋章模式
     * @param mode 模式名稱
     * @return 是否為物件蓋章模式
     */
    private boolean isObjectStampMode(String mode) {
        switch (mode) {
            case "椅子":
            case "桌子":
            case "窗戶":
            case "門":
            case "石頭":
            case "車子":
            case "樹":
            case "樹枝":
            case "證物標示":  // ⭐ 新增
            case "人":
            case "刀":
            case "槍":
                return true;
            default:
            		// ⭐ 新增：檢查是否為自訂物件
                return mainWindow != null && mainWindow.isCustomObject(mode);
        }
    }
    /**
     * 在指定位置蓋上物件
     * @param p 蓋章位置
     */
    private void stampObject(Point p) {
        Shape newObject = null;
        boolean isPerson = false;
        boolean isEvidence = false;

        switch (currentMode) {
        case "椅子":
            // 載入椅子的 SVG
            Shape chairSVG = SVGLoader.loadSVGFromResource("/svg/chair.svg");
            // 縮放到正確大小（0.5m × 0.5m）
            double chairSize = metersToPixels(0.4);
            chairSVG = SVGLoader.scaleShape(chairSVG, chairSize, chairSize);
            // 移動到點擊位置
            newObject = SVGLoader.centerShape(chairSVG, p.x, p.y);
            break;
            
        case "桌子":
            // 載入桌子的 SVG
            Shape tableSVG = SVGLoader.loadSVGFromResource("/svg/table.svg");
            // 縮放到正確大小（1.2m × 0.8m）
            double tableWidth = metersToPixels(1.2);
            double tableHeight = metersToPixels(0.8);
            tableSVG = SVGLoader.scaleShape(tableSVG, tableWidth, tableHeight);
            // 移動到點擊位置
            newObject = SVGLoader.centerShape(tableSVG, p.x, p.y);
            break;
            
        case "窗戶":
            Shape windowSVG = SVGLoader.loadSVGFromResource("/svg/window.svg");
            double windowWidth = metersToPixels(1.5);
            double windowHeight = metersToPixels(0.15);
            windowSVG = SVGLoader.scaleShape(windowSVG, windowWidth, windowHeight);
            newObject = SVGLoader.centerShape(windowSVG, p.x, p.y);
            break;
            
        case "門":
            Shape doorSVG = SVGLoader.loadSVGFromResource("/svg/door.svg");
            double doorWidth = metersToPixels(2.0);
            double doorHeight = metersToPixels(0.8);
            doorSVG = SVGLoader.scaleShape(doorSVG, doorWidth, doorHeight);
            newObject = SVGLoader.centerShape(doorSVG, p.x, p.y);
            break;
        case "石頭":
            Shape rockSVG = SVGLoader.loadSVGFromResource("/svg/rock.svg");
            double rockWidth = metersToPixels(0.5);
            double rockHeight = metersToPixels(0.5);
            rockSVG = SVGLoader.scaleShape(rockSVG, rockWidth, rockHeight);
            newObject = SVGLoader.centerShape(rockSVG, p.x, p.y);
            break;
        case "車子":
            Shape carSVG = SVGLoader.loadSVGFromResource("/svg/car.svg");
            double carWidth = metersToPixels(1.8);
            double carHeight = metersToPixels(4.4);
            carSVG = SVGLoader.scaleShape(carSVG, carWidth, carHeight);
            newObject = SVGLoader.centerShape(carSVG, p.x, p.y);
            break;
        case "樹":
            Shape treeSVG = SVGLoader.loadSVGFromResource("/svg/tree.svg");
            double treeWidth = metersToPixels(3.0);
            double treeHeight = metersToPixels(3.0);
            treeSVG = SVGLoader.scaleShape(treeSVG, treeWidth, treeHeight);
            newObject = SVGLoader.centerShape(treeSVG, p.x, p.y);
            break;
        case "樹枝":
            Shape branchSVG = SVGLoader.loadSVGFromResource("/svg/branch.svg");
            double branchWidth = metersToPixels(0.2);
            double branchHeight = metersToPixels(0.8);
            branchSVG = SVGLoader.scaleShape(branchSVG, branchWidth, branchHeight);
            newObject = SVGLoader.centerShape(branchSVG, p.x, p.y);
            break;
        case "證物標示":
            newObject = createEvidenceMarker(p);
            isEvidence = true;
            break;
            
        case "人":
        		// ⭐ 修改：建立由 5 個部件組成的人物
            createPersonWithParts(p);
            return; // 直接返回，不執行後面的程式碼

            
        case "刀":
            Shape knifeSVG = SVGLoader.loadSVGFromResource("/svg/knife.svg");
            double knifeWidth = metersToPixels(0.3);
            double knifeHeight = metersToPixels(0.1);
            knifeSVG = SVGLoader.scaleShape(knifeSVG, knifeWidth, knifeHeight);
            newObject = SVGLoader.centerShape(knifeSVG, p.x, p.y);
            break;
            
        case "槍":
            Shape gunSVG = SVGLoader.loadSVGFromResource("/svg/gun.svg");
            double gunLength = metersToPixels(0.25);
            double gunHeight = metersToPixels(0.2);
            gunSVG = SVGLoader.scaleShape(gunSVG, gunLength, gunHeight);
            newObject = SVGLoader.centerShape(gunSVG, p.x, p.y);
            break;
            
        default:
            // ⭐ 修改：處理自訂物件（支援長寬）
            if (mainWindow != null && mainWindow.isCustomObject(currentMode)) {
                MainWindow.CustomObject customObj = mainWindow.getCustomObject(currentMode);
                if (customObj != null) {
                    // 從檔案載入 SVG
                    Shape customSVG = SVGLoader.loadSVG(customObj.filePath);
                    if (customSVG != null) {
                        // ⭐ 修改：根據設定的長寬縮放
                        double customWidth = metersToPixels(customObj.width);
                        double customHeight = metersToPixels(customObj.height);
                        customSVG = SVGLoader.scaleShape(customSVG, customWidth, customHeight);
                        // 移動到點擊位置
                        newObject = SVGLoader.centerShape(customSVG, p.x, p.y);
                        
                        System.out.println("蓋章自訂物件：" + currentMode + 
                            " (" + customObj.width + "m × " + customObj.height + "m)");
                    } else {
                        System.err.println("載入自訂物件失敗：" + currentMode);
                        JOptionPane.showMessageDialog(
                            this,
                            "載入自訂物件失敗！",
                            "錯誤",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                }
            } else {
                // 不認識的模式，建立預設形狀
                double defaultSize = metersToPixels(0.5);
                newObject = new Rectangle2D.Double(
                    p.x - defaultSize/2, 
                    p.y - defaultSize/2, 
                    defaultSize, 
                    defaultSize
                );
            }
    }

        if (newObject != null) {
            ShapeWithColor swc = new ShapeWithColor(newObject, currentColor, isPerson, isEvidence);
            // 檢查是否為證物標示，如果是，則設定自動遞增的編號
            if (isEvidence) {
                swc.evidenceProperties.number = String.valueOf(evidenceCounter++);
            }
            shapes.add(swc);
            repaint();
        }
    }
    /**
     * ⭐ 改進版:建立由圓形和圓角矩形組成的精緻人物
     * @param center 人物中心位置
     */
    private void createPersonWithParts(Point center) {
        double personHeight = metersToPixels(1.8);
        double personWidth = metersToPixels(0.38);
        
        // 計算各部件的尺寸
        double headRadius = personHeight * 0.08;        // 頭部半徑
        double neckHeight = personHeight * 0.05;         // 頸部高度
        double torsoHeight = personHeight * 0.35;        // 軀幹高度
        double legHeight = personHeight * 0.4;           // 腿部高度
        double armLength = personHeight * 0.35;          // 手臂長度
        
        double torsoWidth = personWidth * 0.6;           // 軀幹寬度
        double limbWidth = personWidth * 0.12;           // 四肢寬度
        
        // 計算位置 - 從頭部開始往下
        double topY = center.y - personHeight/2;
        
        // 1. 建立頭部(圓形)
        Ellipse2D headCircle = new Ellipse2D.Double(
            center.x - headRadius,
            topY,
            headRadius * 2,
            headRadius * 2
        );
        
        // 2. 建立軀幹(圓角矩形,包含頸部和身體)
        double torsoTop = topY + headRadius * 2;
        RoundRectangle2D torsoRect = new RoundRectangle2D.Double(
            center.x - torsoWidth/2,
            torsoTop,
            torsoWidth,
            neckHeight + torsoHeight,
            limbWidth * 1.5,  // 圓角寬度
            limbWidth * 1.5   // 圓角高度
        );
        
        // 將頭部和軀幹組合成一個 Path
        Path2D.Double torsoPath = new Path2D.Double();
        torsoPath.append(headCircle, false);
        torsoPath.append(torsoRect, false);
        
        ShapeWithColor torso = new ShapeWithColor(torsoPath, currentColor, true, false);
        torso.personProperties.name = "鄭子儀";
        
        // 3. 建立左手臂(圓角矩形)
        double armTop = torsoTop + neckHeight;
        RoundRectangle2D leftArmRect = new RoundRectangle2D.Double(
            center.x - torsoWidth/2 - limbWidth,
            armTop,
            limbWidth,
            armLength,
            limbWidth * 0.8,  // 圓角
            limbWidth * 0.8
        );
        ShapeWithColor leftArm = new ShapeWithColor(leftArmRect, currentColor, "leftArm", torso);
        
        // 4. 建立右手臂(圓角矩形)
        RoundRectangle2D rightArmRect = new RoundRectangle2D.Double(
            center.x + torsoWidth/2,
            armTop,
            limbWidth,
            armLength,
            limbWidth * 0.8,
            limbWidth * 0.8
        );
        ShapeWithColor rightArm = new ShapeWithColor(rightArmRect, currentColor, "rightArm", torso);
        
        // 5. 建立左腿(圓角矩形)
        double legTop = torsoTop + neckHeight + torsoHeight;
        RoundRectangle2D leftLegRect = new RoundRectangle2D.Double(
            center.x - torsoWidth/2 + limbWidth * 0.3,
            legTop,
            limbWidth,
            legHeight,
            limbWidth * 0.8,
            limbWidth * 0.8
        );
        ShapeWithColor leftLeg = new ShapeWithColor(leftLegRect, currentColor, "leftLeg", torso);
        
        // 6. 建立右腿(圓角矩形)
        RoundRectangle2D rightLegRect = new RoundRectangle2D.Double(
            center.x + torsoWidth/2 - limbWidth * 1.3,
            legTop,
            limbWidth,
            legHeight,
            limbWidth * 0.8,
            limbWidth * 0.8
        );
        ShapeWithColor rightLeg = new ShapeWithColor(rightLegRect, currentColor, "rightLeg", torso);
        
        // 將所有部件加入軀幹的子部件列表
        torso.bodyParts.add(leftArm);
        torso.bodyParts.add(rightArm);
        torso.bodyParts.add(leftLeg);
        torso.bodyParts.add(rightLeg);
        
        // 將所有部件加入畫布(順序:先加軀幹,再加四肢)
        shapes.add(torso);
        shapes.add(leftArm);
        shapes.add(rightArm);
        shapes.add(leftLeg);
        shapes.add(rightLeg);
        
        System.out.println("已建立人物");
        repaint();
    }
    /**
     * ⭐ 新增：建立證物標示標記
     * @param center 中心點位置
     * @return 證物標示的 Shape
     */
    private Shape createEvidenceMarker(Point center) {
        // 建立一個方框（40x40 像素）
        int size = 25;
        int halfSize = size / 2;
        
        Rectangle2D marker = new Rectangle2D.Double(
            center.x - halfSize, 
            center.y - halfSize, 
            size, 
            size
        );
        
        return marker;
    }
    /**
     * 清除整個畫布
     */
    public void clear() {
        shapes.clear();
        isDrawing = false;
        currentShape = null;
        startPoint = null;
        deselectAll();
        repaint();
    }

    // --- 序列化方法 (用於存檔和讀檔) ---

    /**
     * 獲取繪圖資料
     * 給 MainWindow 的儲存功能使用
     * @return 包含所有圖形物件的列表
     */
    @SuppressWarnings("rawtypes")
    public List getDrawingData() {
        return shapes;
    }

    /**
     * 載入繪圖資料
     * 給 MainWindow 的讀取功能使用
     * @param data 從檔案讀取出的物件
     */
    @SuppressWarnings("unchecked")
    public void loadDrawingData(Object data) {
        if (data instanceof List) {
            shapes.clear();
            this.shapes = (List<ShapeWithColor>) data; // 進行類型轉換
            
            // 重設所有狀態
            isDrawing = false;
            currentShape = null;
            startPoint = null;
            deselectAll();
            repaint(); // 重繪以顯示載入的內容
        } else {
            System.err.println("載入資料格式不符！");
        }
    }
 // --- 控制點相關的輔助方法 ---

    /**
     * 更新所有控制點的位置
     * 包含四個角落的縮放點和頂部的旋轉點
     */
    private void updateHandles() {
        // 先清除舊的控制點
        handles.clear();
        rotationHandle = null;
        rotationHandleStick = null;

        if (selectedShape == null) {
            return;
        }

        // 取得物件的邊界框
        Rectangle2D bounds = selectedShape.shape.getBounds2D();
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double halfHandle = HANDLE_SIZE / 2.0;

        // 依序加入左上、右上、右下、左下四個角的縮放點
        handles.add(new Rectangle2D.Double(
            x - halfHandle, 
            y - halfHandle, 
            HANDLE_SIZE, 
            HANDLE_SIZE
        ));
        handles.add(new Rectangle2D.Double(
            x + w - halfHandle, 
            y - halfHandle, 
            HANDLE_SIZE, 
            HANDLE_SIZE
        ));
        handles.add(new Rectangle2D.Double(
            x + w - halfHandle, 
            y + h - halfHandle, 
            HANDLE_SIZE, 
            HANDLE_SIZE
        ));
        handles.add(new Rectangle2D.Double(
            x - halfHandle, 
            y + h - halfHandle, 
            HANDLE_SIZE, 
            HANDLE_SIZE
        ));

        // 計算並建立旋轉點
        double stickLength = 20.0; // 旋轉點指示線的長度
        Point2D topCenter = new Point2D.Double(bounds.getCenterX(), bounds.getY());
        Point2D rotationHandlePos = new Point2D.Double(
            topCenter.getX(), 
            topCenter.getY() - stickLength
        );
        
        // 建立連接線和旋轉控制點
        rotationHandleStick = new Line2D.Double(topCenter, rotationHandlePos);
        rotationHandle = new Ellipse2D.Double(
            rotationHandlePos.getX() - halfHandle, 
            rotationHandlePos.getY() - halfHandle, 
            HANDLE_SIZE, 
            HANDLE_SIZE
        );
    }

    /**
     * 根據滑鼠位置獲取控制點索引
     * @param p 滑鼠位置
     * @return 控制點的索引 (0-3 for scaling, 4 for rotation)，如果沒點到則回傳 -1
     */
    private int getHandleAt(Point p) {
        // 優先檢查是否點到旋轉點
        if (rotationHandle != null && rotationHandle.contains(p)) {
            return ROTATION_HANDLE_INDEX;
        }
        
        // 再檢查是否點到角落的縮放點
        for (int i = 0; i < handles.size(); i++) {
            if (handles.get(i).contains(p)) {
                return i;
            }
        }
        
        return -1; // 沒有點到任何控制點
    }

    /**
     * 取消所有選取和操作狀態
     */
    private void deselectAll() {
        selectedShape = null;
        currentAction = ActionState.NONE;
        handles.clear();
        rotationHandle = null;
        rotationHandleStick = null;
        setCursor(Cursor.getDefaultCursor()); // 恢復預設滑鼠樣式
        
        // 通知 MainWindow 隱藏屬性面板
        if (mainWindow != null) {
            mainWindow.hidePropertyPanel();
        }
        
        repaint();
    }
    
    /**
     * 選取物件時的處理
     * @param shape 要選取的物件
     */
    private void selectShape(ShapeWithColor shape) {
        selectedShape = shape;
        updateHandles();
        
        if (mainWindow != null) {
            // ⭐ 修改：人物部件也顯示人物面板
            if (shape.isPerson || (shape.isPersonPart && shape.parentPerson != null)) {
                mainWindow.showPersonPanel();
                // 如果是部件，顯示父軀幹的屬性
                if (shape.isPersonPart && shape.parentPerson != null) {
                    if (shape.parentPerson.personProperties != null) {
                        mainWindow.updatePersonPanel(shape.parentPerson.personProperties);
                    }
                } else {
                    updatePersonPanel();
                }
            } else if (shape.isEvidence) {
                mainWindow.showEvidencePanel();
                updateEvidencePanel();
            } else {
                mainWindow.showPropertyPanel();
                updatePropertyPanel();
            }
        }
        
        repaint();
    }
    
    /**
     * 更新證物標示屬性面板的顯示內容
     */
    private void updateEvidencePanel() {
        if (mainWindow == null || selectedShape == null) return;
        
        // ⭐ 新增：只有證物標示才更新證物面板
        if (!selectedShape.isEvidence) return;
        
        // ⭐ 新增：檢查 evidenceProperties 是否為 null
        if (selectedShape.evidenceProperties == null) return;
        
        mainWindow.updateEvidencePanel(selectedShape.evidenceProperties);
    }
    
    /**
     * ⭐ 新增：更新選取證物標示的屬性
     * @param props 新的證物標示屬性資料
     */
    public void updateSelectedEvidenceProperties(EvidencePropertyPanel.EvidenceProperties props) {
        if (selectedShape == null || !selectedShape.isEvidence) return;
        
        selectedShape.evidenceProperties.number = props.number;
        selectedShape.evidenceProperties.description = props.description;
        
        // 需要重繪，因為編號會顯示在標記上
        repaint();
    }
    /**
     * 更新屬性面板的顯示內容
     */
    private void updatePropertyPanel() {
        if (mainWindow == null || selectedShape == null) return;
        // ⭐ 新增：人物和證物不更新一般屬性面板
        if (selectedShape.isPerson || selectedShape.isEvidence) return;
        
        // ⭐ 新增：人物部件也不更新一般屬性面板
        if (selectedShape.isPersonPart) return;
        
        // ⭐ 新增：檢查 properties 是否為 null
        if (selectedShape.properties == null) return;
        Rectangle2D bounds = selectedShape.shape.getBounds2D();
        
        // ⭐ 將像素轉換為公尺
        double widthMeters = pixelsToMeters(bounds.getWidth());
        double heightMeters = pixelsToMeters(bounds.getHeight());
        
        // ⭐ 更新屬性中的長寬數值
        selectedShape.properties.width = String.format("%.2fm", widthMeters);
        selectedShape.properties.length = String.format("%.2fm", heightMeters);
        
        System.out.println("更新屬性面板:");
        System.out.println("  像素: " + bounds.getHeight() + "px × " + bounds.getWidth() + "px");
        System.out.println("  公尺: " + heightMeters + "m × " + widthMeters + "m");
        
        mainWindow.updatePropertyPanel(selectedShape.properties);
    }

    /**
     * 更新選取物件的屬性
     * @param props 新的屬性資料
     */
    public void updateSelectedShapeProperties(PropertyPanel.ShapeProperties props) {
        if (selectedShape == null) return;
        
        // 更新物件的所有屬性
        selectedShape.properties.name = props.name;
        selectedShape.properties.description = props.description;
        selectedShape.properties.color = props.color;
        
        // ⭐ 長寬高只更新文字，不改變實際大小
        // 實際大小的改變由 resizeSelectedShapeByMeters 處理
        selectedShape.properties.length = props.length;
        selectedShape.properties.width = props.width;
        selectedShape.properties.height = props.height;
        
        // ⭐ 更新畫布上的顏色
        selectedShape.color = props.color;
        
        // 重繪畫布
        repaint();
    }

    /**
     * 更新選取物件的顏色
     * @param color 新的顏色
     */
    public void updateSelectedShapeColor(Color color) {
        if (selectedShape == null) return;
        
        // 更新屬性中的顏色
        selectedShape.properties.color = color;
        // ⭐ 重點：同時更新畫布上的顏色
        selectedShape.color = color;
        
        repaint();
    }
    
    /**
     * 更新人物屬性面板的顯示內容
     */
    private void updatePersonPanel() {
        if (mainWindow == null || selectedShape == null) return;
        
        // ⭐ 新增：只有人物才更新人物面板
        if (!selectedShape.isPerson) {
            // 如果是部件，顯示父軀幹的屬性
            if (selectedShape.isPersonPart && selectedShape.parentPerson != null) {
                if (selectedShape.parentPerson.personProperties != null) {
                    mainWindow.updatePersonPanel(selectedShape.parentPerson.personProperties);
                }
            }
            return;
        }
        
        // ⭐ 新增：檢查 personProperties 是否為 null
        if (selectedShape.personProperties == null) return;
        
        mainWindow.updatePersonPanel(selectedShape.personProperties);
    }

    /**
     * 更新選取人物的屬性
     * @param props 新的人物屬性資料
     */
    public void updateSelectedPersonProperties(PersonPropertyPanel.PersonProperties props) {
        if (selectedShape == null || !selectedShape.isPerson) return;
        
        selectedShape.personProperties.name = props.name;
        selectedShape.personProperties.gender = props.gender;
        selectedShape.personProperties.height = props.height;
        selectedShape.personProperties.weight = props.weight;
        selectedShape.personProperties.age = props.age;
        selectedShape.personProperties.description = props.description;
    }
    /**
     * ⭐ 新增：內部類別 KeyHandler
     * 專門用來處理鍵盤事件
     */
    private class KeyHandler extends java.awt.event.KeyAdapter {
        
        /**
         * 當鍵盤按鍵被按下時觸發
         */
        @Override
        public void keyPressed(java.awt.event.KeyEvent e) {
            // 只在「移動」模式下才能刪除物件
            if (!currentMode.equals("移動")) {
                return;
            }
            
            // 檢查是否有物件被選取
            if (selectedShape == null) {
                return;
            }
            
            int keyCode = e.getKeyCode();
            
            // 檢查是否按下 Backspace 或 Delete 鍵
            if (keyCode == java.awt.event.KeyEvent.VK_BACK_SPACE || 
                keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                
                // 刪除選取的物件
                deleteSelectedShape();
            }
        }
    }
    
    /**
     * ⭐ 修改：刪除選取的物件（支援刪除整個人物）
     */
    private void deleteSelectedShape() {
        if (selectedShape == null) {
            return;
        }
        
        // 顯示確認對話框
        int response = JOptionPane.showConfirmDialog(
            this,
            "確定要刪除此物件嗎？",
            "刪除確認",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        // 如果使用者選擇「是」
        if (response == JOptionPane.YES_OPTION) {
            // ⭐ 新增：如果是軀幹，同時刪除所有部件
            if (selectedShape.isPerson && selectedShape.bodyParts != null) {
                for (ShapeWithColor part : selectedShape.bodyParts) {
                    shapes.remove(part);
                }
            }
            
            // ⭐ 新增：如果是部件，同時刪除父軀幹和其他部件
            if (selectedShape.isPersonPart && selectedShape.parentPerson != null) {
                ShapeWithColor parent = selectedShape.parentPerson;
                // 刪除父軀幹
                shapes.remove(parent);
                // 刪除所有兄弟部件
                if (parent.bodyParts != null) {
                    for (ShapeWithColor part : parent.bodyParts) {
                        shapes.remove(part);
                    }
                }
            }
            
            // 從列表中移除物件本身
            shapes.remove(selectedShape);
            
            // 取消選取
            deselectAll();
            
            // 重繪畫布
            repaint();
            
            System.out.println("物件已刪除");
        }
    }
}