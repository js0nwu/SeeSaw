package edu.gatech.ubicomp.wearappMovement;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import android.widget.Toast;
import edu.gatech.ubicomp.synchro.detector.*;

import edu.gatech.ubicomp.movement.R;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class MainActivity extends Activity implements SensorEventListener {

    private static final int FLASH_LENGTH = 1000;

    private edu.gatech.ubicomp.wearappMovement.Controller buttonController = new edu.gatech.ubicomp.wearappMovement.Controller("ButtonController");

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private double lastSensorTime = 0;
    private double lastConvertedTS = 0;
	private long lastSync = 0;

    private SynchroDetector synchroDetector = null;

    private DatagramSocket socket = null;
    private DatagramPacket packet = null;

    private final int PACKETSIZE = 36;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
				RelativeLayout screen = (RelativeLayout) findViewById(R.id.screen2);
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Log.d("magnetometer", "magnetometer available in this Android device");
        }
        else {
            Log.d("magnetometer", "no magnetometer in this Android device");
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            Log.d("gyro", "gyroscope available in this Android device");
        }
        else {
            Log.d("gyro", "no gyroscope in this Android device");
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d("accel", "accelerometer available in this Android device");
        }
        else {
            Log.d("accel", "no accelerometer in this Android device");
        }

        synchroDetector = new SynchroDetector(true);
        synchroDetector.setActivationMode(true);
        synchroDetector.setEventRecognitionListener(new EventRecognitionListener() {
            float corrTimestamp = 0;
			float corrValue = 0;
			int corrCounter = 0;
			String direction = "";
			Long timeRecognized = null;
            @Override
            public void onEventRecognized(String result) {
                timeRecognized = System.currentTimeMillis();
				String[] resultSplit = result.split(",");
				corrTimestamp = Float.valueOf(resultSplit[0]);
				corrValue = Float.valueOf(resultSplit[1]);
				corrCounter = Integer.valueOf(resultSplit[2]);
				direction = resultSplit[3];
				Log.d("scrollsyncmain", "oneventrecognized " + result);
				boolean inSync = corrValue >= 0.8;
				if (inSync) {
				    long syncTime = System.currentTimeMillis();
				    if (syncTime - lastSync > 2000) {
       				    Log.v("synced", "synced");
       				    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "sent", Toast.LENGTH_SHORT).show();
                            }
                        });
                        DatagramSocket socket = null ;

                        try
                        {
                            // Convert the arguments first, to ensure that they are valid
                            InetAddress host = InetAddress.getByName( "192.168.219.229" ) ;
                            int port         = 5001 ;

                            if (socket != null) {
                                socket.close();
                            }
                            // Construct the socket
                            socket = new DatagramSocket() ;

                            // Construct the datagram packet
                            byte [] data = "accept".getBytes() ;
                            DatagramPacket packet = new DatagramPacket( data, data.length, host, port ) ;
                            socket.send( packet ) ;
//                            socket.setSoTimeout( 2000 ) ;
                            packet.setData( new byte[PACKETSIZE] ) ;
//                            socket.receive( packet ) ;
//                            System.out.println( new String(packet.getData()) ) ;

                        }
                        catch( Exception e )
                        {
                            System.out.println( e ) ;
                        }
                        finally
                        {
                            if( socket != null )
                                socket.close() ;
                        }
       				    lastSync = syncTime;
                    }
                }
            }
        });

        new Thread(synchroDetector).start();
        buttonController.addTimer(new Runnable() {
            @Override
            public void run() {
				if (synchroDetector != null) {
					synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
				}

                try {
                    Thread.sleep(FLASH_LENGTH/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
				if (synchroDetector != null) {
                    synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
                }
//                boolean inSync = meanTapDetector.detect(new Tuple2<>(new NVector(leftMagVal), new NVector(rightMagVal)));
//                boolean inSync = meanTapDetector.detect(new Tuple2<>(new NVector(leftGyroVal), new NVector(rightGyroVal)));
//                orbitsView.setInSYnc(inSync);
//
//                final ScrollView scrollView = (ScrollView)findViewById(R.id.scrollview);
//                if(inSync && orbitsView.getSyncDirection().equals("right"))
//                {
//                    scrollView.post(new Runnable() {
//                        public void run() {
//                            scrollView.smoothScrollBy(30,30);
//                        }
//                    });
//                }
//                else
//                if(inSync && orbitsView.getSyncDirection().equals("left"))
//                {
//                    scrollView.post(new Runnable() {
//                        public void run() {
//                            scrollView.smoothScrollBy(-30,-30);
//                        }
//                    });
//                }

            }
        }, FLASH_LENGTH/2);
    }

	@Override
    public void onSensorChanged(SensorEvent sensorEvent) {
                //Log.v("onSensorChanged", sensorEvent.sensor.getStringType());
        // consider switching to storing string representation
        float[] sensorValuesCopy = new float[sensorEvent.values.length];
        System.arraycopy(sensorEvent.values, 0, sensorValuesCopy, 0, sensorEvent.values.length);
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            lastSensorTime = sensorEvent.timestamp;
            double convertTimestamp = lastSensorTime / 1000000;
            lastConvertedTS = convertTimestamp;
            double[] sensorValuesDouble = new double[sensorValuesCopy.length];
            for (int i = 0; i < sensorValuesDouble.length; i++) {
                sensorValuesDouble[i] = sensorValuesCopy[i];
            }
            synchroDetector.dataBuffer.offer(new Tuple2<>(new double[] {convertTimestamp}, sensorValuesDouble));
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        }

    }


	@Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}
}
