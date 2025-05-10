package mb.audio.mpd.client.impl;

import static java.text.MessageFormat.format;

public class ArtworkNotFoundException extends ArtworkRetrieverException {
    private static final long serialVersionUID = 1L;

    public ArtworkNotFoundException(String path) {
        super(format("Artwork not found at ''{0}''", path));
    }
    
    public ArtworkNotFoundException(String path, int responseCode) {
        super(format("Artwork not found at ''{0}''; response code {1}", path, responseCode));
    }
}
