package edu.gatech.cc.ubicomp.synclimbglass;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.gatech.cc.ubicomp.synclib.CONSTANTS;
import edu.gatech.cc.ubicomp.synclib.ConnectServer;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;

    private CardScrollView mCardScroller;

    private int view_size = 4;
    private View[] mView = new View[view_size];


    private static final String TAG = "GlassMainActivity";

    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;
    private long lastSensorTime = 0l;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private BlockingQueue bufferQueue;
    ByteBuffer msgBuffer;
    private Boolean msgSending = false;
    private Boolean msgWriting = false;
    ConnectServer sendToServer;

    private int sensor_interval = CONSTANTS.getSensorInterval();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.getByteSize());
        bufferQueue = new ArrayBlockingQueue(CONSTANTS.getQueueSize());

//        mView[0] = buildView(0);
//        mView[1] = buildView(1);
//        mView[2] = buildView(2);
//        mView[3] = buildView(3);

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {

                return view_size;
            }

            @Override
            public Object getItem(int position) {
//                mView = buildView(position);
                return buildView(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
//                mView = buildView(position);
                return buildView(position);
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                Log.d(TAG, "id: " + id);
                if(id==0){
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                }else if(id == 1){
                    if(!msgSending){
                        msgSending = true;

                        sendToServer = new ConnectServer(bufferQueue, true);
                        sendToServer.setWriting("testing");
                        sendToServer.execute();

                        msgWriting = true;
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.SUCCESS);

                    }else if(!msgWriting){
                        sendToServer.setWriting("testing");

                        msgWriting = true;
                    }else if(msgWriting){
                        msgWriting = false;
                        msgSending = false;
                        sendToServer.disconnect();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.DISALLOWED);
                    }

                }else if(id == 2){
                    if(!msgSending){
                        msgSending = true;

                        sendToServer = new ConnectServer(bufferQueue);
                        sendToServer.setIsSending(true);
                        sendToServer.execute();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.SUCCESS);
                    }else{
                        msgWriting = false;
                        msgSending = false;
                        sendToServer.disconnect();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.DISALLOWED);
                    }

                }else if(id == 3){
                    finish();
                }else{
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.DISALLOWED);

                }
                mCardScroller.getAdapter().notifyDataSetChanged();

            }
        });
        setContentView(mCardScroller);


        // Start from here: SYNC
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
//                SensorManager.SENSOR_DELAY_FASTEST);


        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        //sensorManager.unregisterListener(this);
//        CONSTANTS.isSending = false;
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        sensorManager.unregisterListener(this);
        msgSending = false;
        sendToServer.disconnect();

//        CONSTANTS.isSending = false;
        wakeLock.release();
        super.onDestroy();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView(int i) {

        if(i == 1){
            return buildWritingView();
        }
        else if(i == 2){
            return buildUDPView();
        }else if(i == 3){
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

            card.setText("Tap to close");
            return card.getView();
        }else{
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

            card.setText("Return to Home");
            return card.getView();
        }

    }

    private View buildUDPView() {

        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        if(!msgSending){
            card.setText("Tap to start UDP");
        }else{
            card.setText("Sending Now... \n Tap to Stop \n"+ Utils.getIPAddress(true));
        }
        return card.getView();
    }
    private View buildWritingView() {

        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        if(!msgWriting){
            card.setText("Tap to write log");
        }else{
            card.setText("Writing Now... \n Tap to Stop");
        }
        return card.getView();
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        // set reference times
        if(sensorTimeReference == 0l && myTimeReference == 0l) {
            sensorTimeReference = event.timestamp;
            myTimeReference = System.currentTimeMillis();
        }
        // set event timestamp to current time in milliseconds
        event.timestamp = myTimeReference +
                Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
//        Log.d("time", event.timestamp + "|" + System.currentTimeMillis());
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if(msgSending && ( (event.timestamp - lastSensorTime) > sensor_interval)){
//                Log.d("sensorUpdated", ""+event.timestamp + "|" +lastSensorTime +"="+(event.timestamp - lastSensorTime));
                lastSensorTime = event.timestamp;

                msgBuffer.putFloat(0, event.values[0]);
                msgBuffer.putFloat(4, event.values[1]);
                msgBuffer.putFloat(8, event.values[2]);
                msgBuffer.putLong(40, event.timestamp);

                try {
                    if(bufferQueue.remainingCapacity() < 1){
                        bufferQueue.take();
                    }
                    bufferQueue.put(msgBuffer);
                } catch (InterruptedException ex) {
                    Log.d("sensorUpdated", "Error on put");
                }

            }

        }else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            msgBuffer.putFloat(12, event.values[0]);
            msgBuffer.putFloat(16, event.values[1]);
            msgBuffer.putFloat(20, event.values[2]);


        }else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            msgBuffer.putFloat(24, event.values[0]);
            msgBuffer.putFloat(28, event.values[1]);
            msgBuffer.putFloat(32, event.values[2]);
            msgBuffer.putFloat(36, event.values[3]);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
