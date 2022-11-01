package mb.audio.mpd.client;

import org.bff.javampd.playlist.MPDPlaylistSong;

public abstract class InfoViewer {
    
    public void playbackStarted(MPDPlaylistSong song) {
    }
    
    public void playbackPaused() {
    }
    
    public void playbackStopped() {
    }
    
    public void timeElapsed(long elapsedTime) {
    }
}
