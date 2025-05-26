package mb.audio.mpd.client.impl;

import java.awt.Component;
import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

public class AlbumIcon implements Icon {
    private static final Logger LOG = Logger.getLogger(AlbumIcon.class.getName());
    private String path;
    private Icon defautIcon;
    private boolean notFound;
    
    public AlbumIcon(String path, Icon defaultIcon) {
        this.path = path;
        this.defautIcon = defaultIcon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        
        // Paint default icon if icon was not found after the first try
        if(notFound) {
            defautIcon.paintIcon(c, g, x, y);
        } else {
            try {
                g.drawImage(ArtworkRetriever.getInstance().fetchArtwork(path), x, y, c);
            } catch (ArtworkNotFoundException e) {
                LOG.log(Level.INFO, "Artwork not found at path ''{0}'''", path);
                if (defautIcon != null) {
                    defautIcon.paintIcon(c, g, x, y);
                }
                notFound = true;
            } catch (ArtworkRetrieverException e) {
                LOG.log(Level.WARNING, "Error getting artwork at path ''{0}''", path);
            }
        }
    }

    @Override
    public int getIconWidth() {
        return ArtworkRetriever.THUMB_SIZE;
    }

    @Override
    public int getIconHeight() {
        return ArtworkRetriever.THUMB_SIZE;
    }

}
