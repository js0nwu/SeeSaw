package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PersonActivity extends Activity {

	private TextView personText;
	private TextView activityText;
	private int activityIndex = 0;

	private DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

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
		checkFeaturesAndPermissions();
		setContentView(R.layout.activity_person);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		personText = (TextView) findViewById(R.id.personText);
		activityText = (TextView) findViewById(R.id.activityText);
	}

	public void personLess(View v) {
		int personValue = Integer.parseInt(personText.getText().toString());
		personValue--;
		personValue = Math.max(0, personValue);
		personText.setText("" + personValue);
	}

	public void personMore(View v) {
		int personValue = Integer.parseInt(personText.getText().toString());
		personValue++;
		personValue = Math.max(0, personValue);
		personText.setText("" + personValue);
	}

	public void activityPrev(View v) {
		activityIndex--;
		activityIndex = Math.max(0, activityIndex);
		String activity = Config.ACTIVITY_LIST[activityIndex];
		activityText.setText("" + activity);
	}

	public void activityNext(View v) {
		activityIndex++;
		activityIndex = Math.min(activityIndex, Config.ACTIVITY_LIST.length - 1);
		String activity = Config.ACTIVITY_LIST[activityIndex];
		activityText.setText("" + activity);
	}

	public void startExperiment(View v) {
		Intent sessionIntent = new Intent(PersonActivity.this, SynchroActivity.class);
		long currentTime = System.currentTimeMillis();
		Date date = new Date(currentTime);
		sessionIntent.putExtra("timestamp", formatter.format(date));
		sessionIntent.putExtra("person", personText.getText().toString());
		sessionIntent.putExtra("activity", activityText.getText().toString());
		if (activityText.getText().toString().equals("debug")) {
			Config.DEBUG_VIZ = true;
			Config.TRIGGER_THRESHOLD = 1.0;
			Config.NOISE_MODE = false;
			Config.AWAKE_MODE = false;
			Config.SUBTLE_FACTOR = 0;
		} else if (activityText.getText().toString().equals("noise")) {
			Config.DEBUG_VIZ = false;
			Config.TRIGGER_THRESHOLD = 1.0;
			Config.NOISE_MODE = true;
			Config.AWAKE_MODE = false;
			Config.SUBTLE_FACTOR = 0;
		} else if (activityText.getText().toString().startsWith("subtle")) {
			Config.DEBUG_VIZ = false;
			Config.TRIGGER_THRESHOLD = 0.85;
			Config.NOISE_MODE = false;
			Config.AWAKE_MODE = false;
			String text = activityText.getText().toString();
			if (text.endsWith("1")) {
				Config.SUBTLE_FACTOR = 1;
			} else if (text.endsWith("2")) {
				Config.SUBTLE_FACTOR = 3;
			} else if (text.endsWith("3")) {
				Config.SUBTLE_FACTOR = 6;
			} else {
				Config.SUBTLE_FACTOR = 0;
			}
		} else if (activityText.getText().toString().equals("activate")) {
			Config.DEBUG_VIZ = false;
			Config.TRIGGER_THRESHOLD = 0.85;
			Config.NOISE_MODE = false;
			Config.AWAKE_MODE = false;
			Config.SUBTLE_FACTOR = 0;
			sessionIntent.putExtra("activate", true);
		} else if (activityText.getText().toString().equals("adebug")) {
			Config.DEBUG_VIZ = true;
			Config.TRIGGER_THRESHOLD = 1.0;
			Config.NOISE_MODE = false;
			Config.AWAKE_MODE = true;
			Config.SUBTLE_FACTOR = 0;
			sessionIntent.putExtra("activate", true);
		} else {
			Config.DEBUG_VIZ = false;
			Config.TRIGGER_THRESHOLD = 0.85;
			Config.NOISE_MODE = false;
			Config.AWAKE_MODE = false;
			Config.SUBTLE_FACTOR = 0;
		}
		if (activityText.getText().toString().equals("activate")) {
			Config.SESSION_PAUSE_TIME = 120;
		} else if (activityText.getText().toString().equals("prep")) {
			Config.SESSION_PAUSE_TIME = 3;
		} else {
			Config.SESSION_PAUSE_TIME = 10;
		}

		if (activityText.getText().toString().equals("activate")) {
			Config.NUM_CYCLES = 120;
			Config.NUM_REPS = 30; // (4 * 60) / (4 * 2)
		} else if (activityText.getText().toString().contains("debug")) {
			Config.NUM_CYCLES = 50;
			Config.NUM_REPS = 1;
		} else {
			Config.NUM_CYCLES = 10;
			Config.NUM_REPS = 5;
		}
		PersonActivity.this.startActivity(sessionIntent);
	}
}
