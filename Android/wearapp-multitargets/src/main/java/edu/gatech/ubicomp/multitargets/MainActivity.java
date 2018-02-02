package edu.gatech.ubicomp.multitargets;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import android.widget.Toast;
import edu.gatech.ubicomp.synchro.detector.*;
import edu.gatech.ubicomp.buttons.R;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int FLASH_LENGTH = 1000;
    private Controller buttonController = new Controller("ButtonController");
    private OrbitsView orbitsView;
    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private double lastSensorTime;
    private double lastConvertedTS;

    private double lastSetTS = 0;

    private float[] magCurrent;
    private float[] gyroCurrent;
    private float[] accelCurrent;
    private boolean orbitDirectionTopBottom = false;
	private boolean recordingMode = true;
	private boolean hasRecorded = false;

	private SynchroDetector synchroDetector = null;

    @Override /* KeyEvent.Callback */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                orbitDirectionTopBottom = !orbitDirectionTopBottom;
                orbitsView.setDirection(orbitDirectionTopBottom);
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                orbitDirectionTopBottom = !orbitDirectionTopBottom;
                orbitsView.setDirection(orbitDirectionTopBottom);
        }
        // If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
        return super.onKeyDown(keyCode, event);
    }
    private class SaveSensorsTask extends AsyncTask<Void, Void, Void> {

        private void writeCSV(String filename, ArrayList<float[]> data) {
            Log.v("writeCSV", "writing CSV " + filename);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) {
                    continue;
                }
                for (int j = 0; j < data.get(i).length; j++) {
                    sb.append("" + data.get(i)[j] + ",");
                }
                sb.append("\n");
            }
            String CSVContent = sb.toString();
			File outputFile = new File(Environment.getExternalStorageDirectory() + "/Flux/", filename);
            Log.v("outputFile", outputFile.getAbsolutePath());
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(outputFile);
                outputStream.write(CSVContent.getBytes());
                outputStream.close();
                Log.v("writeCSV", "file write successful");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void writeCSVs(String filename, ArrayList<float[]>... datas) {
            Log.v("writeCSVs", "writing CSVs " + filename);
            StringBuilder sb = new StringBuilder();
            ArrayList<Integer> dataLengths = new ArrayList<>();
            for (ArrayList<float[]> data : datas) {
                dataLengths.add(new Integer(data.size()));
            }
            int minLength = Collections.min(dataLengths);
            for (int i = 0; i < minLength; i++) {
                for (int j = 0; j < datas.length; j++) {
                    for (int k = 0; k < datas[j].get(i).length; k++) {
                        sb.append("" + datas[j].get(i)[k] + ",");
                    }
                }
                sb.append("\n");
            }
            String CSVContent = sb.toString();
            File outputFile = new File(Environment.getExternalStorageDirectory() + "/Flux/", filename);
            Log.v("outputFile", outputFile.getAbsolutePath());
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(outputFile);
                outputStream.write(CSVContent.getBytes());
                outputStream.close();
                Log.v("writeCSVs", "file write successful");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                orbitsView = (OrbitsView) findViewById(R.id.orbits2);
                hasRecorded = true;
                orbitsView.setRecordingMode(recordingMode);
                //Log.d("debug", Boolean.toString(recordingMode));

                if(hasRecorded & !recordingMode) new SaveSensorsTask().execute();

//                TextView tv = (TextView)findViewById(R.id.textView);
//                tv.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        orbitDirectionTopBottom = !orbitDirectionTopBottom;
//                    orbitsView.setDirection(orbitDirectionTopBottom);
//                    }
//                });



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
        synchroDetector.setActivationMode(false);
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
				    if (lastConvertedTS - lastSetTS < 1500) {
				        return;
                    }
				    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (direction.equals("right")) {
                                String msg = "Next";
                                if (orbitDirectionTopBottom) {
                                    msg = "Volume down";
                                }
              				    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                            } else if (direction.equals("left")) {
                                String msg = "Play/Pause";
                                if (orbitDirectionTopBottom) {
                                    msg = "Volume up";
                                }
                                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
				    lastSetTS = lastConvertedTS;
                }
            }
        });

        new Thread(synchroDetector).start();
buttonController.addTimer(new Runnable() {
            @Override
            public void run() {
                if (orbitsView == null) {
                    return;
                }
                orbitsView.drawLeftCircle();
				if (synchroDetector != null) {
					synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
				}

                try {
                    Thread.sleep(FLASH_LENGTH/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

				orbitsView.drawRightCircle();
				if (synchroDetector != null) {
                    synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
                }
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
            magCurrent = sensorValuesCopy;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            lastSensorTime = sensorEvent.timestamp;
            double convertTimestamp = lastSensorTime / 1000000;
            lastConvertedTS = convertTimestamp;
            double[] sensorValuesDouble = new double[sensorValuesCopy.length];
            for (int i = 0; i < sensorValuesDouble.length; i++) {
                sensorValuesDouble[i] = sensorValuesCopy[i];
            }
            synchroDetector.dataBuffer.offer(new Tuple2<>(new double[] {convertTimestamp}, sensorValuesDouble));
            gyroCurrent = sensorValuesCopy;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelCurrent = sensorValuesCopy;
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
