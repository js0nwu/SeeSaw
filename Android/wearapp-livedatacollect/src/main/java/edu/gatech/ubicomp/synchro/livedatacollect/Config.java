package edu.gatech.ubicomp.synchro.livedatacollect;


import android.Manifest;

/**
 * Created by gareyes on 11/5/16.
 */

public class Config {
	public static String[] APP_PERMISSIONS = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE, Manifest.permission.WAKE_LOCK};
	public static int SESSION_PAUSE_TIME = 10; //seconds
    public static int[] FLASH_FREQ = {1000};
	public static String[] ACTIVITY_LIST = {"prep", "sit", "walk", "debug", "noise", "subtle1", "subtle2", "subtle3", "activate", "adebug"};
	public static int NUM_REPS = 5;
	public static int NUM_CYCLES = 10;
	public static double SAMPLING_RATE = 100; // Hz
	public static double ACCELGYRO_SAMPLING_RATE = 25; // Hz
	public static int ICON_HEIGHT = 96;
	public static int ICON_WIDTH = 96;
	public static boolean AUTO_LAUNCH_APP = false;
	public static boolean DEBUG_VIZ = false;
	public static boolean USE_TRIGGER_THRESHOLD = true;
	public static double TRIGGER_THRESHOLD = 1.0;
	public static double TRIGGER_WAITTIME = 0;
	public static boolean NOISE_MODE = false;
	public static boolean AWAKE_MODE = false;
	public static double SUBTLE_FACTOR = 1;
	public static int SUBTLE_BOUND_LENGTH = 40;
	public static int SUBTLE_BOUND_HEIGHT = 120;
	public static long CROSS_PENALTY = 1000;
}