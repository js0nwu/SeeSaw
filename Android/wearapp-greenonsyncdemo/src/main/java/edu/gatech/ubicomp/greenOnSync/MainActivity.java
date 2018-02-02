package edu.gatech.ubicomp.greenOnSync;

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
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
//import edu.gatech.ubicomp.flux.*;
import edu.gatech.ubicomp.buttons.R;
import edu.gatech.ubicomp.synchro.detector.*;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int FLASH_LENGTH = 1000;

    private Controller buttonController = new Controller("ButtonController");

	private OrbitsView orbitsView;

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private ArrayList<float[]> leftMag = new ArrayList<>();
    private ArrayList<float[]> leftGyro = new ArrayList<>();
    private ArrayList<float[]> leftAccel = new ArrayList<>();

    private ArrayList<float[]> rightMag = new ArrayList<>();
    private ArrayList<float[]> rightGyro = new ArrayList<>();
    private ArrayList<float[]> rightAccel = new ArrayList<>();

    private ArrayList<float[]> diffMag = new ArrayList<>();
    private ArrayList<float[]> diffGyro = new ArrayList<>();
    private ArrayList<float[]> diffAccel = new ArrayList<>();

    private float[] magCurrent;
    private float[] gyroCurrent;
    private float[] accelCurrent;

	private boolean recordingMode;
	private boolean hasRecorded = false;

    private Detector meanTapDetector = new MeanTapDetector();
    private Detector stdTapDetector = new StdTapDetector();
    private TextView tv;

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
            String filePrefix = "" + System.currentTimeMillis();
			String fileSuffix = ".csv";
            writeCSV(filePrefix + "leftMag" + fileSuffix, leftMag);
            writeCSV(filePrefix + "leftGyro" + fileSuffix, leftGyro);
            writeCSV(filePrefix + "leftAccel" + fileSuffix, leftAccel);

            writeCSV(filePrefix + "rightMag" + fileSuffix, rightMag);
            writeCSV(filePrefix + "rightGyro" + fileSuffix, rightGyro);
            writeCSV(filePrefix + "rightAccel" + fileSuffix, rightAccel);

            writeCSV(filePrefix + "diffMag" + fileSuffix, diffMag);
            writeCSV(filePrefix + "diffGyro" + fileSuffix, diffGyro);
            writeCSV(filePrefix + "diffAccel" + fileSuffix, diffAccel);

            writeCSVs(filePrefix + "combinedMag" + fileSuffix, leftMag, rightMag, diffMag);
            writeCSVs(filePrefix + "combinedGyro" + fileSuffix, leftGyro, rightGyro, diffGyro);
            writeCSVs(filePrefix + "combinedAccel" + fileSuffix, leftAccel, rightAccel, diffAccel);
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
				RelativeLayout screen = (RelativeLayout) findViewById(R.id.screen2);
                tv = (TextView)findViewById(R.id.textView);
                screen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recordingMode = !recordingMode;
                        hasRecorded = true;
                        orbitsView.setRecordingMode(recordingMode);
                        //Log.d("debug", Boolean.toString(recordingMode));

                        if(hasRecorded & !recordingMode) new SaveSensorsTask().execute();
                    }
                });
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

        meanTapDetector.setEventRecognitionListener(new EventRecognitionListener() {
            @Override
            public void onEventRecognized(String result) {
                Log.v("meanresult", result);
            }
        });
        stdTapDetector.setEventRecognitionListener(new EventRecognitionListener() {
            @Override
            public void onEventRecognized(String result) {
                Log.v("stdresult", result);
                setText(tv, result);
                //orbitsView.setSyncDirection(result);

            }
        });
        buttonController.addTimer(new Runnable() {
            @Override
            public void run() {
                if (orbitsView == null) {
                    return;
                }
                orbitsView.drawLeftCircle();
                float[] leftMagVal = magCurrent;
                float[] leftGyroVal = gyroCurrent;
                float[] leftAccelVal = accelCurrent;

				if(recordingMode) {
					leftMag.add(magCurrent);
					leftGyro.add(gyroCurrent);
					leftAccel.add(accelCurrent);
				}

                try {
                    Thread.sleep(FLASH_LENGTH/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

				orbitsView.drawRightCircle();
                float[] rightMagVal = magCurrent;
                float[] rightGyroVal = gyroCurrent;
                float[] rightAccelVal = accelCurrent;

				if(recordingMode) {
					rightMag.add(magCurrent);
					rightGyro.add(gyroCurrent);
					rightAccel.add(accelCurrent);
				}
                float[] diffMagVal = new float[leftMagVal.length];
                float[] diffGyroVal = new float[leftGyroVal.length];
                float[] diffAccelVal = new float[leftAccelVal.length];
                for (int i = 0; i < diffMagVal.length; i++) {
                    diffMagVal[i] = leftMagVal[i] - rightMagVal[i];
                }
                for (int i = 0; i < diffGyroVal.length; i++) {
                    diffGyroVal[i] = leftGyroVal[i] - rightGyroVal[i];
                }
                for (int i = 0; i < diffAccelVal.length; i++) {
                    diffAccelVal[i] = leftAccelVal[i] - rightAccelVal[i];
                }

                boolean inSync = meanTapDetector.detect(new Tuple2<>(new NVector(leftGyroVal), new NVector(rightGyroVal)));
                orbitsView.setInSYnc(inSync);

                if(!inSync)
                {
                    setText(tv, "unknown");
                }

				if(recordingMode) {
					diffMag.add(diffMagVal);
					diffGyro.add(diffGyroVal);
					diffAccel.add(diffAccelVal);
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

    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText("Sync Direction: "+value);
            }
        });
    }
}
