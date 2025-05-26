package mb.audio.mpd.client;

public class GlobalConfig {
    
    private static boolean touchscreen;
    private static boolean highcontrast;
    
    public static void setTouchscreen(boolean touchscreen) {
        GlobalConfig.touchscreen = touchscreen;
    }

    public static boolean isTouchscreen() {
        String os = System.getProperty("os.name");
        return touchscreen && os != null && os.toLowerCase().contains("linux");
    }

    public static boolean isHighcontrast() {
        return highcontrast;
    }

    public static void setHighcontrast(boolean highcontrast) {
        GlobalConfig.highcontrast = highcontrast;
    }
}
