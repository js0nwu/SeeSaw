package edu.gatech.ubicomp.scroll;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
//import edu.gatech.ubicomp.flux.*;
import edu.gatech.ubicomp.synchro.detector.*;
import edu.gatech.ubicomp.menudemo.R;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int FLASH_LENGTH = 1000;

	private edu.gatech.ubicomp.scroll.OrbitsView orbitsView;

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

    private double lastSensorTime = 0;
    private double lastConvertedTS = 0;

    private double lastSetTS = 0;
    private double lastSetTS2 = 0;
    private int lastTNum = 0;

	private boolean recordingMode = true;
	private boolean hasRecorded = false;

	private int selectedIndex = 0;

	private String[] values;

	private ListView listView = null;

    private SynchroDetector synchroDetector = null;
    private SynchroDetector synchroDetector2 = null;

    private boolean blinkOn = true;

    private void blinkCursor() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (listView == null) {
                    return;
                }
                listView.setItemChecked(selectedIndex, blinkOn);
            }
        });
        blinkOn = !blinkOn;
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
                orbitsView = (edu.gatech.ubicomp.scroll.OrbitsView) findViewById(R.id.orbits2);
                hasRecorded = true;
                orbitsView.setRecordingMode(recordingMode);
                if(hasRecorded & !recordingMode) new SaveSensorsTask().execute();
                listView = (ListView) findViewById(R.id.listView);
                values = new String[] {"Browser", "Phone", "Contacts", "SMS"};
                final ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_activated_1, values);
                listView.setAdapter(adapter);
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listView.setItemChecked(selectedIndex, true);
				RelativeLayout screen = (RelativeLayout) findViewById(R.id.screen2);
				screen.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						//recordingMode = !recordingMode;
						//hasRecorded = true;
						//orbitsView.setRecordingMode(recordingMode);
						//Log.d("debug", Boolean.toString(recordingMode));


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
				Log.d("menu", "r " + corrValue);
				orbitsView.setSyncDirection(direction);
				boolean inSync = corrValue >= 0.8;
                orbitsView.setInSYnc(inSync);
                if (inSync) {
                    if (lastConvertedTS - lastSetTS < 1500) {
                        return;
                    }
                    if (direction.equals("right")) {
                        selectedIndex++;
                    } else if (direction.equals("left")) {
                        selectedIndex--;
                    }
                    selectedIndex = Math.min(values.length - 1, selectedIndex);
                    selectedIndex = Math.max(0, selectedIndex);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < values.length; i++) {
                                listView.setItemChecked(i, false);
                            }
                            listView.setItemChecked(selectedIndex, true);
                        }
                    });
                    lastSetTS = lastConvertedTS;
                }
            }
        });
        synchroDetector.referencePeriod = 1000;
        new Thread(synchroDetector).start();
        synchroDetector2 = new SynchroDetector(true);
        synchroDetector2.setActivationMode(false);
        synchroDetector2.setEventRecognitionListener(new EventRecognitionListener() {
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
				Log.d("menu", "r " + corrValue);
				orbitsView.setSyncDirection(direction);
				boolean inSync = corrValue >= 0.4;
                orbitsView.setInSYnc(inSync);
                if (inSync) {
                    if (lastConvertedTS - lastSetTS2 < 5000) {
                        return;
                    }
//                    if (direction.equals("right")) {
//                        selectedIndex++;
//                    } else if (direction.equals("left")) {
//                        selectedIndex--;
//                    }
//                    selectedIndex = Math.min(values.length - 1, selectedIndex);
//                    selectedIndex = Math.max(0, selectedIndex);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            for (int i = 0; i < values.length; i++) {
//                                listView.setItemChecked(i, false);
//                            }
//                            listView.setItemChecked(selectedIndex, true);
//                        }
//                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), values[selectedIndex].toString(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplication(), ConfirmationActivity.class);
                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
                            startActivity(intent);
                        }
                    });
                    lastSetTS2 = lastConvertedTS;
                }
            }
        });
        synchroDetector2.referencePeriod = 2000;
        new Thread(synchroDetector2).start();
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
            Tuple2<double[]> dataTuple = new Tuple2<>(new double[] {convertTimestamp}, sensorValuesDouble);
            synchroDetector.dataBuffer.offer(dataTuple);
            synchroDetector2.dataBuffer.offer(dataTuple);
            gyroCurrent = sensorValuesCopy;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelCurrent = sensorValuesCopy;
        }
        int tNum = (int) (lastConvertedTS / (1000 / 4)) % 8;

        if (tNum != lastTNum) {
            if (tNum == 0 || tNum == 4) {
                if (orbitsView != null) {
                    orbitsView.drawLeftCircle();
                }
                if (synchroDetector != null) {
                    synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
                }
            } else if (tNum == 2 || tNum == 6) {
                if (orbitsView != null) {
                    orbitsView.drawRightCircle();
                }
                if (synchroDetector != null) {
                    synchroDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
                }
            }
            if (tNum == 0) {
                blinkCursor();
                if (synchroDetector2 != null) {
                    synchroDetector2.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
                }
            } else if (tNum == 4) {
                blinkCursor();
                if (synchroDetector2 != null) {
                    synchroDetector2.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
                }
            }
        }

        lastTNum = tNum;
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
