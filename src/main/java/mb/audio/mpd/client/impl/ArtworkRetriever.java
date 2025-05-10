package mb.audio.mpd.client.impl;

import static java.text.MessageFormat.format;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;

public class ArtworkRetriever {
    private static final Logger LOG = Logger.getLogger(ArtworkRetriever.class.getName());
    private static final String CACHE_DIR = "cache";
    private static final String COVER_FILE_NAME = "cover.jpg";
    
    // TODO Next two properties should be configurable
    public static final int THUMB_SIZE = 50;
    private static final int MEMORY_CACHE_SIZE = 100;
    
    private static ArtworkRetriever ref;
    
    private Cache<Path, BufferedImage> memoryCache;
    
    public static ArtworkRetriever getInstance() {
        synchronized (ArtworkRetriever.class) {
            if(ref == null) {
                ref = new ArtworkRetriever();
            }
        }
        return ref;
    }
    
    private ArtworkRetriever() {
        init();
    }

    private void init() {
        Path path = Paths.get(CACHE_DIR);
        if(!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException("Error creating cache directory", e);
            }
        }
        
        memoryCache = CacheBuilder.newBuilder().maximumSize(MEMORY_CACHE_SIZE).build();
    }
    
    public BufferedImage fetchArtworkDirect(String basePath) 
            throws ArtworkRetrieverException, ArtworkNotFoundException {
        
        URL url;
        try {
            url = new URL(basePath + COVER_FILE_NAME);
        } catch (MalformedURLException e) {
            throw new ArtworkRetrieverException(
                    format("Building image URL failed. Passed base path is ''{0}''.", basePath), e);
        }
        
        InputStream is;
        if (basePath.startsWith("file")) {
            try {
                
                // Assume this is a local file
                is = url.openConnection().getInputStream();
            } catch (IOException e) {
                LOG.log(Level.FINE, "Error getting input stream of local file", e);
                throw new ArtworkNotFoundException(url.toString());
            }
            
        } else {
            try {
                
                // Encode URI path
                String uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                        url.getQuery(), url.getRef()).toASCIIString();
                url = new URL(uri);

                // Connect
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                // con.setAuthenticator(createAuthenticator(...));

                if (con.getResponseCode() == 200) {
                    is = con.getInputStream();
                } else {
                    throw new ArtworkNotFoundException(url.toExternalForm(), con.getResponseCode());
                }
            } catch (URISyntaxException |  IOException e) {
                throw new ArtworkRetrieverException(
                        format("Error getting input stream of remote image file at ''{0}''", url), e);
            }
        }
        
        BufferedImage image;
        try(is) {
            image = ImageIO.read(is);
        } catch (IOException e) {
            throw new ArtworkRetrieverException("Error reading image", e);
        }
        
        return image;
    }
    
    public BufferedImage fetchArtwork(String basePath) 
            throws ArtworkRetrieverException, ArtworkNotFoundException {
        
        URL url;
        try {
            url = new URL(basePath + COVER_FILE_NAME);
        } catch (MalformedURLException e) {
            throw new ArtworkRetrieverException(
                    format("Building image URL failed. Passed base path is ''{0}''.", basePath), e);
        }
        Path cachedPath = Paths.get(CACHE_DIR + "/" + hashUrl(url) + ".jpg");
            
        // Check memory cache
        BufferedImage image = memoryCache.getIfPresent(cachedPath);
        if(image == null) {
            if(Files.exists(cachedPath)) { // Check file cache
                
                // Load image directly
                // TODO Verify size in case something changed outside of the application
                try {
                    image = ImageIO.read(cachedPath.toFile());
                } catch (Exception e) {
                    throw new ArtworkRetrieverException(
                            format("Error loading image from cache at ''{0}''.", cachedPath.toString()), e);
                }
                
                // Add to memory cache
                memoryCache.put(cachedPath, image);
            } else {
                
                // Resize and write to cache
                try {
                    // TODO Anti-aliasing should be configurable
                    image = Thumbnails.of(fetchArtworkDirect(basePath))
                        .size(THUMB_SIZE, THUMB_SIZE)
                        .antialiasing(Antialiasing.ON)
                        .asBufferedImage();
                    Thumbnails.of(image)
                        .size(THUMB_SIZE, THUMB_SIZE)
                        .outputFormat("jpg")
                        .toFile(cachedPath.toString());
                } catch (IOException e) {
                    throw new ArtworkRetrieverException(
                            format("Error saving image to cache at ''{0}''.", cachedPath.toString()), e);
                }
                
                // Add to memory cache
                memoryCache.put(cachedPath, image);
            }
        }

        return image;
    }
    
    private String hashUrl(URL url) {
        return Long.toHexString(url.toString().hashCode());
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

