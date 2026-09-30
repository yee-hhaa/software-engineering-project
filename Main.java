/**
 * Main.java - 程式進入點
 * 這是整個應用程式的起始類別
 */
public class Main {
    /**
     * 程式的主要方法
     * @param args 命令列參數（未使用）
     */
    public static void main(String[] args) {
        // 使用 SwingUtilities.invokeLater 確保GUI元件的安全更新與正確操作
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // 啟動歡迎畫面
                new WelcomeScreen();
            }
        });
    }
}
