package edu.gatech.cc.ubicomp.simpleseesaw;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.constraint.ConstraintLayout;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.gatech.cc.ubicomp.synclib.CONSTANTS;
import edu.gatech.cc.ubicomp.synclib.ConnectServer;

public class MainActivity extends WearableActivity implements SensorEventListener {


    private SensorManager sensorManager;
    private TextView tv1, tv2;
    Button btn1, btn2, btn3;

    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;
    private long lastSensorTime = 0l;

    private static final String TAG = "WatchMainActivity";

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private BlockingQueue bufferQueue;
    ByteBuffer msgBuffer;
    private Boolean msgSending = false;
    ConnectServer sendToServer;

    TwoFingersDoubleTapDetector twoFingersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
//                1);

        tv1 = (TextView) findViewById(R.id.tv1);
        tv1.setText(CONSTANTS.getIpAddress()+":"+CONSTANTS.getPORT());

        tv2 = (TextView) findViewById(R.id.tv2);
        tv2.setText(Utils.getIPAddress(true));


        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(btn1ClickListener);

        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(btn2ClickListener);

        btn3 = (Button) findViewById(R.id.btn3);
        btn3.setOnClickListener(btn3ClickListener);

        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.getByteSize());
        bufferQueue = new ArrayBlockingQueue(5);

        // Enables Always-on
        setAmbientEnabled();

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();




        //region Exit by two fingers double tap
        twoFingersListener = new TwoFingersDoubleTapDetector() {
            @Override
            public void onTwoFingersDoubleTap() {
                Toast.makeText(getApplicationContext(), "2 FINGERS CLOSE", Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        BoxInsetLayout mContainerView = findViewById(R.id.container);
        mContainerView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                twoFingersListener.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(this);
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

    Button.OnClickListener btn1ClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
//            if (!CONSTANTS.isSending) {
//                CONSTANTS.isSending = true;
//                ConnectServer sendToServer = new ConnectServer(bufferQueue);
//                sendToServer.execute();
//
//                btn1.setTextColor(getColor(R.color.green));
//                btn1.setText("Sending...");
//            }

            if(!msgSending){
                msgSending = true;

                sendToServer = new ConnectServer(bufferQueue);
                sendToServer.setIsSending(true);
                sendToServer.execute();

                btn1.setTextColor(Color.rgb(50,200,50));
                btn1.setText("Sending...");
                tv2.setText(Utils.getIPAddress(true));
            }

        }
    };

    Button.OnClickListener btn2ClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
//            CONSTANTS.isSending = false;
//
//            btn1.setTextColor(getColor(R.color.white));
//            btn1.setText("Connect");

            msgSending = false;
            sendToServer.disconnect();

            btn1.setTextColor(Color.rgb(50,50,50));
            btn1.setText("Connect");
            btn3.setTextColor(Color.rgb(50,50,50));
            btn3.setText("Write FILE");
        }
    };

    Button.OnClickListener btn3ClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            if(!msgSending){
                msgSending = true;

                sendToServer = new ConnectServer(bufferQueue);
                sendToServer.setWriting("testing");
                sendToServer.execute();

                btn3.setTextColor(Color.rgb(50,200,50));
                btn3.setText("Writing...");
            }else if(!sendToServer.getWriting()){
                sendToServer.setWriting("testing");
            }

        }
    };
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


        // TYPE_GYROSCOPE
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if(msgSending && ( (event.timestamp - lastSensorTime) > CONSTANTS.SENSOR_INTERVAL)){
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
//                    Log.d("sensorUpdated", "DATA put "+event.timestamp);
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
