package mb.audio.mpd.client.impl;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import org.bff.javampd.playlist.MPDPlaylistSong;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.swing.FontIcon;

import mb.audio.mpd.client.InfoViewer;

public class SwingInfoViewer extends InfoViewer {
    
    private static final Logger LOG = Logger.getLogger(SwingInfoViewer.class.getName());
    private static final int SMALL_ICON_RATIO = 9;
    
    private JFrame frame;
    private Canvas imageCanvas;
    private JTextPane textPane;
    private String musicPath;
    private BufferedImage artwork;
    private FontIcon icon;
    
    public SwingInfoViewer() {
        setupAndShow();
    }
    
    @Override
    public void setMusicPath(String path) {
        this.musicPath = path;
    }

    @Override
    public void playbackStarted() {
        icon = FontIcon.of(FontAwesomeRegular.PLAY_CIRCLE);
        updateIcon();
    }

    @Override
    public void playbackPaused() {
        icon = FontIcon.of(FontAwesomeRegular.PAUSE_CIRCLE);
        updateIcon();
    }

    @Override
    public void playbackStopped() {
        icon = FontIcon.of(FontAwesomeRegular.STOP_CIRCLE);
        updateIcon();
    }
    
    @Override
    public void songChanged(MPDPlaylistSong song) {
        String artist = "";
        if(song.getArtistName() != null || song.getAlbumArtist() != null) {
            artist = song.getArtistName() != null ? song.getArtistName() : song.getAlbumArtist();
        }
        updateText(song.getTitle(), artist);
        
        artwork = fetchArtwork(song);
        updateImage();
    }

    @Override
    public void timeElapsed(long elapsedTime) {
    }
    
    private void setupAndShow() {
        
        // Create and layout controls
        frame = new JFrame("MPD Viewer");
        frame.setLayout(new GridLayout(1, 2));
        
        icon = FontIcon.of(FontAwesomeRegular.PLAY_CIRCLE);
        
        imageCanvas = new Canvas() {
            private static final long serialVersionUID = 1L;
            
            // NB: Called on repaint
            public void paint(Graphics g) {
                if(artwork != null) {
                    
                    int sizeFactor = artwork.getHeight() / artwork.getWidth();
                    if(getHeight() > getWidth()) {
                        int scaledHeight = getWidth() * sizeFactor;
                        g.drawImage(artwork, 0, getHeight() / 2 - scaledHeight / 2, getWidth(), scaledHeight, null);
                    } else {
                        int scaledWidth = getHeight() * sizeFactor;
                        g.drawImage(artwork, getWidth() / 2 - scaledWidth / 2, 0, scaledWidth, getHeight(), null);
                    }
                } else {
                    g.clearRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        frame.add(imageCanvas);
        
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.setBackground(UIManager.getColor("Panel.background"));
        textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add some padding
        textPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        JPanel textPanePanel = new JPanel();
        textPanePanel.setLayout(new BoxLayout(textPanePanel, BoxLayout.PAGE_AXIS));
        textPanePanel.add(Box.createVerticalGlue());
        textPanePanel.add(textPane);
        textPanePanel.add(Box.createVerticalGlue());
        frame.add(textPanePanel);
        
        // Setup listeners
        setupListeners();
        
        // Show
        frame.setSize(640, 480);
        frame.setVisible(true);
    }
    
    private void setupListeners() {
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                updateIcon();
            }
        });
    }
    
    private void updateIcon() {
        icon.setIconSize(frame.getWidth() / SMALL_ICON_RATIO);
        //titleLabel.setIcon(icon);
    }
    
    private void updateImage() {
        imageCanvas.repaint();
    }
    
    private void updateText(String title, String artist) {
        String text = MessageFormat.format("<html><body style='text-align:center; margin: 0 auto;'>"
                + "<p style='top: 50%; left: 50%;'>"
                + "<h1>{0}</h1>"
                + "<h2>By ''{1}''<h2>"
                + "</p>"
                + "</body></html>", title, artist);
        textPane.setText(text);
    }
    
    private BufferedImage fetchArtwork(MPDPlaylistSong song) {
        
        String path = musicPath + "/" + song.getFile();
        path = path.substring(0, path.lastIndexOf('/') + 1); 
        
        BufferedImage image = null;
        try {
            InputStream is = ArtworkRetriever.fetchArtwork(path);
            image = ImageIO.read(is);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return image;
    }
}
