package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class StartActivity extends Activity {

    private EditText flashText;
    private EditText sessionText;
    private EditText personText;
    private EditText experimentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        flashText = (EditText) findViewById(R.id.flashText);
        sessionText = (EditText) findViewById(R.id.sessionText);
        personText = (EditText) findViewById(R.id.personText);
        experimentText = (EditText) findViewById(R.id.experimentText);
    }

    public void startSession(View v) {
        Intent sessionIntent = new Intent(StartActivity.this, MainActivity.class);
        sessionIntent.putExtra("FLASH_LENGTH", Integer.parseInt(flashText.getText().toString()));
        sessionIntent.putExtra("SESSION_LENGTH_TICKS", Integer.parseInt(sessionText.getText().toString()));
        sessionIntent.putExtra("person", personText.getText().toString());
        sessionIntent.putExtra("experiment", experimentText.getText().toString());
        StartActivity.this.startActivity(sessionIntent);
    }

    public void flashLess(View v) {
        int flashValue = Integer.parseInt(flashText.getText().toString());
        flashValue -= 50;
        flashText.setText("" + flashValue);
    }

    public void flashMore(View v) {
        int flashValue = Integer.parseInt(flashText.getText().toString());
        flashValue += 50;
        flashText.setText("" + flashValue);
    }

    public void sessionLess(View v) {
        int sessionValue = Integer.parseInt(sessionText.getText().toString());
        sessionValue -= 5;
        sessionText.setText("" + sessionValue);
    }

    public void sessionMore(View v) {
        int sessionValue = Integer.parseInt(sessionText.getText().toString());
        sessionValue += 5;
        sessionText.setText("" + sessionValue);
    }

    public void personLess(View v) {
        int personValue = Integer.parseInt(personText.getText().toString());
        personValue -= 1;
        personValue = Math.max(0, personValue);
        personText.setText("" + personValue);
    }

    public void personMore(View v) {
        int personValue = Integer.parseInt(personText.getText().toString());
        personValue += 1;
        personValue = Math.max(0, personValue);
        personText.setText("" + personValue);
    }

    public void experimentLess(View v) {
        int experimentValue = Integer.parseInt(experimentText.getText().toString());
        experimentValue -= 1;
        experimentValue = Math.max(0, experimentValue);
        experimentText.setText("" + experimentValue);
    }

    public void experimentMore(View v) {
        int experimentValue = Integer.parseInt(experimentText.getText().toString());
        experimentValue += 1;
        experimentValue = Math.max(0, experimentValue);
        experimentText.setText("" + experimentValue);
    }
}
