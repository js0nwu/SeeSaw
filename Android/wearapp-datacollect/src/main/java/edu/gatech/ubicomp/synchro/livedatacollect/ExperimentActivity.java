package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExperimentActivity extends Activity {

    private List<Experiment> experimentList = new ArrayList<>();

    private String person;

    private final String EXPERIMENT_LIST_KEY = "experimentList";

    private SharedPreferences pref;

    private TextView experimentText;
    private TextView progressText;

    private Experiment currentExperiment;

    private void populateExperimentList() {
        String existingListData = pref.getString(EXPERIMENT_LIST_KEY, "");
        if (!existingListData.equals("") && existingListData.contains("\n")) {
            String[] existingList = existingListData.split("\n");
            for (String s : existingList) {
                if (s.contains(",")) {
                    String[] e = s.split(",");
                    Experiment oldExperiment = new Experiment(e[0], e[1], Integer.parseInt(e[2]), Integer.parseInt(e[3]), e[4]);
                    experimentList.add(oldExperiment);
                }
            }
            return;
        }
        int[] flashes = { 750, 1000, 1250 };
        int[] ticks = { 15 };
        String[] scenarios = { "Thumb - sync", "Sit - still", "Walk - sync", "Walk - noise", "Browse - noise"};
        int repeats = 2;
        int counter = 0;
        for (String scenario : scenarios) {
            for (int flash : flashes) {
                for (int tick: ticks) {
                    for (int i = 0; i < repeats; i++) {
                        Experiment e = new Experiment(person, "" + counter, flash, tick, scenario);
                        experimentList.add(e);
                        counter++;
                    }
                }
            }
        }
        List<Experiment> experimentListCopy = new ArrayList<>(experimentList);
        Collections.shuffle(experimentList);
        String filePrefix = "p" + person + "_t" + System.currentTimeMillis();
        String fileSuffix = ".txt";
        String filename = filePrefix + fileSuffix;
        Log.v("writeExperiment", "write experiment " + filename);
        StringBuilder sb = new StringBuilder();
        for (Experiment e : experimentListCopy) {
            sb.append(e.toString());
            sb.append("\n");
        }
        String content = sb.toString();
        File outputFile = new File(Environment.getExternalStorageDirectory() + "/Synchro/", filename);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment);
        Intent experimentIntent = getIntent();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        person = experimentIntent.getStringExtra("person");
        Log.v("person", person);
        experimentText = (TextView) findViewById(R.id.experimentText);
        progressText = (TextView) findViewById(R.id.progressText);
        pref = getSharedPreferences("experiment_" + person, Context.MODE_PRIVATE);
        populateExperimentList();
        updateLabels();
    }

    private String serializeExperimentList() {
        StringBuilder sb = new StringBuilder();
        for (Experiment e : experimentList) {
            sb.append(e.getPerson() + "," + e.getExperiment() + "," + e.getFlashes() + "," + e.getTicks() + "," + e.getScenario());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void startTrial(View v) {
        Intent sessionIntent = new Intent(ExperimentActivity.this, MainActivity.class);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putString(EXPERIMENT_LIST_KEY, serializeExperimentList());
        prefEditor.commit();
        sessionIntent.putExtra("FLASH_LENGTH", currentExperiment.getFlashes());
        sessionIntent.putExtra("SESSION_LENGTH_TICKS", currentExperiment.getTicks());
        sessionIntent.putExtra("person", currentExperiment.getPerson());
        sessionIntent.putExtra("experiment", currentExperiment.getExperiment());
        ExperimentActivity.this.startActivity(sessionIntent);
        if (experimentList.size() > 0) {
            updateLabels();
        } else {
            finish();
        }
    }

    private void updateLabels() {
        currentExperiment = experimentList.remove(0);
        experimentText.setText(currentExperiment.getScenario() + " " + currentExperiment.getFlashes() + "ms " + currentExperiment.getTicks() + "x");
        progressText.setText((experimentList.size() + 1) + " left");
    }

    public void newTrial(View v) {
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putString(EXPERIMENT_LIST_KEY, "");
        prefEditor.commit();
        recreate();
    }
}
