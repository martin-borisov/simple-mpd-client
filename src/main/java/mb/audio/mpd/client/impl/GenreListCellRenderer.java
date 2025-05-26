package mb.audio.mpd.client.impl;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.bff.javampd.genre.MPDGenre;

public class GenreListCellRenderer extends JLabel implements ListCellRenderer<MPDGenre> {
    private static final long serialVersionUID = 1L;
    public static final String EMPTY_VALUE = "All Genres";
    
    public GenreListCellRenderer() {
        setOpaque(true);
        
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MPDGenre> list, MPDGenre value, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        setText(value != null ? 
                (value.getName().isEmpty() ?  EMPTY_VALUE : value.getName()) : null);
        
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
