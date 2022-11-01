package mb.audio.mpd.client;

import org.bff.javampd.monitor.StandAloneMonitor;
import org.bff.javampd.server.MPD;

import com.beust.jcommander.JCommander;

import mb.audio.mpd.client.impl.ConsoleInfoViewer;

public class MpdClient {
    
    public static void main(String[] args) {
        
        // Parse command
        Args arg = new Args();
        JCommander.newBuilder().addObject(arg).build().parse(args);
        
        // Create client
        MpdClient client = new MpdClient(arg.getHost());
        client.setViewer(new ConsoleInfoViewer());
    }
    
    private MPD mpd;
    private InfoViewer viewer;

    public MpdClient(String host) {
        this(host, 6600, null);
    }

    public MpdClient(String host, int port, String password) {
        mpd = MPD.builder().server(host).port(port).password(password).build();
        viewer = new InfoViewer() {};
        setupListeners();
    }

    public void setViewer(InfoViewer viewer) {
        this.viewer = viewer;
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
                mpd.getPlayer().getCurrentSong().ifPresent((song) -> {
                    viewer.playbackStarted(song);
                });
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
        });
        monitor.addTrackPositionChangeListener((event) -> {
            viewer.timeElapsed(event.getElapsedTime());
        });
        monitor.start();
    }

}
