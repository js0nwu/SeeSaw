package edu.gatech.ubicomp.synchro.swipedatacollect;

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
		Intent sessionIntent = new Intent(PersonActivity.this, SwipeActivity.class);
		long currentTime = System.currentTimeMillis();
		Date date = new Date(currentTime);
		sessionIntent.putExtra("timestamp", formatter.format(date));
		sessionIntent.putExtra("person", personText.getText().toString());
		sessionIntent.putExtra("activity", activityText.getText().toString());
		PersonActivity.this.startActivity(sessionIntent);
	}
}
