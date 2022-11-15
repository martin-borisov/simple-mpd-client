package mb.audio.mpd.client.impl;

import static java.text.MessageFormat.format;

import org.bff.javampd.playlist.MPDPlaylistSong;

import mb.audio.mpd.client.InfoViewer;

public class ConsoleInfoViewer extends InfoViewer {

    @Override
    public void songChanged(MPDPlaylistSong song) {
        System.out.println(format("Song changed > ''{0}''", song.getTitle()));
    }

    @Override
    public void playbackStarted() {
        System.out.println("Playback started");
    }

    @Override
    public void playbackPaused() {
        System.out.println("Playback paused");
    }

    @Override
    public void playbackStopped() {
        System.out.println("Playback stopped");
    }

    @Override
    public void timeElapsed(long elapsedTime) {
        System.out.println(format("Time elapsed: {0}", elapsedTime));
    }
}
