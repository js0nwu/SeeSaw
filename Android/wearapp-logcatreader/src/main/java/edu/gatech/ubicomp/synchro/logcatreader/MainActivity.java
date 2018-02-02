package edu.gatech.ubicomp.synchro.logcatreader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

	private TextView mTextView;

	private static final String TAG = MainActivity.class.getCanonicalName();
	private static final String processId = Integer.toString(android.os.Process.myPid());

	private Handler logHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
		stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
			@Override
			public void onLayoutInflated(WatchViewStub stub) {
				mTextView = (TextView) stub.findViewById(R.id.text);
				logHandler = new Handler();
				logHandler.post(new Runnable() {
					@Override
					public void run() {
						while(true) {

							Log.d(TAG, "testing");
							StringBuilder builder = new StringBuilder();

							try {
								String[] command = new String[] { "logcat", "-b", "events"};

								Process process = Runtime.getRuntime().exec(command);

								BufferedReader bufferedReader = new BufferedReader(
										new InputStreamReader(process.getInputStream()));

								String line;
								while ((line = bufferedReader.readLine()) != null) {
//									if (line.contains(processId)) {
										builder.append(line);
										builder.append("\n");
										Log.d(TAG, builder.toString());
										builder.setLength(0);
//									}
								}
							} catch (IOException ex) {
								Log.e(TAG, "getLog failed", ex);
							}
						}
					}
				});
			}
		});
	}

	@Override /* KeyEvent.Callback */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_NAVIGATE_NEXT:
				// Do something that advances a user View to the next item in an ordered list.
				Log.d("tag", "next");
				return true;
			case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
				// Do something that advances a user View to the previous item in an ordered list.
				Log.d("tag", "previous");
				return true;
		}
		// If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
		return super.onKeyDown(keyCode, event);
	}
}
