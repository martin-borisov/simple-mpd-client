package mb.audio.mpd.client.impl;

import static java.text.MessageFormat.format;

import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

public class ArtworkRetriever {
    
    private static final Logger LOG = Logger.getLogger(ArtworkRetriever.class.getName());
    
    public static InputStream fetchArtwork(String path) {
        
        try {
            URL url = new URL(path + "cover.jpg");
            LOG.info(format("Trying to fetch cover image at: {0}", url));
            
            if(path.startsWith("file")) {
                
                // Assume this is a local file
                return url.openConnection().getInputStream();
            } else {
                
                // Encode URI path
                String uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), 
                        url.getPort(), url.getPath(), url.getQuery(), url.getRef()).toASCIIString();
                url = new URL(uri);
                
                // Connect
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //con.setAuthenticator(createAuthenticator(...));

                if (con.getResponseCode() == 200) {
                    return con.getInputStream();
                } else {
                    throw new RuntimeException(
                            format("Cover image missing or connection failed with response code {0}",
                                    con.getResponseCode()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching album art failed", e);
        }
    }

    @SuppressWarnings("unused")
    private static Authenticator createAuthenticator(String user, String pwd) {
        return new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pwd.toCharArray());
            }
        };
    }
}

