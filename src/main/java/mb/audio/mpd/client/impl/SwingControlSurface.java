package mb.audio.mpd.client.impl;

import static mb.audio.mpd.client.util.SwingGraphicsUtils.createResizableLabel;
import static mb.audio.mpd.client.util.SwingGraphicsUtils.resizeLabel;

import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.genre.MPDGenre;
import org.bff.javampd.player.Player.Status;
import org.bff.javampd.playlist.MPDPlaylistSong;
import org.bff.javampd.song.MPDSong;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme;

import mb.audio.mpd.client.GlobalConfig;
import mb.audio.mpd.client.MpdControlSurface;
import mb.audio.mpd.client.impl.AlbumListModel.Album;
import mb.audio.mpd.client.util.SwingGraphicsUtils;
import net.miginfocom.swing.MigLayout;

public class SwingControlSurface extends MpdControlSurface {
    
    private static final Logger LOG = Logger.getLogger(SwingControlSurface.class.getName());
    private static final int SMALL_ICON_RATIO = 20;
    private static final String SONG_PANEL_ID = "Playback Panel";
    private static final String LIBRARY_PANEL_ID = "Library Panel";
    private static final String UNKNOWN = "Unknown";
    private static final Color DEFAULT_ICON_COLOR = 
            GlobalConfig.isHighcontrast() ? Color.WHITE : Color.BLACK;
    
    private JFrame frame;
    private Canvas imageCanvas;
    private JLabel titleLabel, artistLabel, albumLabel, elapsedTimeLabel, totalTimeLabel;
    private JSlider timeSlider;
    private JButton playButton, prevButton, nextButton, panelSwitchButton;
    private JList<AlbumListModel.Album> albumList;
    private JComboBox<MPDGenre> genreComboBox;
    private Image artwork;
    private FontIcon icon;
    private Timer playButtonBlinkTimer, timePollTimer;
    private long elapsedTimeCounter;
    
    public SwingControlSurface() {
        
        // Custom LaF
        if(GlobalConfig.isHighcontrast()) {
            FlatHighContrastIJTheme.setup();
            UIManager.put("Component.focusWidth", 0);
        } else {
            FlatDarkLaf.setup();
        }
        UIManager.put("Button.arc", 999);
        
        if(GlobalConfig.isTouchscreen()) {
            UIManager.put("ScrollBar.width", 16);
        }
        
        setupAndShow();
    }
    
    @Override
    public void setMusicPath(String path) {
        super.setMusicPath(path);
        ((AlbumListModel) albumList.getModel()).setMusicPath(path);
    }
    
    @Override
    public void setMusicDatabase(MusicDatabase musicDatabase) {
        super.setMusicDatabase(musicDatabase);
        ((AlbumListModel) albumList.getModel()).setMusicDatabase(musicDatabase);
        
        // Load genres asynchronously
        new SwingWorker<Collection<MPDGenre>, Void>() {
            protected Collection<MPDGenre> doInBackground() throws Exception {
                Collection<MPDGenre> genres = Collections.emptyList();
                if(mdb != null) {
                    genres =  mdb.getGenreDatabase().listAllGenres();
                }
                return genres;
            }
            
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void done() {
                
                Collection<MPDGenre> genres;
                try {
                    genres = get();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error fetching genres from music database", e);
                    return;
                }
                
                // Make sure genres list contains an empty genre, i.e. a substitute for "All Genres"
                MPDGenre emptyGenre = genres.stream()
                        .filter(genre -> "".equals(genre.getName()))
                        .findFirst()
                        .orElseGet(() -> {
                            MPDGenre genre = new MPDGenre("");
                            ((DefaultComboBoxModel) genreComboBox.getModel()).insertElementAt(genres, 0);
                            return genre;
                        });
                ((DefaultComboBoxModel) genreComboBox.getModel()).addAll(genres);
                ((DefaultComboBoxModel) genreComboBox.getModel()).setSelectedItem(emptyGenre);
            }
        }.execute();
    }

    @Override
    public void playbackStarted() {
        icon = FontIcon.of(FontAwesomeSolid.PLAY, Color.GREEN);
        updateIcons();
        startPlayButtonBlink();
        startElapsedTimePoll();
    }

    @Override
    public void playbackPaused() {
        icon = FontIcon.of(FontAwesomeSolid.PLAY, DEFAULT_ICON_COLOR);
        updateIcons();
        stopPlayButtonBlink();
        stopElapsedTimePoll();
    }

    @Override
    public void playbackStopped() {
        icon = FontIcon.of(FontAwesomeSolid.STOP, DEFAULT_ICON_COLOR);
        updateIcons();
        stopPlayButtonBlink();
        stopElapsedTimePoll();
    }
    
    @Override
    public void songChanged(MPDPlaylistSong song) {
        
        // Update song metadata
        String artist = UNKNOWN;
        if(song.getArtistName() != null || song.getAlbumArtist() != null) {
            artist = song.getArtistName() != null ? song.getArtistName() : song.getAlbumArtist();
        }
        updateSongLabels(song.getTitle() != null ? song.getTitle() : UNKNOWN, artist, 
                song.getAlbumName() != null ? song.getAlbumName() : UNKNOWN);
        
        // Update image
        try {
            artwork = ArtworkRetriever.getInstance().fetchArtworkDirect(
                    buildArtworkPath(musicPath, song));
        } catch (ArtworkNotFoundException e) {
            LOG.log(Level.INFO, "Artwork not found at base path ''{0}'' for song ''{1}''", 
                    new Object[] {musicPath, song.getFile()});
        } catch (ArtworkRetrieverException e) {
            LOG.log(Level.WARNING, "Error getting artwork at base path ''{0}'' for song ''{1}''", 
                    new Object[] {musicPath, song.getFile()});
        }
        SwingUtilities.invokeLater(() -> repaintArtworkImage());
        
        // Reset and update song time
        SwingUtilities.invokeLater(() -> {
            elapsedTimeLabel.setText(SwingGraphicsUtils.secondsToTimeText(player.getElapsedTime()));
            timeSlider.setMinimum(0);
            timeSlider.setMaximum(song.getLength());
            timeSlider.setValue((int) player.getElapsedTime());
            totalTimeLabel.setText(SwingGraphicsUtils.secondsToTimeText(song.getLength()));
        });
    }

    @Override
    public void timeElapsed(long elapsedTime) {
        // TODO Use this to show elapsed time
    }
    
    private void setupAndShow() {
        frame = new JFrame("MPD Viewer");
        
        JPanel rootPanel = new JPanel(new MigLayout("fill, wrap", "[fill]", "[fill, 100%][][]"));
        frame.add(rootPanel);
        
        JPanel switchPanel = new JPanel(new CardLayout());
        rootPanel.add(switchPanel);
        
        /* Song panel */
        final JPanel songPanel = new JPanel(new MigLayout(
                "insets 0, fill, wrap", "[fill, 47%][6%, align center][fill, 47%]", "[fill, 100%][][]"));
        switchPanel.add(songPanel, SONG_PANEL_ID);
        
        // Canvas
        // TODO Investigate why some image types are not drawn
        imageCanvas = new Canvas() {
            private static final long serialVersionUID = 1L;
            
            // NB: Called on repaint
            public void paint(Graphics g) {
                if(artwork != null) {
                    
                    int sizeFactor = artwork.getHeight(null) / artwork.getWidth(null);
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
        imageCanvas.setMinimumSize(new Dimension(100, 100)); // This is needed for proper canvas resizing
        songPanel.add(imageCanvas, "spany 3");
        
        // Separator
        songPanel.add(new JSeparator(SwingConstants.VERTICAL), "spany 3");
        
        // Metadata panel
        JPanel metadataPanel = new JPanel(new MigLayout(
                "insets 0, fill, wrap", "[fill]", "[fill, 33%][fill, 33%][fill, 33%]"));
        songPanel.add(metadataPanel);
        
        titleLabel = createResizableLabel();
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        metadataPanel.add(titleLabel);
        
        artistLabel = createResizableLabel();
        artistLabel.setVerticalAlignment(SwingConstants.CENTER);
        metadataPanel.add(artistLabel);
        
        albumLabel = createResizableLabel();
        albumLabel.setVerticalAlignment(SwingConstants.CENTER);
        metadataPanel.add(albumLabel);
        
        /* Library panel */
        JPanel libraryPanel = new JPanel(new MigLayout(
                "insets 0, fill, wrap", 
                GlobalConfig.isTouchscreen() ? "[fill, 90%][fill, 20%]" : "[fill]", "[fill][fill, 100%]"));
        switchPanel.add(libraryPanel, LIBRARY_PANEL_ID);
        
        // Genre panel
        genreComboBox = new JComboBox<>();
        genreComboBox.setRenderer(new GenreListCellRenderer());
        
        JPanel genrePanel = new JPanel(new MigLayout("insets 0, fill", "[fill][fill, 100%]", "[]"));
        genrePanel.add(new JLabel("Genre"));
        genrePanel.add(genreComboBox);
        libraryPanel.add(genrePanel, GlobalConfig.isTouchscreen() ? "spanx" : ""); // NB: Needed for proper layout in fullscreen
        
        // Album list
        albumList = new JList<AlbumListModel.Album>(new AlbumListModel());
        albumList.setCellRenderer(new AlbumListCellRenderer());
        albumList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                playAlbum(albumList.getSelectedValue());
            }
        });
        albumList.setPrototypeCellValue(new Album("This is an album with long title", 
                "Dummy", "2000", "Rock", AlbumListModel.DEFAULT_ALBUM_ICON)); // This provides a huge performance boost in album list loading
        JScrollPane albumListScrollPane = new JScrollPane(albumList);
        libraryPanel.add(albumListScrollPane);
        
        if(GlobalConfig.isTouchscreen()) {
            albumListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            
            JPanel scrollButtonsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
            libraryPanel.add(scrollButtonsPanel);
            
            JButton upButton = SwingGraphicsUtils.createButtonWithResizableIcon(
                    FontIcon.of(FontAwesomeSolid.ARROW_UP, DEFAULT_ICON_COLOR));
            upButton.putClientProperty("JButton.buttonType", "square");
            SwingGraphicsUtils.makeButtonHoldable(upButton, () -> {
                albumList.ensureIndexIsVisible(albumList.getFirstVisibleIndex() - 1);
            });
            scrollButtonsPanel.add(upButton);
            
            JButton downButton = SwingGraphicsUtils.createButtonWithResizableIcon(
                    FontIcon.of(FontAwesomeSolid.ARROW_DOWN, DEFAULT_ICON_COLOR));
            downButton.putClientProperty("JButton.buttonType", "square");
            SwingGraphicsUtils.makeButtonHoldable(downButton, () -> {
                albumList.ensureIndexIsVisible(albumList.getLastVisibleIndex() + 1);
            });
            scrollButtonsPanel.add(downButton);
        }
        
        /* Time panel */
        JPanel timePanel = new JPanel(new MigLayout("insets 0, fill", "[fill][fill, 100%][fill]", "[]"));
        rootPanel.add(timePanel);
        
        elapsedTimeLabel = new JLabel();
        timePanel.add(elapsedTimeLabel);
        
        timeSlider = new JSlider(SwingConstants.HORIZONTAL);
        timePanel.add(timeSlider);
        
        totalTimeLabel = new JLabel();
        timePanel.add(totalTimeLabel);
        
        createTimePollTimer();
        
        /* Buttons panel */
        JPanel buttonsPanel = new JPanel(new MigLayout(
                "insets 0, fill", "[fill, 10%][3%, align center][fill, 29%][fill, 29%][fill, 29%]", "[fill]"));
        rootPanel.add(buttonsPanel);
        
        panelSwitchButton = new JButton(FontIcon.of(FontAwesomeSolid.LIST, DEFAULT_ICON_COLOR));
        panelSwitchButton.addActionListener(e -> {
            panelSwitchButton.setIcon(FontIcon.of(
                    songPanel.isShowing() ? FontAwesomeSolid.ARROW_LEFT : FontAwesomeSolid.LIST, DEFAULT_ICON_COLOR));
            updateIcons();
            ((CardLayout) switchPanel.getLayout()).show(switchPanel, 
                    songPanel.isShowing() ? LIBRARY_PANEL_ID : SONG_PANEL_ID);
        });
        buttonsPanel.add(panelSwitchButton);
        
        buttonsPanel.add(new JSeparator(SwingConstants.VERTICAL));

        prevButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_BACKWARD, DEFAULT_ICON_COLOR));
        prevButton.addActionListener(e -> player.playPrevious());
        buttonsPanel.add(prevButton);
        
        playButton = new JButton(icon = FontIcon.of(FontAwesomeSolid.PLAY, DEFAULT_ICON_COLOR));
        playButton.addActionListener(e -> togglePlayback());
        buttonsPanel.add(playButton);
        createPlayButtonBlinkTimer();
        
        nextButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_FORWARD, DEFAULT_ICON_COLOR));
        nextButton.addActionListener(e -> player.playNext());
        buttonsPanel.add(nextButton);
        
        // Setup listeners
        setupListeners();
        
        // Exit when frame closed
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); 
        
        // Full screen, Linux only
        if(GlobalConfig.isTouchscreen()) {
        
            // Maximize and remove windows frame
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
            frame.setUndecorated(true);
        
            // This is needed on Linux desktops for true full screen while it has no effect on Windows and Mac
            frame.getGraphicsConfiguration().getDevice().setFullScreenWindow(frame);
        }
        
        // Show
        frame.setSize(640, 480);
        frame.setVisible(true);
        
        // NB: This properly resizes the buttons on start,
        // which might be a workaround for a bug in the layouts
        panelSwitchButton.invalidate();
    }
    
    private void setupListeners() {
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                updateIcons();
            }
        });
        genreComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    ((AlbumListModel) albumList.getModel()).filter(((MPDGenre) e.getItem()).getName());
                }
            }
        });
    }
    
    private void updateIcons() {
        
        // Prev
        ((FontIcon) prevButton.getIcon()).setIconSize(frame.getWidth() / SMALL_ICON_RATIO);
        
        // NB: The invalidate call here ensures that the buttons are properly
        // resized when the frame is maximized or normalized. There is a potential bug 
        // which prevents the buttons to be auto-resized when maximizing/normalizing and
        // changing the icon size.
        prevButton.invalidate();
        
        // Play
        icon.setIconSize(frame.getWidth() / SMALL_ICON_RATIO);
        playButton.setIcon(icon);
        
        // Next
        ((FontIcon) nextButton.getIcon()).setIconSize(frame.getWidth() / SMALL_ICON_RATIO);
        
        // Back
        ((FontIcon) panelSwitchButton.getIcon()).setIconSize(frame.getWidth() / SMALL_ICON_RATIO);
    }
    
    private void repaintArtworkImage() {
        if(imageCanvas.isShowing()) { // Don't repaint if currently not showing
            imageCanvas.repaint();
        }
    }
    
    private void updateSongLabels(String title, String artist, String album) {
        titleLabel.setText("'" + title + "'");
        resizeLabel(titleLabel);
        artistLabel.setText("By " + artist);
        resizeLabel(artistLabel);
        albumLabel.setText("Album '" + album + "'");
        resizeLabel(albumLabel);
    }
    
    private void createPlayButtonBlinkTimer() {
        playButtonBlinkTimer = new Timer(100, (event) -> {
            Color col = icon.getIconColor();
            
            if(Color.BLACK.equals(DEFAULT_ICON_COLOR)) {
                col = new Color(col.getRed(), col.getGreen() < 255 ? Math.min(col.getGreen() + 25, 255) : 0,
                        col.getBlue());
            } else if(Color.WHITE.equals(DEFAULT_ICON_COLOR)){

                col = new Color(col.getRed() > 0 ? Math.max(col.getRed() - 25, 0) : 255, 255,
                        col.getBlue() > 0 ? Math.max(col.getBlue() - 25, 0) : 255);
            }
            
            icon.setIconColor(col);
            playButton.repaint();
        });
    }
    
    private void startPlayButtonBlink() {
        playButtonBlinkTimer.restart();
    }
    
    private void stopPlayButtonBlink() {
        playButtonBlinkTimer.stop();
    }
    
    private void createTimePollTimer() {
        timePollTimer = new Timer(1000, (event) -> {
            
            elapsedTimeLabel.setText(SwingGraphicsUtils.secondsToTimeText(elapsedTimeCounter));
            timeSlider.setValue((int) elapsedTimeCounter);
            elapsedTimeCounter++;
            
            // Sync with server every ten seconds
            if(elapsedTimeCounter % 10 == 0) {
                elapsedTimeCounter = player.getElapsedTime();
            }
        });
    }
    
    private void startElapsedTimePoll() {
        elapsedTimeCounter = player.getElapsedTime();
        timePollTimer.restart();
    }
    
    private void stopElapsedTimePoll() {
        timePollTimer.stop();
    }
    
    private void togglePlayback() {
        Status status = player.getStatus();
        switch (status) {
        case STATUS_PAUSED:
        case STATUS_STOPPED:
            player.play();
            break;
            
        case STATUS_PLAYING:
            player.pause();
            break;
        }
    }
    
    private void playAlbum(Album album) {
        playlist.clearPlaylist();
        playlist.insertAlbum(album.getName());
        player.play();
    }
    
    /* Utils */
       
    public static String buildArtworkPath(String musicPath, MPDSong song) {
        String path = musicPath + "/" + song.getFile();
        return path.substring(0, path.lastIndexOf('/') + 1);
    }
}
