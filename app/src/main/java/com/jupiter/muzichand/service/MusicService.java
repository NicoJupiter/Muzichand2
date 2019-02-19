package com.jupiter.muzichand.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import android.content.ContentUris;
import android.os.PowerManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.content.ContentResolver;
import android.database.Cursor;

import com.jupiter.muzichand.MainActivity;
import com.jupiter.muzichand.R;
import com.jupiter.muzichand.Song;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import java.io.IOException;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String CHANNEL_ID = "";
    public MediaPlayer mp;
    private static final String Tag = MainActivity.class.getSimpleName();

    private final IBinder mBinder = new MusicBinder();
    int CurrentPosition;

    private Uri uriMusic;

    //song list
    private ArrayList<Song> songs;

    //current position
    private int songPosn;

    private String songTitle="";
    private static final int NOTIFY_ID=1;

    private boolean shuffle=false;
    private Random rand;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        mp.stop();
        mp.release();
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        mediaPlayer.reset();
        return false;
    }

    public class LocalBinder extends Binder {

        public MusicService getService(){
            Log.i(Tag , "service connected");
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {

        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;

        rand=new Random();

        //create player
        mp = new MediaPlayer();
        initMusicPlayer();
        Log.i(Tag , "service created");

    }

    public void initMusicPlayer(){
        mp.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public int onStartCommand(Intent intent , int flags , int startId) {
        Log.i(Tag , "service start");

        return START_STICKY;
    }

    public void playSong(){
        mp.reset();

        //get song
        Song playSong = songs.get(songPosn);

        //get id
        long currSong = playSong.getID();

        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try{

            mp.setDataSource(getApplicationContext(), trackUri);
            mp.prepareAsync();
            songTitle=playSong.getTitle();
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

    }

    public String getSongTitle()
    {
        return songTitle;
    }

    public int getPosition()
    {
        return songPosn;
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    public int seekBarGetCurrentPosition(){
        if(mp!=null&&mp.isPlaying()){
            CurrentPosition=mp.getCurrentPosition();
        }
        return CurrentPosition;
    }

    public int getPosn(){
        return mp.getCurrentPosition();
    }

    public int getDur(){
        return mp.getDuration();
    }

    public boolean isPng(){
        return mp.isPlaying();
    }

    public void pausePlayer(){
        mp.pause();
    }

    public void seek(int posn){
        mp.seekTo(posn);
    }

    public void go(){
        mp.start();
    }

    public void playPrev()
    {
        songPosn--;
        if(songPosn == 0) {
            songPosn=songs.size()-1;
        }
        playSong();
    }

    public void playNext(){

        if(shuffle){
            int newSong = songPosn;
            while(newSong == songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else{
            songPosn++;
            if(songPosn ==songs.size()) songPosn=0;
        }
        playSong();

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
        mp.stop();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        mediaPlayer.start();

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notIntent = new Intent(this, MainActivity.class);
            notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                    notIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_play)
                    .setContentTitle("Playing")
                    .setContentText(songTitle)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            mBuilder.build();




        }
        else {
            Log.i(Tag , "API inférieur à 16 notification build failed");
        }

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(mediaPlayer.getCurrentPosition() == 0){
            mediaPlayer.reset();
            playNext();
        }
    }



}
