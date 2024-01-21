package mb.audio.mpd.client.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

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
}
