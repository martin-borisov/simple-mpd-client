package mb.audio.mpd.client.impl;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class AlbumListCellRenderer extends JLabel implements ListCellRenderer<AlbumListModel.Album> {
    private static final long serialVersionUID = 1L;
    
    public AlbumListCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends AlbumListModel.Album> list, AlbumListModel.Album album, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        StringBuilder buf = new StringBuilder();
        buf.append("<html><body>");
        buf.append("<p><b>").append(album.getName()).append("</b></p>");
        if(!album.getArtist().isBlank()) {
            buf.append("<p>").append(album.getArtist()).append("</p>");
        }
        if(!album.getYear().isBlank() || !album.getGenre().isBlank()) {
            buf.append("<p>");
            if(!album.getYear().isBlank()) {
                buf.append(album.getYear());
            }
            if(!album.getGenre().isBlank()) {
                buf.append(" / ").append(album.getGenre());
            }
            buf.append("</p>");
        }
        if(!album.getGenre().isBlank()) {
            buf.append("<p>").append(album.getGenre()).append("</p>");
        }
        buf.append("</body></html>");
        
        setText(buf.toString());
        setIcon(album.getIcon());
        
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}
