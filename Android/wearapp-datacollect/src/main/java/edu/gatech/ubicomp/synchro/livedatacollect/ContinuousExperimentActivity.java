package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContinuousExperimentActivity extends Activity implements SensorEventListener {

    private int SAVE_INTERVAL = 5000;

    private Controller buttonController = new Controller("ButtonController");

    private OrbitsView orbitsView;

    private boolean debugMode = true;

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private ArrayList<String> sensorData = new ArrayList<>();

    private Runnable saveDataRunnable;

    private boolean isRecording = true;

    private TextView counterText;

    private String person;
    private String experiment;
    private Long timestamp;

    private int cycles = 0;


    private int lineCounter = 0;
    private int experimentCounter = 0;

    private List<Experiment> experimentList = new ArrayList<>();
    private List<Experiment> experimentListShuffled = new ArrayList<>();
    private boolean isLeft = true;

    private Handler taskHandler = new Handler();

    private class TickerRunnable implements Runnable {
        private String experiment;
        private int delayTime;
        private int currentTicks;
        private int totalTicks;

        public TickerRunnable(String experiment, int delayTime, int currentTicks, int totalTicks) {
            this.experiment = experiment;
            this.delayTime = delayTime;
            this.currentTicks = currentTicks;
            this.totalTicks = totalTicks;
        }

        @Override
        public void run() {
            if (orbitsView == null) {
                return;
            }
            if(debugMode) {
                counterText.setText("#" + experiment + "," + "Freq: " + delayTime*2 + "," + "Flash: " + currentTicks + " of " + totalTicks);
            } else {
                counterText.setText("Flash: " + currentTicks + " of " + totalTicks);
            }
            if (isLeft) {
                orbitsView.drawLeftCircle();
                String sensorStringLeft = "" + System.currentTimeMillis() + ",left,";
                sensorData.add(sensorStringLeft);
            } else {
                orbitsView.drawRightCircle();
                String sensorStringRight = "" + System.currentTimeMillis() + ",right,";
                sensorData.add(sensorStringRight);
                cycles++;
            }
            isLeft = !isLeft;

            Log.v("debug", "" + isLeft + "," + currentTicks + "," + totalTicks);
            if (currentTicks < totalTicks) {
                Log.v("debug", "launching" + delayTime + "," + currentTicks + "," + totalTicks);
                taskHandler.postDelayed(new TickerRunnable("" + experimentCounter, delayTime, currentTicks + 1, totalTicks), delayTime);
            } else {
                Log.v("tag", "launching pauser");
                taskHandler.postDelayed(pauser, 500);
                String endTimeStamp = "" + System.currentTimeMillis() + ",end,";
                sensorData.add(endTimeStamp);
            }
        }
    }

    private void runTickSequence(Experiment e) {
        String beginTimeStamp = "" + System.currentTimeMillis() + ",begin" + e.getFlashes() + ",";
        sensorData.add(beginTimeStamp);
        vibrate();
        int flashLength = e.getFlashes();
        int sessionLength = e.getTicks();
        isLeft = true;
        cycles = 0;
        showControls();
        taskHandler.postDelayed(new TickerRunnable("" + experimentCounter, flashLength / 2, 1, sessionLength * 2), 500);
        experimentCounter++;
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            if (experimentListShuffled.size() == 0) {

                return;
            }
            Log.v("el size", "" + experimentListShuffled.size());
            Experiment e = experimentListShuffled.remove(0);
            int flashLength = e.getFlashes();
            int sessionLength = e.getTicks();
            Log.v("debug", "" + flashLength + "," + sessionLength);
            Log.v("tag", "testing");
            runTickSequence(e);
            Log.v("debug", "sessionLength: "+sessionLength+", flashLength: "+flashLength);
            Log.d("debug", "" + flashLength/2 + (sessionLength * flashLength) + 500);
        }
    };


    private Runnable pauser = new Runnable() {
        @Override
        public void run() {
            if (experimentListShuffled.size() == 0) {
                closeApp();
            }
            hideControls();

            int delayTime = buttonController.getPoissonRandom(Config.promptTime);
            taskHandler.postDelayed(animator, delayTime);
        }
    };

    private class SaveSensorsTask extends AsyncTask<Void, Void, Void> {

        private void writeSensorDataCSV(String filename, ArrayList<String> data) {
            Log.v("writeSensorData", "writing sensorData " + filename);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) {
                    continue;
                }
                sb.append(data.get(i));
                sb.append("\n");
            }
            String CSVContent = sb.toString();

            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Synchro/");
            if (!dir.exists()) {dir.mkdirs();} // Create folder if needed
            File outputFile = new File(dir, filename);
            //File outputFile = new File(Environment.getExternalStorageDirectory() + "/Synchro/", filename);
            Log.v("outputFile", outputFile.getAbsolutePath());
            BufferedOutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile, true));
                outputStream.write(CSVContent.getBytes());
                outputStream.close();
                Log.v("writeSensorData", "file write successful");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String filePrefix = "p" + person + "_t" + timestamp + "_" + "x" + experiment;
            String fileSuffix = ".csv";
            Log.v("debug", "" + sensorData.size());
            ArrayList<String> sensorDataCopy = new ArrayList<>(sensorData);
            //Collections.copy(sensorDataCopy, sensorData);
            sensorData.clear();
            writeSensorDataCSV(filePrefix + fileSuffix, sensorDataCopy);
            return null;
        }
    }

    private void saveSession() {
        new SaveSensorsTask().execute();
    }

    private void populateExperimentList() {
        int counter = 0;
        for (int flash : Config.flashFreq) {
            for (int i = 0; i < Config.numOfReps; i++) {
                Experiment e = new Experiment(person, "" + counter, flash, Config.numOfCycles, experiment);
                experimentList.add(e);
                counter++;
            }
        }
        experimentListShuffled = new ArrayList<>(experimentList);
        Collections.shuffle(experimentListShuffled);
        String filePrefix = "p" + person + "_t" + timestamp + "_" + "x" + experiment;
        String fileSuffix = ".txt";
        String filename = filePrefix + fileSuffix;
        Log.v("writeExperiment", "write experiment " + filename);
        StringBuilder sb = new StringBuilder();
        for (Experiment e : experimentListShuffled) {
            Log.v("debug", e.toString());
            sb.append(e.toString());
            sb.append("\n");
        }
        String content = sb.toString();
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Synchro/");
        if (!dir.exists()) {dir.mkdirs();} // Create folder if needed
        File outputFile = new File(dir, filename);
        //File outputFile = new File(Environment.getExternalStorageDirectory() + "/Synchro/", filename);
        Log.v("outputFile", outputFile.getAbsolutePath());
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outputFile);
            outputStream.write(content.getBytes());
            outputStream.close();
            Log.v("writeExperiment", "file write successful");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideControls() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (orbitsView == null || counterText == null) {
                    return;
                }
                orbitsView.setVisibility(View.INVISIBLE);
                counterText.setVisibility(View.INVISIBLE);

                orbitsView.clearCircles();
                counterText.setText("");
            }
        });
    }

    private void showControls() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (orbitsView == null || counterText == null) {
                    return;
                }
                orbitsView.setVisibility(View.VISIBLE);
                counterText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final Intent sessionIntent = getIntent();
        person = sessionIntent.getStringExtra("person");
        experiment = sessionIntent.getStringExtra("experiment");
        timestamp = sessionIntent.getLongExtra("timestamp", 0);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                orbitsView = (OrbitsView) findViewById(R.id.orbits);
                counterText = (TextView) findViewById(R.id.counterText);
                LinearLayout screen = (LinearLayout) findViewById(R.id.screen);
                orbitsView.setRecordingMode(true);
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

        // Generate the experiment list
        populateExperimentList();

        // Add a timer to save data every SAVE_INTERVAL
        saveDataRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    saveSession();
                    taskHandler.postDelayed(this, SAVE_INTERVAL);
                } else {
                    // remove callbacks

                }
            }
        };
        taskHandler.postDelayed(saveDataRunnable, SAVE_INTERVAL);
        taskHandler.post(pauser);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // consider switching to storing string representation
        String sensorString = "" + System.currentTimeMillis() + ",";
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorString += "magnet,";
            for (int i = 0; i < sensorEvent.values.length; i++) {
                sensorString += sensorEvent.values[i] + ",";
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorString += "gyro,";
            for (int i = 0; i < sensorEvent.values.length; i++) {
                sensorString += sensorEvent.values[i] + ",";
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorString += "accel,";
            for (int i = 0; i < sensorEvent.values.length; i++) {
                sensorString += sensorEvent.values[i] + ",";
            }
        }
        sensorString += sensorEvent.timestamp + ",";
        sensorString += lineCounter + ",";
        sensorData.add(sensorString);
        lineCounter++;
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

    @Override
    protected void onStop() {
        super.onStop();
        closeApp();
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    private void closeApp() {
        taskHandler.removeCallbacks(saveDataRunnable);
        taskHandler.removeCallbacks(animator);
        taskHandler.removeCallbacks(pauser);
        isRecording = false;
        finish();
        //finishAffinity();
    }
}