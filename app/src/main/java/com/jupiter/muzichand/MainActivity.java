package com.jupiter.muzichand;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SeekBar;



import java.util.ArrayList;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.ListView;

import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;

import com.jupiter.muzichand.service.MusicService;
import com.jupiter.muzichand.service.SensorService;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl  {

    private static final String Tag = MainActivity.class.getSimpleName();
    private MusicService musicServiceBinder = null;
    private SensorService sensorServiceBinder = null;
    private SeekBar seekBar;
    boolean mBound , mBoundSensor = false;
    public TextView currentSong;
    private ListView songView;
    private ArrayList<Song> songList;
    private Intent playIntent;
    private Intent sensorIntent;
    private MusicController musicController;
    private boolean paused=false, playbackPaused=false;
    public SongAdapter songAdt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        songView = (ListView)findViewById(R.id.song_list);
        currentSong = (TextView)findViewById(R.id.current_song);

        songList = new ArrayList<Song>();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);


            return;
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);


            return;
        }else {


            setController();

            //******START SERVICE
            if(playIntent==null){
                playIntent = new Intent(this, MusicService.class);
                bindService(playIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                startService(playIntent);
            }

           if(sensorIntent==null){
               startService(new Intent(this, SensorService.class));

            }



            getSongList();

            //*******SET ALL SONG ON A LIST
           songAdt = new SongAdapter(this, songList);
            songView.setAdapter(songAdt);



        }


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(paused){
            setController();
            paused=false;
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));
    }

    @Override
    protected void onStop() {
        musicController.hide();
        super.onStop();
    }

    //***********SETTINGS MUSIC CONTROLLER
    private void setController(){
        musicController = new MusicController(this);

        musicController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        musicController.setMediaPlayer(this);
        musicController.setAnchorView(findViewById(R.id.song_list));
        musicController.setEnabled(true);

    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    public void songPicked(View view){


        musicServiceBinder.setSong(Integer.parseInt(view.getTag().toString()));
        musicServiceBinder.playSong();

        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        musicController.show(0);

       // songAdt.changeColorView(view, musicServiceBinder.getPosition());

       setTitleSong();

        Log.e(Tag, Integer.toString(musicServiceBinder.getPosition()));
    }





    private void playNext(){

        musicServiceBinder.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        musicController.show(0);
        setTitleSong();
    }


    private void playPrev(){

        musicServiceBinder.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        musicController.show(0);
        setTitleSong();
    }

    public void setTitleSong()
    {
        currentSong.setText(musicServiceBinder.getSongTitle());
    }

    @Override
    public int getCurrentPosition() {
        if(musicServiceBinder!=null && mBound && musicServiceBinder.isPng())
        return musicServiceBinder.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicServiceBinder!=null && mBound && musicServiceBinder.isPng())
        return musicServiceBinder.getDur();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicServiceBinder!=null && mBound)
        return musicServiceBinder.isPng();
        return false;
    }


    @Override
    public void pause() {
        playbackPaused=true;
        musicServiceBinder.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicServiceBinder.seek(pos);
    }

    @Override
    public void start() {
        musicServiceBinder.go();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }


    //********************SERVICE CONNECTION***********************
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicServiceBinder = binder.getService();
            musicServiceBinder.setList(songList);
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            if(message != null) {
                playNext();
            }
            Log.e("receiver", "Got message: " + message);
        }
    };



    public void getSongList() {

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,           // 0
                MediaStore.Audio.Media.TITLE,         // 2
                MediaStore.Audio.Media.ARTIST,         // 2
               };

        //final String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor == null) {
            // Query failed...
            Log.e(Tag, "Failed to retrieve music: cursor is null :-(");

        }
        else if (!musicCursor.moveToFirst()) {
            Log.e(Tag, "Failed to move cursor to first row (no query results).");

        }else {

            musicCursor.moveToFirst();
                //get columns
                int titleColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
                //add songs to list
                do {
                    Log.i(Tag, "move to next");
                    long thisId = musicCursor.getLong(idColumn);
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    songList.add(new Song(thisId, thisTitle ,thisArtist));
                }
                while (musicCursor.moveToNext());

        }

    }


    //*********Create menu for shuffle an exit button**********
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shiffle_end_menu, menu);
        return true;
    }

    //********Option for menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicServiceBinder.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicServiceBinder=null;
                stopService(sensorIntent);
                sensorServiceBinder=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicServiceBinder=null;
        stopService(sensorIntent);
        sensorServiceBinder=null;
        super.onDestroy();
    }

}
