package com.jupiter.muzichand;

public class Song {

    private long id;
    private String artist;
    private String title;

    public Song(long m_id , String m_title , String m_artist ) {
        id = m_id;
        title = m_title;
        artist = m_artist;
    }

    public long getID(){
        return id;
    }
    public String getArtist(){
        return artist;
    }
    public String getTitle(){
        return title;
    }





}
