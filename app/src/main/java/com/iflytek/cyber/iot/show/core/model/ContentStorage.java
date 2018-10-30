package com.iflytek.cyber.iot.show.core.model;

public class ContentStorage {

    private static ContentStorage sStorage;
    private Content currentContent;
    private boolean isMusicPlaying = false;

    public static ContentStorage get() {
        if (sStorage == null) {
            sStorage = new ContentStorage();
        }
        return sStorage;
    }

    public void saveContent(Content content) {
        this.currentContent = content;
    }

    public Content getCurrentContent() {
        return currentContent;
    }

    public void setMusicPlaying(boolean musicPlaying) {
        isMusicPlaying = musicPlaying;
    }

    public boolean isMusicPlaying() {
        return isMusicPlaying;
    }
}
