package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Intent;
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

public class Experiment2Activity extends Activity {

    private List<String> experimentList = new ArrayList<>();
	private List<String> experimentListShuffled = new ArrayList<>();

    private String person;
	private Long timestamp;

    private TextView experimentText;
    private TextView progressText;

    private String currentExperiment;

    private void populateExperimentList() {
        for (String s : Config.conditions) {
            experimentList.add(s);
        }
		experimentListShuffled = new ArrayList<>(experimentList);
        Collections.shuffle(experimentListShuffled);
		timestamp = System.currentTimeMillis();
        String filePrefix = "p" + person + "_t" + timestamp + "_x" + "list";
        String fileSuffix = ".txt";
        String filename = filePrefix + fileSuffix;
        Log.v("writeExperiment", "write experiment " + filename);
        StringBuilder sb = new StringBuilder();
        for (String e : experimentListShuffled) {
            sb.append(e);
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
        populateExperimentList();
        updateLabels();
    }

    public void startTrial(View v) {
        Intent sessionIntent = new Intent(Experiment2Activity.this, ContinuousExperimentActivity.class);
        sessionIntent.putExtra("person", person);
        sessionIntent.putExtra("experiment", currentExperiment);
		sessionIntent.putExtra("timestamp", timestamp);
        Experiment2Activity.this.startActivity(sessionIntent);
        if (experimentListShuffled.size() > 0) {
            updateLabels();
        } else {
            finish();
        }
    }

    private void updateLabels() {
        currentExperiment = experimentListShuffled.remove(0);
        experimentText.setText(currentExperiment);
        progressText.setText((experimentListShuffled.size() + 1) + " left");
    }

    public void newTrial(View v) {
        recreate();
    }
}