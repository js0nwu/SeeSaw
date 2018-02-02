package edu.gatech.ubicomp.synchro.livedatacollect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;

import edu.gatech.ubicomp.synchro.detector.EventRecognitionListener;
import edu.gatech.ubicomp.synchro.detector.SynchroDetector;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SynchroActivity extends Activity implements SensorEventListener {

	private String TAG = this.getClass().getSimpleName();

	private boolean debugMode = false;

	private SynchroView synchroView;
	private LineChartView lineChartView;

	private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private ArrayList<String> sensorData = new ArrayList<>();
	private ArrayList<String> corrData = new ArrayList<>();
	private ArrayList<String> syncTimestampData = new ArrayList<>();

	private String corrDataOutput = "";

    private boolean isRecording = true;

    private TextView counterText;

	private String timestamp;
	private String person;
	private String activity;
	private boolean activate;

    private int cycles = 0;

    private int lineCounter = 0;
	private int corrCounter = 0;
    private int experimentCounter = 0;

    private boolean isLeft = true;

	private HandlerThread flashUIThread;
    private Handler flashUIHandler;

	private HandlerThread sensorDataThread;
	private Handler sensorDataHandler;

	private SynchroDetector syncDetector = null;

    private double lastDetectorSensorStart = 0;
    private double lastSensorTime = 0;
    private double lastConvertedTS = 0;
    private double lastDetectorStart = 0;
	private long lastStartTime = 0;

    private boolean isRunning = false;

    private List<Double> accelWindow = new ArrayList<>();

	private Long beginTimestamp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {


		super.onCreate(savedInstanceState);
		if(debugMode) Log.d(TAG, "oncreate");
		if(Config.DEBUG_VIZ) {
			setContentView(R.layout.activity_synchro_debug);
			lineChartView = (LineChartView) findViewById(R.id.linechart);
		} else {
			setContentView(R.layout.activity_synchro);
		}
		synchroView = (SynchroView) findViewById(R.id.synchroView);
		counterText = (TextView) findViewById(R.id.counterText);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		init();
	}

	private void init() {
		if(debugMode) Log.d(TAG, "init");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final Intent sessionIntent = getIntent();
		timestamp = sessionIntent.getStringExtra("timestamp");
		person = sessionIntent.getStringExtra("person");
		activity = sessionIntent.getStringExtra("activity");
		activate = sessionIntent.getBooleanExtra("activate", false);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
			magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			//Log.d("magnetometer", "magnetometer available in this Android device");
		}
		else {
			//Log.d("magnetometer", "no magnetometer in this Android device");
		}
		if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
			gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			//Log.d("gyro", "gyroscope available in this Android device");
		}
		else {
			//Log.d("gyro", "no gyroscope in this Android device");
		}
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			//Log.d("accel", "accelerometer available in this Android device");
		}
		else {
			//Log.d("accel", "no accelerometer in this Android device");
		}

		// Sensor data thread
		sensorDataThread = new HandlerThread("SensorData", Process.THREAD_PRIORITY_DEFAULT);
		sensorDataThread.start();
		sensorDataHandler = new Handler(sensorDataThread.getLooper());

		// Register listener
		double samplingPeriodUs = Math.pow(10, 6) / Config.SAMPLING_RATE;
//		sensorManager.registerListener(this, magnetometer, (int)samplingPeriodUs, sensorDataHandler);
//		sensorManager.registerListener(this, accelerometer, (int) samplingPeriodUs, sensorDataHandler);
        sensorManager.registerListener(this, gyroscope, (int) samplingPeriodUs, sensorDataHandler);
		//sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL, sensorDataHandler);

		// Init the detector
		initializeDetector();

		// Handler for flashing
		flashUIThread = new HandlerThread("FlashUI", Process.THREAD_PRIORITY_FOREGROUND);
		flashUIThread.start();
		flashUIHandler = new Handler(flashUIThread.getLooper());
		flashUIHandler.post(pauser);
	}

	private void initializeDetector() {
		if(syncDetector != null) syncDetector.isRunning = false;
		syncDetector = new SynchroDetector(true);
		syncDetector.setActivationMode(activate);
		syncDetector.setEventRecognitionListener(new EventRecognitionListener() {
			float corrTimestamp = 0;
			float corrValue = 0;
			int corrCounter = 0;
			String direction = "";
			Long timeRecognized = null;
			@Override
			public void onEventRecognized(final String result) {
				timeRecognized = System.currentTimeMillis();
				String[] resultSplit = result.split(",");
				corrTimestamp = Float.valueOf(resultSplit[0]);
				corrValue = Float.valueOf(resultSplit[1]);
				corrCounter = Integer.valueOf(resultSplit[2]);
				direction = resultSplit[3];
				Log.d(TAG, "oneventrecognized " + result);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(Config.DEBUG_VIZ) {
							lineChartView.addPoint(corrValue);
							counterText.setText(""+corrValue);
						}
						synchroView.setFeedback(corrValue, direction);
						if(Config.USE_TRIGGER_THRESHOLD) {
							System.out.println(timeRecognized - beginTimestamp);
							if(timeRecognized-beginTimestamp >= Config.TRIGGER_WAITTIME) {
								long crossDiff = System.currentTimeMillis() - synchroView.getLastCross();
								if (corrValue >= Config.TRIGGER_THRESHOLD && crossDiff > Config.CROSS_PENALTY) {
									synchroView.setVisibility(View.INVISIBLE);
									if(syncTimestampData.size() == 1) {
										counterText.setText("Sync " + direction.substring(0, 1).toUpperCase() + direction.substring(1) + " Detected");
										corrData.add(corrTimestamp + "," + corrValue + "," + corrCounter + "," + "sync " + direction);
										syncTimestampData.add(timeRecognized + "," + "sync " + direction + "," + result);
									}
								}
							}
						}
					}
				});
				corrData.add(result);
				corrCounter++;
			}
		});

		new Thread(syncDetector).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(debugMode) Log.d(TAG, "onresume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(debugMode) Log.d(TAG, "onpause");
		closeApp();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(debugMode) Log.d(TAG, "onstop");
//		closeApp();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(debugMode) Log.d(TAG, "ondestroy");
	}

	public Runnable drawLeftCircle = new Runnable() {
		@Override
		public void run() {
			synchroView.drawLeftCircle();
		}
	};

	public Runnable drawRightCircle = new Runnable() {
		@Override
		public void run() {
			synchroView.drawRightCircle();
		}
	};

	private class TickerRunnable implements Runnable {
		private String experiment;
		private int periodTime;
		private int currentTicks;
		private int totalTicks;
		private long startTime;
		private long waitTime;
		private long postDrawTime;


		public TickerRunnable(String experiment, int periodTime, int currentTicks, int totalTicks) {
			this.experiment = experiment;
			this.periodTime = periodTime;
			this.currentTicks = currentTicks;
			this.totalTicks = totalTicks;
			this.startTime = System.currentTimeMillis();
		}

		@Override
		public void run() {
			if(debugMode) Log.d(TAG, "starting tickerrunnable");
			long lastTime = startTime;
			long currentTime = 0;
			for (int i = 0; i < Config.NUM_CYCLES * 2; i++) {
				long nextStartTime = startTime + (periodTime * i);

				currentTime = System.currentTimeMillis();
				long actualTime = currentTime - lastTime;
				lastTime = currentTime;
				if(debugMode) Log.d(TAG, "cycle# " + i);
				if (isLeft) {
					String sensorStringLeft = "" + System.currentTimeMillis() + ",left," + lastConvertedTS;
					runOnUiThread(drawLeftCircle);
					postDrawTime = System.currentTimeMillis();
					sensorData.add(sensorStringLeft);
				} else {
					String sensorStringRight = "" + System.currentTimeMillis() + ",right," + lastConvertedTS;
					runOnUiThread(drawRightCircle);
					postDrawTime = System.currentTimeMillis();
					sensorData.add(sensorStringRight);
					cycles++;
				}
				if (syncDetector != null) {
					if(isLeft) syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
					else syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
				}
				isLeft = !isLeft;

				waitTime = periodTime - (postDrawTime - nextStartTime);
				long cycleTime = periodTime + waitTime;
				if (Math.abs(cycleTime) > periodTime) cycleTime = periodTime;
				if(debugMode) Log.d(TAG, "period: " + periodTime + ", actual: "  + actualTime + ", waittime: " + waitTime + ", newcycletime: " + cycleTime);
				lastStartTime = startTime;
				try {
					Thread.sleep(cycleTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(debugMode) Log.d(TAG, "ending tickerrunnable");
			if(debugMode) Log.d(TAG, "launching pauser");
			isRunning = false;
			String endTimeStamp = "" + System.currentTimeMillis() + ",end";
			sensorData.add(endTimeStamp);
			saveSensorData();
			saveCorrData();
			saveSyncData();
			flashUIHandler.postDelayed(pauser, 1000);
		}
	}

	private Runnable pauser = new Runnable() {
		@Override
		public void run() {
			if(debugMode) Log.d(TAG, "pauser");
			if(activity.equals("prep") && experimentCounter == 5) {
				closeApp();
				return;
			}
			if (experimentCounter == Config.NUM_REPS) {
				if(debugMode) Log.d(TAG, "closing app");
				closeApp();
				return;
			} else {
				hideControls();
				System.out.println("size of buffer: " + syncDetector.dataBuffer.size());
				syncDetector.isRunning = false;
				syncDetector = null;
				isRunning = true;
				if(debugMode) Log.v(TAG, "resetting syncDetector");
				int delayTime = 0;
				if(activity.equals("prep")) {
					delayTime = 3000;
				} else {
					delayTime = Utils.getPoissonRandom(Config.SESSION_PAUSE_TIME);
				}
				flashUIHandler.postDelayed(animator, delayTime);
			}
		}
	};

	private void hideControls() {
		if(debugMode) Log.d(TAG, "hidecontrols");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (synchroView == null || counterText == null) {
					return;
				}
				if(Config.DEBUG_VIZ) {
					if(lineChartView == null) {
						return;
					}
					lineChartView.clearViz();
				} else {
					synchroView.setVisibility(View.INVISIBLE);
					counterText.setVisibility(View.INVISIBLE);
				}
				synchroView.clearCircles();
			}
		});
	}

	private Runnable animator = new Runnable() {
        @Override
        public void run() {
			if(debugMode) Log.d(TAG, "animator");
            runTickSequence();
        }
    };

	private void runTickSequence() {
		//syncDetector.reset();
		initializeDetector();
		lastDetectorStart = 0;
		lastDetectorSensorStart = lastSensorTime;
		beginTimestamp = System.currentTimeMillis();
		String beginString = "" + beginTimestamp + ",begin " + Config.FLASH_FREQ[0];
		syncTimestampData.add(beginString);
		sensorData.add(beginString);
		vibrate();
		isLeft = true;
		cycles = 0;
		showControls();
		flashUIHandler.postDelayed(new TickerRunnable("" + experimentCounter, Config.FLASH_FREQ[0] / 2, 1, Config.NUM_CYCLES * 2), 0);
		experimentCounter++;
		if(debugMode) Log.d(TAG, "experimentCounter: " + experimentCounter);
	}

	private void showControls() {
		if (Config.NOISE_MODE) {
			return;
		}
		if(debugMode) Log.d(TAG, "showcontrols");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (synchroView == null || counterText == null) {
					return;
				}
				if(Config.DEBUG_VIZ) {
					if(lineChartView == null) {
						return;
					}
					lineChartView.setVisibility(View.VISIBLE);
				}
				if (Config.AWAKE_MODE) {
					counterText.setText("Awake Me");
				} else {
					counterText.setText("Sync Right");
				}
				synchroView.setVisibility(View.VISIBLE);
				counterText.setVisibility(View.VISIBLE);
			}
		});
	}

	private void writeDataCSV(String filename, ArrayList<String> data) {
		Log.v(TAG, "writing data " + filename);
		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Synchro/" + "P" + person + "/synchro/" + "t" + timestamp + "_p" + person + "_" + activity + "/data/");
		if (!dir.exists()) {dir.mkdirs();} // Create folder if needed
		File outputFile = new File(dir, filename);
		Log.v(TAG, "outputFile: " + outputFile.getAbsolutePath());
		try {
			FileUtils.writeLines(outputFile, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "data write successful");
	}

	private class SaveSensorsTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {
            String filePrefix = "t" + timestamp + "_p" + person + "_" + activity + "_x" + experimentCounter + "_" + "raw";
            String fileSuffix = ".csv";
            ArrayList<String> sensorDataCopy = new ArrayList<>(sensorData);
			lineCounter = 0;
            sensorData.clear();
            writeDataCSV(filePrefix + fileSuffix, sensorDataCopy);
            return true;
        }

		protected void onPostExecute(Boolean result) {
			Log.d(TAG, "completed save sensor data " + result);
		}
    }

	private class SaveCorrelationTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... voids) {
			String filePrefix = "t" + timestamp + "_p" + person + "_" + activity + "_x" + experimentCounter + "_" + "corr";
			String fileSuffix = ".csv";
			ArrayList<String> corrDataCopy = new ArrayList<>(corrData);
			corrCounter = 0;
			corrData.clear();
			writeDataCSV(filePrefix + fileSuffix, corrDataCopy);
			return true;
		}

		protected void onPostExecute(Boolean result) {
			Log.d(TAG, "completed save correlation data " + result);
		}
	}

	private class SaveSyncTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... voids) {
			String filePrefix = "t" + timestamp + "_p" + person + "_" + activity + "_x" + experimentCounter + "_" + "sync";
			String fileSuffix = ".csv";
			ArrayList<String> syncDataCopy = new ArrayList<>(syncTimestampData);
			syncTimestampData.clear();
			writeDataCSV(filePrefix + fileSuffix, syncDataCopy);
			return true;
		}

		protected void onPostExecute(Boolean result) {
			Log.d(TAG, "completed save sync data " + result);
		}
	}

    private void saveSensorData() {
        new SaveSensorsTask().execute();
    }
	private void saveCorrData() {
		new SaveCorrelationTask().execute();
	}
	private void saveSyncData() {
		new SaveSyncTask().execute();
	}

	@Override
    public void onSensorChanged(SensorEvent sensorEvent) {

		if(isRunning) {

			String sensorString = "" + System.currentTimeMillis() + ",";

			if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				//TODO: are these timestamps used?
				lastSensorTime = sensorEvent.timestamp;
				if (lastDetectorSensorStart == -1) {
					lastDetectorSensorStart = lastSensorTime;
				}
				double convertTimestamp = lastSensorTime / 1000000;
				String ssInput = "" + convertTimestamp + ",";
				//Log.d("onsensorchangedts", ""+convertTimestamp);
				lastConvertedTS = convertTimestamp;

				sensorString += "gyro,";
				for (int i = 0; i < sensorEvent.values.length; i++) {
					sensorString += sensorEvent.values[i] + ",";
					ssInput += sensorEvent.values[i] + ",";
				}
				sensorString += "" + convertTimestamp + ",";

				if (syncDetector != null && isRunning) {
					double[] ta = new double[]{convertTimestamp};
					double[] sa = new double[sensorEvent.values.length];
					for (int i = 0; i < sensorEvent.values.length; i++) {
						sa[i] = sensorEvent.values[i];
					}
					Tuple2<double[]> detectTuples = new Tuple2<>(ta, sa);
					synchroView.setGyroDraw(sa[0]);
					syncDetector.dataBuffer.offer(detectTuples);
				}
			}
			else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				sensorString += "magnet,";
				for (int i = 0; i < sensorEvent.values.length; i++) {
					sensorString += sensorEvent.values[i] + ",";
				}
				sensorString += "" + sensorEvent.timestamp / 1000000 + ",";
			} else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				sensorString += "accel,";
				for (int i = 0; i < sensorEvent.values.length; i++) {
					sensorString += sensorEvent.values[i] + ",";
				}
				sensorString += "" + sensorEvent.timestamp / 1000000 + ",";
			}
			sensorString += sensorEvent.timestamp + ",";
			sensorString += lineCounter;
			sensorData.add(sensorString);
			lineCounter++;
		}
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void vibrate() {
		if (Config.NOISE_MODE) {
			return;
		}
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    private void closeApp() {
		if (Config.NOISE_MODE) {
		    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		    v.vibrate(500);
		}
		if(debugMode) Log.d(TAG, "closeapp");
        try {
			isRunning = false;
			syncDetector.isRunning = false;
			flashUIHandler.removeCallbacksAndMessages(null);
			sensorDataHandler.removeCallbacksAndMessages(null);
			sensorManager.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	finish();
		}
	}
}