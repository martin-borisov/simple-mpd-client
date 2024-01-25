package mb.audio.mpd.client.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import javax.swing.JButton;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class SwingGraphicsUtils {
    
    /**
     * Calculates the largest size of the given font for which the given string will
     * fit into the given size. This method uses the default toolkit.
     */
    public static int getMaxFittingFontSize(Graphics g, Font font, String string, Dimension size) {
        return getMaxFittingFontSize(g, font, string, size.width, size.height);
    }
    
    /**
     * Calculates the largest size of the given font for which the given string will
     * fit into the given size.
     */
    public static int getMaxFittingFontSize(Graphics g, Font font, String string, int width, int height) {
        int minSize = 0;
        int maxSize = 288;
        int curSize = font.getSize();

        while (maxSize - minSize > 2) {
            FontMetrics fm = g.getFontMetrics(new Font(font.getName(), font.getStyle(), curSize));
            int fontWidth = fm.stringWidth(string);
            int fontHeight = fm.getLeading() + fm.getMaxAscent() + fm.getMaxDescent();

            if ((fontWidth > width) || (fontHeight > height)) {
                maxSize = curSize;
                curSize = (maxSize + minSize) / 2;
            } else {
                minSize = curSize;
                curSize = (minSize + maxSize) / 2;
            }
        }

        return curSize;
    }

    /**
     * Calculates the largest size of the given font for which the given string will
     * fit into the given size.
     */
    public static int getMaxFittingFontSize(Graphics g, Font font, String string, int height) {
        int minSize = 0;
        int maxSize = 288;
        int curSize = font.getSize();

        while (maxSize - minSize > 2) {
            FontMetrics fm = g.getFontMetrics(new Font(font.getName(), font.getStyle(), curSize));
            int fontHeight = fm.getLeading() + fm.getMaxAscent() + fm.getMaxDescent();

            if (fontHeight > height) {
                maxSize = curSize;
                curSize = (maxSize + minSize) / 2;
            } else {
                minSize = curSize;
                curSize = (minSize + maxSize) / 2;
            }
        }

        return curSize;
    }
    
    /**
     * Creates a label that resizes based on its content
     */
    public static JLabel createResizableLabel() {
        JLabel label = new JLabel();
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setMinimumSize(new Dimension(10, 10)); // This is needed for proper resizing
        label.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resizeLabel(label);
            }
        });
        return label;
    }
    
    /**
     * Resizes a label based on its content
     */
    public static void resizeLabel(JLabel label) {
        int maxFittingFontSize = SwingGraphicsUtils.getMaxFittingFontSize(label.getGraphics(),
                label.getFont(), label.getText(), (int) (label.getHeight() / 3));
        label.setFont(new Font(label.getFont().getName(), label.getFont().getStyle(), maxFittingFontSize));
    }
    
    /**
     * Makes passed JButton holdable
     */
    public static void makeButtonHoldable(JButton button, Runnable func) {
        button.addMouseListener(new MouseAdapter() {
            private final static int INIT_WAIT_TIME_MS = 250;
            private final static int SECONDARY_WAIT_TIME_MS = 100;
            private final static int TIME_SWITCH_LIMIT_MS = 3000;
            private boolean pressed;
            private long counter;

            @Override
            public void mousePressed(MouseEvent event) {
                pressed = true;
                new Thread(() -> {
                    while (pressed) {
                        try {
                            Thread.sleep(counter < TIME_SWITCH_LIMIT_MS ?  
                                    INIT_WAIT_TIME_MS : SECONDARY_WAIT_TIME_MS);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        SwingUtilities.invokeLater(func);
                        counter += INIT_WAIT_TIME_MS;
                    }
                }).start();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                pressed = false;
                counter = 0;
            }
        });
    }
    
    /**
     * Converts time in seconds to formatted time string
     */
    public static String secondsToTimeText(long seconds) {
        long hrs = (seconds / 60) / 60;
        long min = (seconds / 60) % 60;
        long sec = seconds % 60;
        return MessageFormat.format("{0}{1}:{2}{3}:{4}{5}", 
                hrs < 10 ? "0" : "", hrs, 
                min < 10 ? "0" : "", min, 
                sec < 10 ? "0" : "", sec);
    }
} 
