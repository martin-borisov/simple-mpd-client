package mb.audio.mpd.client;

import static java.text.MessageFormat.format;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.monitor.StandAloneMonitor;
import org.bff.javampd.player.Player;
import org.bff.javampd.player.Player.Status;
import org.bff.javampd.playlist.Playlist;
import org.bff.javampd.server.MPD;

import com.beust.jcommander.JCommander;
import mb.audio.mpd.client.impl.SwingControlSurface;

public class MpdClient {
    
    private static final int DEFAULT_PORT = 6600;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    
    public static void main(String[] args) {
        
        // Parse command
        Args arg = new Args();
        JCommander.newBuilder().addObject(arg).build().parse(args);
        
        // Set global config
        GlobalConfig.setTouchscreen(arg.isTouchscreen());
        
        // Create client
        MpdClient client;
        int port = arg.getPort() > 0 ? arg.getPort() : DEFAULT_PORT;
        if(arg.getPassword() != null) {
            client = new MpdClient(arg.getHost(), port, arg.getPassword(), arg.getMusicPath());
        } else {
            client = new MpdClient(arg.getHost(), port, arg.getMusicPath());
        }
        
        MpdControlSurface surface = null;
        if(arg.getSurface() != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<MpdControlSurface> surfaceClass = (Class<MpdControlSurface>) Class.forName(arg.getSurface());
                surface = surfaceClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(format("Loading surface class ''{0}'' failed", arg.getSurface()));
            }  
        } else {
            surface = new SwingControlSurface();
        }
        client.setSurface(surface);
    }
    
    private MPD mpd;
    private MpdControlSurface surface;
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
        
        surface = new MpdControlSurface() {};
        setupListeners();
        initState();
    }

    public void setSurface(MpdControlSurface surface) {
        this.surface = surface;
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
                surface.playbackStarted();
                break;
                
            case PLAYER_PAUSED:
                surface.playbackPaused();
                break;
                
            case PLAYER_STOPPED:
                surface.playbackStopped();
                break;

            default:
                break;
            }
        });
        monitor.addPlaylistChangeListener((event) -> {
            switch (event.getEvent()) {
            case SONG_CHANGED:
                mpd.getPlayer().getCurrentSong().ifPresent((song) -> {
                    surface.songChanged(song);
                });
                break;

            default:
                break;
            }
        });
        monitor.addTrackPositionChangeListener((event) -> {
            surface.timeElapsed(event.getElapsedTime());
        });
        monitor.start();
    }
    
    private void initState() {
        
        // Inject properties
        surface.setMusicPath(musicPath);
        surface.setPlayer(player);
        surface.setMusicDatabase(musicDatabase);
        surface.setPlaylist(playlist);
        
        // Set song
        if(mpd.getPlaylist().getCurrentSong() != null) {
            surface.songChanged(mpd.getPlaylist().getCurrentSong());
        }
        
        // Set playback status
        Status status = mpd.getPlayer().getStatus();
        switch (status) {
        case STATUS_PLAYING:
            surface.playbackStarted();
            break;
            
        case STATUS_PAUSED:
            surface.playbackPaused();
            break;
            
        case STATUS_STOPPED:
            surface.playbackStopped();
            break;

        default:
            break;
        }
    }
}
