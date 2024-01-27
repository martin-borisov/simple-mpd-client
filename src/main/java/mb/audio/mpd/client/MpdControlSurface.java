package mb.audio.mpd.client;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.player.Player;
import org.bff.javampd.playlist.MPDPlaylistSong;
import org.bff.javampd.playlist.Playlist;

public abstract class MpdControlSurface {
    
    protected String musicPath;
    protected Player player;
    protected MusicDatabase mdb;
    protected Playlist playlist;
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setMusicDatabase(MusicDatabase musicDatabase) {
        this.mdb = musicDatabase;
    }
    
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }
    
    public void setMusicPath(String path) {
        this.musicPath = path;
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
