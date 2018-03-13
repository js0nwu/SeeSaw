package edu.gatech.ubicomp.synchro.swipedatacollect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class SwipeActivity extends Activity implements SensorEventListener {

	private String TAG = this.getClass().getSimpleName();

	private boolean debugMode = false;
	private boolean isRecording = false;
	boolean isLeftCircleTouched = false;
	boolean isRightCircleTouched = false;

	private SwipeView swipeView;
	private TextView outputText;

	private int experimentCounter = 0;
	private int watchLineCounter = 0;

	private ArrayList<String> swipeData = new ArrayList<>();
	private ArrayList<String> sensorData = new ArrayList<>();

	private String timestamp;
	private String person;
	private String activity;
	private Long vibrateTimestamp;

	private boolean synced = true;

	private Handler taskHandler = new Handler();
	private SensorManager mSensorManager;
	private HandlerThread mServiceThread;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	private Sensor mAccelerometer;
	private Sensor mGyroscope;
	private Sensor mMagnetometer;

	private HandlerThread flashUIThread;
	private Handler flashUIHandler;

	private DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final Intent sessionIntent = getIntent();
		timestamp = sessionIntent.getStringExtra("timestamp");
		person = sessionIntent.getStringExtra("person");
		activity = sessionIntent.getStringExtra("activity");

		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
		stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
			@Override
			public void onLayoutInflated(WatchViewStub stub) {
				outputText = (TextView) findViewById(R.id.outputText);
				swipeView = (SwipeView) findViewById(R.id.swipeview);
				swipeView.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View view, MotionEvent motionEvent) {

						String touchLog = "";
						float x = motionEvent.getX() ;
						float y = motionEvent.getY() ;
						Long motionEventTimestamp = System.currentTimeMillis();

						switch (motionEvent.getAction())
						{
							case MotionEvent.ACTION_DOWN: {
								touchLog = motionEventTimestamp + ",touch_down," + x + "," + y;
								if(debugMode) Log.d(TAG, touchLog);
								swipeData.add(touchLog);
								//if(swipeView.leftCircle.contains(x,y)) {
								if(swipeView.leftTargetArea.contains(x,y)) {
									isLeftCircleTouched = true;
								} else if(swipeView.rightTargetArea.contains(x,y)) {
									isRightCircleTouched = true;
								}

								break;
							}
							case MotionEvent.ACTION_MOVE: {
								touchLog = motionEventTimestamp + ",touch_move," + x + "," + y;
								if(debugMode) Log.d(TAG, touchLog);
								swipeData.add(touchLog);
								if(isLeftCircleTouched) {
									float newLeft = x - Config.ICON_WIDTH / 2;
									float newRight = x + Config.ICON_WIDTH / 2;
									if (newLeft < 0) {
										newLeft = 0;
										newRight = Config.ICON_WIDTH;
									} else if (newRight > swipeView.screenW) {
										newLeft = swipeView.screenW - Config.ICON_WIDTH;
										newRight = swipeView.screenW;
									}
									swipeView.leftCircle.set(newLeft, (swipeView.screenH / 2) - Config.ICON_HEIGHT / 2, newRight, (swipeView.screenH / 2) + Config.ICON_HEIGHT / 2);
									if (swipeView.rightTargetArea.contains(swipeView.leftCircle)) {
										swipeView.setSwipeDetectedColors();
									} else {
										swipeView.resetSwipeDetectedColors();
									}
								} else if(isRightCircleTouched) {
									float newLeft = x - Config.ICON_WIDTH / 2;
									float newRight = x + Config.ICON_WIDTH / 2;
									System.out.println(newLeft + "," + newRight);
									if (newLeft < 0) {
										newLeft = 0;
										newRight = Config.ICON_WIDTH;
									} else if (newRight > swipeView.screenW) {
										newLeft = swipeView.screenW - Config.ICON_WIDTH;
										newRight = swipeView.screenW;
									}
									System.out.println(newLeft + "," + newRight);
									swipeView.rightCircle.set(newLeft, (swipeView.screenH / 2) - Config.ICON_HEIGHT / 2, newRight, (swipeView.screenH / 2) + Config.ICON_HEIGHT / 2);
									if (swipeView.leftTargetArea.contains(swipeView.rightCircle)) {
										swipeView.setSwipeDetectedColors();
									} else {
										swipeView.resetSwipeDetectedColors();
									}
								}
								break;
							}

							case MotionEvent.ACTION_UP: {
								touchLog = motionEventTimestamp + ",touch_up," + x + "," + y;
								if(debugMode) Log.d(TAG, touchLog);
								swipeData.add(touchLog);
								isLeftCircleTouched = false;
								isRightCircleTouched = false;
								if(swipeView.rightTargetArea.contains(swipeView.leftCircle)) {
									swipeDetected("right");
								} else if (swipeView.leftTargetArea.contains(swipeView.rightCircle)) {
									swipeDetected("left");
								} else {
									swipeView.resetCircleLocations();
									Long swipeTimestamp = System.currentTimeMillis();
									String swipeLog = "" + swipeTimestamp + ",reset,0,0";
									swipeData.add(swipeLog);
								}
								break;
							}
						}
						return true;
					}
				});
			}
		});

		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		mServiceThread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_DEFAULT);
		mServiceThread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = mServiceThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		//		android.os.Message msg = mServiceHandler.obtainMessage();
		//		msg.arg1 = startId;
		//		mServiceHandler.sendMessage(msg);

		// Initialize sensor manager and objects
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI, mServiceHandler);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI, mServiceHandler);
		mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI, mServiceHandler);
		flashUIThread = new HandlerThread("FlashUI", Process.THREAD_PRIORITY_FOREGROUND);
		flashUIThread.start();
		flashUIHandler = new Handler(flashUIThread.getLooper());
		flashUIHandler.post(pauser);
		taskHandler.post(pauser);
    }

	public Runnable sendVibration = new Runnable() {
		@Override
		public void run() {
			vibrateShort();
		}
	};

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			if(debugMode) Log.d(TAG, msg.toString());
			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				// Restore interrupt status.
//				Thread.currentThread().interrupt();
//			}
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			//stopSelf(msg.arg1);
		}
	}

	private Runnable pauser = new Runnable() {
		@Override
		public void run() {
			if(debugMode) Log.d(TAG, "entering pauser");
			if(activity.equals("prep") && experimentCounter == 5) {
				closeApp();
				return;
			}
			if (experimentCounter == Config.NUM_REPS) {
				closeApp();
				return;
			} else {
				isRecording = true;
				hideControls();
				if (swipeView != null) {
					swipeView.resetSwipeDetectedColors();
				}
				int delayTime = 0;
				if(activity.equals("prep")) {
					delayTime = 3000;
				} else {
					delayTime = getPoissonRandom(Config.promptTime);
				}
				taskHandler.postDelayed(animator, delayTime);
			}
		}
	};

	private Runnable animator = new Runnable() {
		@Override
		public void run() {
			if (debugMode) Log.d(TAG, "entering animator");
			outputText.setText("Swipe Right");
			vibrateTimestamp = System.currentTimeMillis();
			String vibrateLog = "" + vibrateTimestamp + ",vibrate,0,0";
			swipeData.add(vibrateLog);
			showControls();
			synced = false;
			flashUIHandler.post(new TickerRunnable());
			experimentCounter++;
		}
	};

	private void swipeDetected(String direction) {
	    synced = true;
		if(debugMode) Log.d(TAG, "entering swipedetected");
		Long swipeTimestamp = System.currentTimeMillis();
		String swipeLog = "" + swipeTimestamp + ",swipe " + direction + ",0,0";

		outputText.setText("Swipe " + direction.substring(0, 1).toUpperCase() + direction.substring(1) + " Detected");

		isRecording = false;
		swipeData.add(swipeLog);

		saveSession();
		Log.v(TAG, "launching pauser");
		taskHandler.postDelayed(pauser, 1000);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		StringBuilder builder = new StringBuilder();
		builder.append(event.sensor.getStringType() + ",");
		builder.append(watchLineCounter + ",");
		builder.append(event.timestamp + ",");
		builder.append(System.currentTimeMillis() + ",");

		for (int i = 0; i < event.values.length; i++) {
			builder.append(event.values[i]);
			if (i != event.values.length - 1) builder.append(",");
		}
		builder.append("\n");

		String sensorReading = builder.toString();
		if(isRecording) {
			sensorData.add(sensorReading);
			watchLineCounter++;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}

	private class saveDataTask extends AsyncTask<Void, Void, Void> {

		private void writeSensorDataCSV(String filename, ArrayList<String> data) {
			Log.v("writeSensorData", "writing swipeData " + filename);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < data.size(); i++) {
				if (data.get(i) == null) {
					continue;
				}
				sb.append(data.get(i));
				sb.append("\n");
			}
			String CSVContent = sb.toString();

			File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Synchro/" + "P" + person + "/swipe/" + "t" + timestamp + "_p" + person + "_" + activity + "/data/");
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

			int sessionCounter = experimentCounter;

			// File setup
			String filePrefix = "t" + timestamp + "_p" + person + "_" + activity + "_x" + sessionCounter;
			String fileSuffix = ".csv";

			// Copy of data structures
			ArrayList<String> swipeDataCopy = new ArrayList<>(swipeData);
			ArrayList<String> sensorDataCopy = new ArrayList<>(sensorData);

			// Clear data structures
			swipeData.clear();
			sensorData.clear();
			watchLineCounter = 0;

			// Write files
			writeSensorDataCSV(filePrefix + "_" + "swipe" + fileSuffix, swipeDataCopy);
			writeSensorDataCSV(filePrefix + "_" + "wrist" + fileSuffix, sensorDataCopy);
			return null;
		}
	}

	private void saveSession() {
		new saveDataTask().execute();
	}

	private void hideControls() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (swipeView == null || outputText == null) {
					return;
				}
				swipeView.setVisibility(View.INVISIBLE);
				outputText.setVisibility(View.INVISIBLE);
				swipeView.resetCircleLocations();
			}
		});
	}

	private void showControls() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (swipeView == null || outputText == null) {
					return;
				}
				swipeView.setVisibility(View.VISIBLE);
				outputText.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		closeApp();
	}

	private void vibrate() {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(50);
	}

	private void vibrateShort() {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(50);
	}

	private void closeApp() {
		isRecording = false;
		taskHandler.removeCallbacks(animator);
		taskHandler.removeCallbacks(pauser);
		flashUIHandler.removeCallbacks(null);
		mSensorManager.unregisterListener(this);
		finish();
	}

	public int getPoissonRandom(double seconds) {
		Random r = new Random();
		double L = Math.exp(-seconds);
		int k = 0;
		double p = 1.0;
		do {
			p = p * r.nextDouble();
			k++;
		} while (p > L);
		return (k - 1) * 1000;
	}

	public class TickerRunnable implements Runnable {

		private long startTime;
		private long periodTime = 1000;

		public TickerRunnable() {
			startTime = System.currentTimeMillis();
		}

		public void run() {
			long lastTime = startTime;
			long postDrawTime = 0;
			long currentTime = 0;
			long waitTime = 0;
			for (int i = 0; i < 10; i++) {
				if (synced) {
					return;
				}
				long nextStartTime = startTime + (i * periodTime);
				currentTime = System.currentTimeMillis();
				lastTime = currentTime;
				runOnUiThread(sendVibration);
				postDrawTime = System.currentTimeMillis();
				waitTime = periodTime - (postDrawTime - nextStartTime);
				long cycleTime = periodTime + waitTime;
				if (Math.abs(cycleTime) > periodTime) cycleTime = periodTime;
				try {
					Thread.sleep(cycleTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

