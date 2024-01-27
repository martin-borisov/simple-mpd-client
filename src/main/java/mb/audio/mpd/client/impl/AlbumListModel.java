package mb.audio.mpd.client.impl;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.song.MPDSong;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.audio.mpd.client.impl.AlbumListModel.Album;

public class AlbumListModel extends AbstractListModel<Album> {
    private static final Logger LOG = Logger.getLogger(AlbumListModel.class.getName());
    private static final long serialVersionUID = 1L;
    public static final int ALBUM_ICON_SIZE = 100;
    public static final Icon DEFAULT_ALBUM_ICON = 
            FontIcon.of(FontAwesomeSolid.QUESTION, ALBUM_ICON_SIZE, Color.GRAY);
    
    private MusicDatabase mdb;
    private String musicPath;
    private List<String> albumNamesCache;
    private Map<Integer, Album> albumsCache;
    private BlockingQueue<Album> albumBlockingQueue;
    
    public AlbumListModel() {
        albumsCache = Collections.synchronizedMap(new HashMap<Integer, Album>());
        albumBlockingQueue = new LinkedBlockingQueue<Album>();
        startAlbumMetadataRetrivalThread();
    }

    @Override
    public int getSize() {
        int size = 0;
        if(albumNamesCache != null) {
            size = albumNamesCache.size();
        } else if(mdb != null) {
            
            // Filter out zero length albums
            albumNamesCache = new ArrayList<String>(
                    mdb.getAlbumDatabase().listAllAlbumNames().stream()
                        .filter(string -> string.length() > 0).collect(Collectors.toList()));
            size = albumNamesCache.size();
        }
        return size;
    }

    @Override
    public Album getElementAt(int index) {
        Album album = null;
        if(albumsCache.containsKey(index)) {
            album = albumsCache.get(index);
        } else if(albumNamesCache != null) {
            String albumName = albumNamesCache.get(index);
            album = new Album(albumName, DEFAULT_ALBUM_ICON);
            albumsCache.put(index, album);
            
            // Queue album for artwork fetch to be done by a worker
            albumBlockingQueue.offer(album);
        }
        return album;
    }
    
    public void setMusicDatabase(MusicDatabase mdb) {
        this.mdb = mdb;
        fireIntervalAdded(this, 0, getSize());
    }
    
    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
        fireIntervalAdded(this, 0, getSize());
    }

    private Icon fetchAlbumArt(String albumName) {
        Icon icon = null;
        if (mdb != null && musicPath != null) {
            
            // Escape album name to prevent API errors
            albumName = albumName.replace("\"", "");
            
            // Fetch first album song and derive path
            MPDSong firstAlbumSong = mdb.getSongDatabase().findAlbum(albumName)
                    .stream().findFirst().orElse(null);
            if (firstAlbumSong != null) {
                BufferedImage image = SwingControlSurface.fetchArtwork(musicPath, firstAlbumSong);
                if(image != null) {
                    icon = new ImageIcon(image.getScaledInstance(
                            ALBUM_ICON_SIZE, ALBUM_ICON_SIZE, Image.SCALE_SMOOTH));
                } else {
                    icon = DEFAULT_ALBUM_ICON;
                }
            } else {
                icon = DEFAULT_ALBUM_ICON;
            }
        }
        return icon;
    }
    
    private void startAlbumMetadataRetrivalThread() {
        Thread thread = new Thread(() -> {
            while(true) {
                try {
                    Album album = albumBlockingQueue.take();
                    
                    LOG.log(Level.FINE, "Fetching metadata of album ''{0}''", album.getName());
                    mdb.getAlbumDatabase().findAlbum(album.getName()).stream().findFirst().ifPresent(a -> {
                        album.setArtist(a.getAlbumArtist());
                        a.getDates().stream().findFirst().ifPresent(d -> album.setYear(d));
                        album.setGenre(a.getGenres().stream().collect(Collectors.joining(", ")));
                    });
                    
                    LOG.log(Level.FINE, "Fetching album art of album ''{0}''", album.getName());
                    album.setIcon(fetchAlbumArt(album.getName()));
                    
                    LOG.log(Level.FINE, "Trying to refresh list item ''{0}''", album.getName());
                    int idx = albumNamesCache.indexOf(album.getName());
                    if(idx > -1) {
                        LOG.log(Level.FINE, "List item ''{0}'' found in album names cache with index {1}", 
                                new Object[] {album.getName(), idx});
                        fireContentsChanged(this, idx, idx);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Exception while fetching album ''{0}'' metadata");
                    continue;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    static class Album {
        
        private String name, artist, year, genre;
        private Icon icon;
        
        public Album(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
            this.artist = "";
            this.year = "";
            this.genre = "";
        }

        public Album(String name, String artist, String year, String genre, Icon icon) {
            this(name, icon);
            this.artist = artist;
            this.year = year;
            this.genre = genre;
        }

        public String getName() {
            return name;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }
    }
}
