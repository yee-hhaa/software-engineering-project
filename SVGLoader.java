import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * SVGLoader 類別 - 載入 SVG 檔案並轉換為 Java Shape
 * 支援動態改變顏色
 */
public class SVGLoader {
    
    /**
     * 從檔案載入 SVG 並轉換為 Shape
     * @param svgFilePath SVG 檔案路徑（例如：resources/chair.svg）
     * @return Shape 物件
     */
    public static Shape loadSVG(String svgFilePath) {
        try {
            // 讀取 SVG 檔案
            File svgFile = new File(svgFilePath);
            if (!svgFile.exists()) {
                System.err.println("SVG 檔案不存在：" + svgFilePath);
                return createDefaultShape();
            }
            
            // 解析 XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(svgFile);
            
            // 取得 SVG 根元素
            Element svgElement = doc.getDocumentElement();
            
            // 解析 SVG 路徑並轉換為 Shape
            return parseSVGElement(svgElement);
            
        } catch (Exception e) {
            System.err.println("載入 SVG 失敗：" + e.getMessage());
            e.printStackTrace();
            return createDefaultShape();
        }
    }
    
    /**
     * 從資源資料夾載入 SVG
     * @param resourcePath 資源路徑（例如：/svg/chair.svg）
     * @return Shape 物件
     */
    public static Shape loadSVGFromResource(String resourcePath) {
        try {
            // 嘗試多種載入方式
            InputStream inputStream = null;
            
            // 方法 1：使用 Class.getResourceAsStream
            inputStream = SVGLoader.class.getResourceAsStream(resourcePath);
            
            // 方法 2：如果方法 1 失敗，嘗試使用 ClassLoader
            if (inputStream == null) {
                inputStream = SVGLoader.class.getClassLoader().getResourceAsStream(resourcePath.substring(1));
            }
            
            // 方法 3：如果前兩個都失敗，嘗試不帶開頭的斜線
            if (inputStream == null) {
                String pathWithoutSlash = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                inputStream = SVGLoader.class.getClassLoader().getResourceAsStream(pathWithoutSlash);
            }
            
            if (inputStream == null) {
                System.err.println("找不到資源：" + resourcePath);
                System.err.println("請確認：");
                System.err.println("1. resources 資料夾已設定為 Resources Root");
                System.err.println("2. svg 資料夾位於 resources 資料夾內");
                System.err.println("3. chair.svg 檔案位於 svg 資料夾內");
                return createDefaultShape();
            }
            
            // 解析 XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            
            // 取得 SVG 根元素
            Element svgElement = doc.getDocumentElement();
            
            // 解析 SVG 路徑並轉換為 Shape
            return parseSVGElement(svgElement);
            
        } catch (Exception e) {
            System.err.println("載入 SVG 資源失敗：" + e.getMessage());
            e.printStackTrace();
            return createDefaultShape();
        }
    }
    
    /**
     * 解析 SVG 元素並轉換為 Shape
     */
    private static Shape parseSVGElement(Element svgElement) {
        Path2D.Double combinedPath = new Path2D.Double();
        
        // 尋找所有 path 元素
        NodeList pathNodes = svgElement.getElementsByTagName("path");
        for (int i = 0; i < pathNodes.getLength(); i++) {
            Element pathElement = (Element) pathNodes.item(i);
            String d = pathElement.getAttribute("d");
            if (d != null && !d.isEmpty()) {
                Shape pathShape = parseSVGPath(d);
                combinedPath.append(pathShape, false);
            }
        }
        
        // 尋找所有 rect 元素
        NodeList rectNodes = svgElement.getElementsByTagName("rect");
        for (int i = 0; i < rectNodes.getLength(); i++) {
            Element rectElement = (Element) rectNodes.item(i);
            Shape rectShape = parseRect(rectElement);
            if (rectShape != null) {
                combinedPath.append(rectShape, false);
            }
        }
        
        // 尋找所有 circle 元素
        NodeList circleNodes = svgElement.getElementsByTagName("circle");
        for (int i = 0; i < circleNodes.getLength(); i++) {
            Element circleElement = (Element) circleNodes.item(i);
            Shape circleShape = parseCircle(circleElement);
            if (circleShape != null) {
                combinedPath.append(circleShape, false);
            }
        }
        
        // 尋找所有 ellipse 元素
        NodeList ellipseNodes = svgElement.getElementsByTagName("ellipse");
        for (int i = 0; i < ellipseNodes.getLength(); i++) {
            Element ellipseElement = (Element) ellipseNodes.item(i);
            Shape ellipseShape = parseEllipse(ellipseElement);
            if (ellipseShape != null) {
                combinedPath.append(ellipseShape, false);
            }
        }
        
        // 尋找所有 line 元素
        NodeList lineNodes = svgElement.getElementsByTagName("line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineElement = (Element) lineNodes.item(i);
            Shape lineShape = parseLine(lineElement);
            if (lineShape != null) {
                combinedPath.append(lineShape, false);
            }
        }
        
        return combinedPath;
    }
    
    /**
     * 解析 SVG path 的 d 屬性
     */
    private static Shape parseSVGPath(String d) {
        Path2D.Double path = new Path2D.Double();
        
        // 移除多餘空白
        d = d.trim().replaceAll("\\s+", " ");
        
        // 簡化的 SVG path 解析器
        String[] commands = d.split("(?=[MmLlHhVvCcSsQqTtAaZz])");
        
        double currentX = 0, currentY = 0;
        double lastControlX = 0, lastControlY = 0;
        
        for (String cmd : commands) {
            if (cmd.isEmpty()) continue;
            
            char command = cmd.charAt(0);
            String params = cmd.substring(1).trim();
            
            if (params.isEmpty() && command != 'Z' && command != 'z') continue;
            
            String[] coords = params.isEmpty() ? new String[0] : params.split("[\\s,]+");
            
            try {
                switch (command) {
                    case 'M': // 絕對移動
                        currentX = Double.parseDouble(coords[0]);
                        currentY = Double.parseDouble(coords[1]);
                        path.moveTo(currentX, currentY);
                        break;
                    case 'm': // 相對移動
                        currentX += Double.parseDouble(coords[0]);
                        currentY += Double.parseDouble(coords[1]);
                        path.moveTo(currentX, currentY);
                        break;
                    case 'L': // 絕對直線
                        currentX = Double.parseDouble(coords[0]);
                        currentY = Double.parseDouble(coords[1]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'l': // 相對直線
                        currentX += Double.parseDouble(coords[0]);
                        currentY += Double.parseDouble(coords[1]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'H': // 絕對水平線
                        currentX = Double.parseDouble(coords[0]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'h': // 相對水平線
                        currentX += Double.parseDouble(coords[0]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'V': // 絕對垂直線
                        currentY = Double.parseDouble(coords[0]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'v': // 相對垂直線
                        currentY += Double.parseDouble(coords[0]);
                        path.lineTo(currentX, currentY);
                        break;
                    case 'C': // 絕對三次貝茲曲線
                        double cx1 = Double.parseDouble(coords[0]);
                        double cy1 = Double.parseDouble(coords[1]);
                        double cx2 = Double.parseDouble(coords[2]);
                        double cy2 = Double.parseDouble(coords[3]);
                        currentX = Double.parseDouble(coords[4]);
                        currentY = Double.parseDouble(coords[5]);
                        path.curveTo(cx1, cy1, cx2, cy2, currentX, currentY);
                        lastControlX = cx2;
                        lastControlY = cy2;
                        break;
                    case 'c': // 相對三次貝茲曲線
                        cx1 = currentX + Double.parseDouble(coords[0]);
                        cy1 = currentY + Double.parseDouble(coords[1]);
                        cx2 = currentX + Double.parseDouble(coords[2]);
                        cy2 = currentY + Double.parseDouble(coords[3]);
                        currentX += Double.parseDouble(coords[4]);
                        currentY += Double.parseDouble(coords[5]);
                        path.curveTo(cx1, cy1, cx2, cy2, currentX, currentY);
                        lastControlX = cx2;
                        lastControlY = cy2;
                        break;
                    case 'Z':
                    case 'z': // 閉合路徑
                        path.closePath();
                        break;
                }
            } catch (Exception e) {
                System.err.println("解析 SVG 路徑失敗：" + cmd);
            }
        }
        
        return path;
    }
    
    /**
     * 解析 rect 元素
     */
    private static Shape parseRect(Element rectElement) {
        try {
            double x = Double.parseDouble(rectElement.getAttribute("x"));
            double y = Double.parseDouble(rectElement.getAttribute("y"));
            double width = Double.parseDouble(rectElement.getAttribute("width"));
            double height = Double.parseDouble(rectElement.getAttribute("height"));
            
            String rx = rectElement.getAttribute("rx");
            String ry = rectElement.getAttribute("ry");
            
            if (!rx.isEmpty() && !ry.isEmpty()) {
                double radiusX = Double.parseDouble(rx);
                double radiusY = Double.parseDouble(ry);
                return new RoundRectangle2D.Double(x, y, width, height, radiusX * 2, radiusY * 2);
            }
            
            return new Rectangle2D.Double(x, y, width, height);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析 circle 元素
     */
    private static Shape parseCircle(Element circleElement) {
        try {
            double cx = Double.parseDouble(circleElement.getAttribute("cx"));
            double cy = Double.parseDouble(circleElement.getAttribute("cy"));
            double r = Double.parseDouble(circleElement.getAttribute("r"));
            
            return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析 ellipse 元素
     */
    private static Shape parseEllipse(Element ellipseElement) {
        try {
            double cx = Double.parseDouble(ellipseElement.getAttribute("cx"));
            double cy = Double.parseDouble(ellipseElement.getAttribute("cy"));
            double rx = Double.parseDouble(ellipseElement.getAttribute("rx"));
            double ry = Double.parseDouble(ellipseElement.getAttribute("ry"));
            
            return new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析 line 元素
     */
    private static Shape parseLine(Element lineElement) {
        try {
            double x1 = Double.parseDouble(lineElement.getAttribute("x1"));
            double y1 = Double.parseDouble(lineElement.getAttribute("y1"));
            double x2 = Double.parseDouble(lineElement.getAttribute("x2"));
            double y2 = Double.parseDouble(lineElement.getAttribute("y2"));
            
            return new Line2D.Double(x1, y1, x2, y2);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 縮放 Shape 到指定大小
     * @param shape 原始 Shape
     * @param targetWidth 目標寬度
     * @param targetHeight 目標高度
     * @return 縮放後的 Shape
     */
    public static Shape scaleShape(Shape shape, double targetWidth, double targetHeight) {
        Rectangle2D bounds = shape.getBounds2D();
        
        double scaleX = targetWidth / bounds.getWidth();
        double scaleY = targetHeight / bounds.getHeight();
        
        AffineTransform transform = new AffineTransform();
        transform.translate(-bounds.getX(), -bounds.getY());
        transform.scale(scaleX, scaleY);
        
        return transform.createTransformedShape(shape);
    }
    
    /**
     * 將 Shape 移動到指定中心點
     * @param shape 原始 Shape
     * @param centerX 目標中心 X
     * @param centerY 目標中心 Y
     * @return 移動後的 Shape
     */
    public static Shape centerShape(Shape shape, double centerX, double centerY) {
        Rectangle2D bounds = shape.getBounds2D();
        
        double dx = centerX - bounds.getCenterX();
        double dy = centerY - bounds.getCenterY();
        
        AffineTransform transform = AffineTransform.getTranslateInstance(dx, dy);
        return transform.createTransformedShape(shape);
    }
    
    /**
     * 建立預設形狀（當 SVG 載入失敗時使用）
     */
    private static Shape createDefaultShape() {
        return new Rectangle2D.Double(0, 0, 50, 50);
    }
}