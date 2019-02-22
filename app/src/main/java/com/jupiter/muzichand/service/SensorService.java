package com.jupiter.muzichand.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jupiter.muzichand.MainActivity;

public class SensorService extends Service implements SensorEventListener {

    private static final String Tag = MainActivity.class.getSimpleName();
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;




    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI, new Handler());
        return START_STICKY;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public class SensorBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    public class LocalBinder extends Binder {

        public SensorService getService(){
            return SensorService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }

    }

    private void getAccelerometer(SensorEvent event) {

        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];



        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        Log.i(Tag , "Values :" + x + " " + y + " " + z + " " + accelationSquareRoot);

        if (accelationSquareRoot >= 5) {
            Log.e(Tag, "oui");
            sendMessage();
        }


    }

    private void sendMessage() {
        Log.e(Tag, "Broadcasting message");
        Intent intent = new Intent("my-event");
        intent.putExtra("message", "playNext");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}