package mb.audio.mpd.client;

import com.beust.jcommander.Parameter;

public class Args {
    
    @Parameter(names = "--host", required = true)
    private String host;
    
    @Parameter(names = "--port")
    private int port;
    
    @Parameter(names = "--password")
    private String password;
    
    @Parameter(names = "--musicpath")
    private String musicPath;
    
    @Parameter(names = "--viewer")
    private String viewer;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public String getViewer() {
        return viewer;
    }

    public void setViewer(String viewer) {
        this.viewer = viewer;
    }
}
