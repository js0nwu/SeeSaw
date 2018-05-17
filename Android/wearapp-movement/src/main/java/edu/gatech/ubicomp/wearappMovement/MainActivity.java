package edu.gatech.ubicomp.wearappMovement;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import android.widget.Toast;
import edu.gatech.ubicomp.synchro.detector.*;

import edu.gatech.ubicomp.movement.R;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity implements SensorEventListener {

    private static final int FLASH_LENGTH = 1000;

    private edu.gatech.ubicomp.wearappMovement.Controller buttonController = new edu.gatech.ubicomp.wearappMovement.Controller("ButtonController");

    private SensorManager sensorManager;
    private Sensor gyroscope;

    private double lastSensorTime = 0;
    private double lastConvertedTS = 0;
	private long lastSync = 0;

    private SynchroDetector synchroDetector = null;

    private DatagramSocket socket = null;
    private DatagramPacket packet = null;

    private final int PACKETSIZE = 36;

    private final int SAVE_INTERVAL = 10000;

    private String baseDir = null;
    private File saveDir = null;
    private ArrayList<String> writeData = new ArrayList<>();

    private Handler saveHandler = new Handler();

    private Thread synchroThread = null;

    private boolean saveOutput = true;

    private Runnable saveOutputRunnable = new Runnable() {
        @Override
        public void run() {
            new SaveOutputTask().execute();
            if (saveOutput) {
                saveHandler.postDelayed(saveOutputRunnable, SAVE_INTERVAL);
            }
        }
    };

    private class SaveOutputTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {
            if (writeData.size() == 0) {
                return true;
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);
            File outputFile = new File(baseDir, reportDate + ".csv");
            ArrayList<String> writeDataCopy = new ArrayList<String>(writeData);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outputFile);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                for (String line : writeDataCopy) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeData.clear();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d("SaveOutputTask", "completed save output data " + result);
        }
    }

    private void checkFeaturesAndPermissions() {

        for (String s : Config.APP_PERMISSIONS) {
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), s);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.i("location", "no permission");
                ActivityCompat.requestPermissions(this, Config.APP_PERMISSIONS, 1);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String ipAddress = Utils.getIPAddress(true);
        if (ipAddress == null || ipAddress.equals("")) {
            finish();
            return;
        }
        Toast.makeText(getApplicationContext(), "IP: " + ipAddress, Toast.LENGTH_LONG).show();
        checkFeaturesAndPermissions();
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
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            Log.d("gyro", "gyroscope available in this Android device");
        }
        else {
            Log.d("gyro", "no gyroscope in this Android device");
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
                writeData.add("" + timeRecognized + "," + result);
				boolean inSync = corrValue >= 0.8;
				if (inSync) {
				    long syncTime = System.currentTimeMillis();
				    if (syncTime - lastSync > 1000) {
       				    Log.v("synced", "synced");
       				    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "sent", Toast.LENGTH_SHORT).show();
                            }
                        });
       				    writeData.add("" + syncTime + ",sent");
                        try
                        {
                            InetAddress host = InetAddress.getByName( "192.168.219.229" ) ;
                            int port = 5001 ;
                            // Construct the socket
                            if (socket == null) {
                                socket = new DatagramSocket();
                            }
                            // Construct the datagram packet
                            byte [] data = "accept".getBytes() ;
                            DatagramPacket packet = new DatagramPacket( data, data.length, host, port ) ;
                            socket.send( packet ) ;
                            packet.setData( new byte[PACKETSIZE] ) ;
                        }
                        catch(Exception e)
                        {
                            System.out.println(e) ;
                        }
       				    lastSync = syncTime;
                    }
                }
            }
        });

        synchroThread = new Thread(synchroDetector);
        synchroThread.start();
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
            }
        }, FLASH_LENGTH/2);
        saveHandler.postDelayed(saveOutputRunnable, SAVE_INTERVAL);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);
        baseDir = "/sdcard/Synchro/" + reportDate + "/";
        saveDir = new File(baseDir);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        Log.v("onCreate", "" + saveDir.exists());
        writeData.add("" + System.currentTimeMillis() + ",start");

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
        }
    }


	@Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

	@Override
	protected void onResume() {
		super.onResume();
		saveOutput = true;
		sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
        saveOutput = false;
        synchroDetector.isRunning = false;
        synchroThread.interrupt();
        if (socket != null) {
		    if (socket.isConnected()) {
		        socket.disconnect();
            }
            if (!socket.isClosed()) {
		        socket.close();
            }
            socket = null;
        }
    }
}
