package mb.audio.mpd.client;

import org.bff.javampd.playlist.MPDPlaylistSong;

public abstract class InfoViewer {
    
    public void setMusicPath(String path) {
    }
    
    public void playbackStarted() {
    }
    
    public void playbackPaused() {
    }
    
    public void playbackStopped() {
    }
    
    public void songChanged(MPDPlaylistSong song) {
    }
    
    public void timeElapsed(long elapsedTime) {
    }
}
