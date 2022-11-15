package mb.audio.mpd.client;

import org.bff.javampd.monitor.StandAloneMonitor;
import org.bff.javampd.player.Player.Status;
import org.bff.javampd.server.MPD;

import com.beust.jcommander.JCommander;

import mb.audio.mpd.client.impl.SwingInfoViewer;

public class MpdClient {
    
    private static final int DEFAULT_PORT = 6600;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    
    public static void main(String[] args) {
        
        // Parse command
        Args arg = new Args();
        JCommander.newBuilder().addObject(arg).build().parse(args);
        
        // Create client
        MpdClient client;
        int port = arg.getPort() > 0 ? arg.getPort() : DEFAULT_PORT;
        if(arg.getPassword() != null) {
            client = new MpdClient(arg.getHost(), port, arg.getPassword());
        } else {
            client = new MpdClient(arg.getHost(), port);
        }
        client.setViewer(new SwingInfoViewer());
    }
    
    private MPD mpd;
    private InfoViewer viewer;

    public MpdClient(String host, int port) {
        this(host, port, null);
    }

    public MpdClient(String host, int port, String password) {
        mpd = MPD.builder().server(host).port(port).password(password).timeout(CONNECTION_TIMEOUT_MS).build();
        if(!mpd.isConnected()) {
            throw new RuntimeException("Failed to connect to MPD server");
        }
        
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
