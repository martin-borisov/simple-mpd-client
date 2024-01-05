package mb.audio.mpd.client.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class SwingGraphicsUtils {
    
    /**
     * Calculates the largest size of the given font for which the given string 
     * will fit into the given size. This method uses the default toolkit.
     */
    public static int getMaxFittingFontSize(Graphics g, Font font, String string, Dimension size){
      return getMaxFittingFontSize(g, font, string, size.width, size.height);
    }

    /**
     * Calculates the largest size of the given font for which the given string 
     * will fit into the given size.
     */
    public static int getMaxFittingFontSize(Graphics g, Font font, String string, int width, int height){
      int minSize = 0;
      int maxSize = 288;
      int curSize = font.getSize();

      while (maxSize - minSize > 2){
        FontMetrics fm = g.getFontMetrics(new Font(font.getName(), font.getStyle(), curSize));
        int fontWidth = fm.stringWidth(string);
        int fontHeight = fm.getLeading() + fm.getMaxAscent() + fm.getMaxDescent();

        if ((fontWidth > width) || (fontHeight > height)){
          maxSize = curSize;
          curSize = (maxSize + minSize) / 2;
        }
        else{
          minSize = curSize;
          curSize = (minSize + maxSize) / 2;
        }
      }

      return curSize;
    }
}
