package mb.audio.mpd.client;

import static java.text.MessageFormat.format;

import javax.swing.UIManager;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.monitor.StandAloneMonitor;
import org.bff.javampd.player.Player;
import org.bff.javampd.player.Player.Status;
import org.bff.javampd.playlist.Playlist;
import org.bff.javampd.server.MPD;

import com.beust.jcommander.JCommander;
import com.formdev.flatlaf.FlatDarkLaf;

import mb.audio.mpd.client.impl.SwingInfoViewer;

public class MpdClient {
    
    private static final int DEFAULT_PORT = 6600;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    
    public static void main(String[] args) {
        
        // Parse command
        Args arg = new Args();
        JCommander.newBuilder().addObject(arg).build().parse(args);
        
        // Custom LaF
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 999);
        
        // Create client
        MpdClient client;
        int port = arg.getPort() > 0 ? arg.getPort() : DEFAULT_PORT;
        if(arg.getPassword() != null) {
            client = new MpdClient(arg.getHost(), port, arg.getPassword(), arg.getMusicPath());
        } else {
            client = new MpdClient(arg.getHost(), port, arg.getMusicPath());
        }
        
        InfoViewer viewer = null;
        if(arg.getViewer() != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<InfoViewer> viewerClass = (Class<InfoViewer>) Class.forName(arg.getViewer());
                viewer = viewerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(format("Loading viewer class ''{0}'' failed", arg.getViewer()));
            }  
        } else {
            viewer = new SwingInfoViewer();
        }
        client.setViewer(viewer);
    }
    
    private MPD mpd;
    private InfoViewer viewer;
    private String musicPath;
    private Player player;
    private MusicDatabase musicDatabase;
    private Playlist playlist;
    
    public MpdClient(String host, int port) {
        this(host, port, null, null);
    }

    public MpdClient(String host, int port, String musicPath) {
        this(host, port, null, musicPath);
    }

    public MpdClient(String host, int port, String password, String musicPath) {
        mpd = MPD.builder().server(host).port(port).password(password).timeout(CONNECTION_TIMEOUT_MS).build();
        if(!mpd.isConnected()) {
            throw new RuntimeException("Failed to connect to MPD server");
        }

        this.musicPath = musicPath;
        this.player = mpd.getPlayer();
        this.musicDatabase = mpd.getMusicDatabase();
        this.playlist = mpd.getPlaylist();
        
        viewer = new InfoViewer() {};
        setupListeners();
        initState();
    }

    public void setViewer(InfoViewer viewer) {
        this.viewer = viewer;
        initState();
    }
    
    public void close() {
        mpd.getStandAloneMonitor().stop();
    }
    
    private void setupListeners() {
        StandAloneMonitor monitor = mpd.getStandAloneMonitor();
        monitor.addPlayerChangeListener((event) -> {
            
            switch (event.getStatus()) {
            case PLAYER_STARTED:
            case PLAYER_UNPAUSED:
                viewer.playbackStarted();
                break;
                
            case PLAYER_PAUSED:
                viewer.playbackPaused();
                break;
                
            case PLAYER_STOPPED:
                viewer.playbackStopped();
                break;

            default:
                break;
            }
        });
        monitor.addPlaylistChangeListener((event) -> {
            switch (event.getEvent()) {
            case SONG_CHANGED:
                mpd.getPlayer().getCurrentSong().ifPresent((song) -> {
                    viewer.songChanged(song);
                });
                break;

            default:
                break;
            }
        });
        monitor.addTrackPositionChangeListener((event) -> {
            viewer.timeElapsed(event.getElapsedTime());
        });
        monitor.start();
    }
    
    private void initState() {
        
        // Inject properties
        viewer.setMusicPath(musicPath);
        viewer.setPlayer(player);
        viewer.setMusicDatabase(musicDatabase);
        viewer.setPlaylist(playlist);
        
        // Set song
        if(mpd.getPlaylist().getCurrentSong() != null) {
            viewer.songChanged(mpd.getPlaylist().getCurrentSong());
        }
        
        // Set playback status
        Status status = mpd.getPlayer().getStatus();
        switch (status) {
        case STATUS_PLAYING:
            viewer.playbackStarted();
            break;
            
        case STATUS_PAUSED:
            viewer.playbackPaused();
            break;
            
        case STATUS_STOPPED:
            viewer.playbackStopped();
            break;

        default:
            break;
        }
    }
}
