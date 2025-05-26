package mb.audio.mpd.client.impl;

import java.awt.Color;
import java.awt.Image;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.Icon;

import org.bff.javampd.database.MusicDatabase;
import org.bff.javampd.song.MPDSong;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.audio.mpd.client.impl.AlbumListModel.Album;

public class AlbumListModel extends AbstractListModel<Album> {
    private static final Logger LOG = Logger.getLogger(AlbumListModel.class.getName());
    private static final long serialVersionUID = 1L;
    
    // Default configuration
    public static final boolean LOAD_ALBUM_ART = true;
    public static final int ALBUM_ICON_SIZE = 50;
    public static final int ALBUM_ICON_SCALING_ALG = Image.SCALE_SMOOTH;
    
    public static final Icon DEFAULT_ALBUM_ICON = 
            FontIcon.of(FontAwesomeSolid.QUESTION, ALBUM_ICON_SIZE, Color.GRAY);
    
    private MusicDatabase mdb;
    private String musicPath;
    private List<Album> albumCache, filteredAlbumsList;
    private BlockingQueue<Album> albumBlockingQueue;
    
    public AlbumListModel() {
        albumBlockingQueue = new LinkedBlockingQueue<Album>();
        startAlbumMetadataRetrivalThread();
    }

    @Override
    public int getSize() {
        int size = 0;
        if(filteredAlbumsList != null) {
            size = filteredAlbumsList.size();
        }
        return size;
    }

    @Override
    public Album getElementAt(int index) {
        
        // NB: getSize() is always called first so this is safe
        return filteredAlbumsList.get(index);
        
        /*
        Album album = filteredAlbumsList.get(index);
        if(!album.isLoaded() && !albumBlockingQueue.contains(album)) {
            
            // Queue album for artwork fetch to be done by a worker
            albumBlockingQueue.offer(album);
        }
        return album;
        */
    }
    
    public void setMusicDatabase(MusicDatabase mdb) {
        this.mdb = mdb;
        initAlbumList();
        fireIntervalAdded(this, 0, getSize());
    }
    
    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
        fireIntervalAdded(this, 0, getSize());
    }
    
    public void filter(String genre) {
        if(genre == null || "".equals(genre)) {
            filteredAlbumsList = albumCache;
        } else {
            filteredAlbumsList = albumCache.stream().filter(a -> genre.equalsIgnoreCase(a.getGenre()))
                    .collect(Collectors.toList());
        }
        fireContentsChanged(this, 0, albumCache.size());
    }
    
    private void initAlbumList() {
        if (mdb != null) {
            
            // Filter out zero length album names
            albumCache = mdb.getAlbumDatabase().listAllAlbumNames().stream().filter(string -> string.length() > 0)
                    .map(string -> new Album(string, DEFAULT_ALBUM_ICON)).collect(Collectors.toList());
            filteredAlbumsList = albumCache;
            
            // Queue all albums for data retrieval
            albumBlockingQueue.addAll(albumCache);
        }
    }

    private Icon fetchAlbumArt(String albumName) {
        AlbumIcon icon = null;
        if (mdb != null && musicPath != null && LOAD_ALBUM_ART) {
            
            // Escape album name to prevent API errors
            albumName = albumName.replace("\"", "");
            
            // Fetch first album song and derive path
            MPDSong firstAlbumSong = mdb.getSongDatabase().findAlbum(albumName)
                    .stream().findFirst().orElse(null);
            if (firstAlbumSong != null) {
                icon = new AlbumIcon(SwingControlSurface.buildArtworkPath(
                        musicPath, firstAlbumSong), DEFAULT_ALBUM_ICON);
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
                    album.setLoaded(true);
                    
                    LOG.log(Level.FINE, "Trying to refresh list item ''{0}''", album.getName());
                    // int idx = albumNamesCache.indexOf(album.getName());
                    int idx = albumCache.indexOf(album);
                    if(idx > -1) {
                        LOG.log(Level.FINE, "List item ''{0}'' found in album names cache with index {1}", 
                                new Object[] {album.getName(), idx});
                        fireContentsChanged(this, idx, idx);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Exception while fetching album metadata", e);
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
        private boolean loaded;
        
        public Album(String name) {
            this(name, null);
        }

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

        public boolean isLoaded() {
            return loaded;
        }

        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }
    }
}
