package mb.audio.mpd.client;

import com.beust.jcommander.Parameter;

public class Args {
    
    @Parameter(names = "--host", required = true)
    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
