package mb.audio.mpd.client.impl;

import static java.text.MessageFormat.format;

import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.bff.javampd.playlist.MPDPlaylistSong;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.swing.FontIcon;

import mb.audio.mpd.client.InfoViewer;
import net.miginfocom.swing.MigLayout;

public class SwingInfoViewer extends InfoViewer {
    
    private static final int BIG_ICON_RATIO = 10;
    private static final int BIG_LABEL_RATIO = 22;
    private static final int SMALL_LABEL_RATIO = 30;
    
    private JFrame frame;
    private JLabel stateLabel;
    private JLabel titleLabel;
    private JLabel artistLabel;
    
    public SwingInfoViewer() {
        setupAndShow();
    }

    @Override
    public void playbackStarted() {
        stateLabel.setIcon(FontIcon.of(FontAwesomeRegular.PLAY_CIRCLE, frame.getWidth() / BIG_ICON_RATIO));
    }

    @Override
    public void playbackPaused() {
        stateLabel.setIcon(FontIcon.of(FontAwesomeRegular.PAUSE_CIRCLE, frame.getWidth() / BIG_ICON_RATIO));
    }

    @Override
    public void playbackStopped() {
        stateLabel.setIcon(FontIcon.of(FontAwesomeRegular.STOP_CIRCLE, frame.getWidth() / BIG_ICON_RATIO));
    }
    
    @Override
    public void songChanged(MPDPlaylistSong song) {
        titleLabel.setText(song.getTitle());
        
        if(song.getArtistName() != null || song.getAlbumArtist() != null) {
            artistLabel.setText(format("By ''{0}''", 
                    song.getArtistName() != null ? song.getArtistName() : song.getAlbumArtist()));
        }
    }

    @Override
    public void timeElapsed(long elapsedTime) {
    }
    
    private void setupAndShow() {
        
        // TODO Set initial state
        FontIcon icon = FontIcon.of(FontAwesomeRegular.PLAY_CIRCLE);
        
        // Create and layout controls
        stateLabel = new JLabel(icon);
        
        titleLabel = new JLabel();
        titleLabel.setFont(new Font("Arial Bold", Font.PLAIN, BIG_LABEL_RATIO));
        
        artistLabel = new JLabel();
        artistLabel.setFont(new Font("Arial Bold", Font.PLAIN, SMALL_LABEL_RATIO));
        
        frame = new JFrame("MPD Viewer");
        frame.setLayout(new MigLayout("fill, wrap", "[][grow, left]", "[][]"));
        frame.add(stateLabel, "spany 2");
        frame.add(titleLabel, "aligny bottom");
        frame.add(artistLabel, "aligny top");
        
        // Setup listeners
        setupListeners();
        
        // Show
        frame.setSize(640, 480);
        frame.setVisible(true);
    }
    
    private void setupListeners() {
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                stateLabel.setIcon(FontIcon.of(((FontIcon) stateLabel.getIcon()).getIkon(), frame.getWidth() / BIG_ICON_RATIO));
                titleLabel.setFont(titleLabel.getFont().deriveFont((float) (frame.getWidth() / BIG_LABEL_RATIO)));
                artistLabel.setFont(artistLabel.getFont().deriveFont((float) (frame.getWidth() / SMALL_LABEL_RATIO)));
            }
        });
    }
}
