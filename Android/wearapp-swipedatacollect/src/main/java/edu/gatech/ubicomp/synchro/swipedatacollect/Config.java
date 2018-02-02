package edu.gatech.ubicomp.synchro.swipedatacollect;


import android.Manifest;

/**
 * Created by gareyes on 11/5/16.
 */

public class Config {
	public static String[] ACTIVITY_LIST = {"prep", "sit", "walk"};
	public static String[] APP_PERMISSIONS = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE};
	public static float ICON_WIDTH = 96;
	public static float ICON_HEIGHT = 96;
	public static int NUM_REPS = 5;
	public static double SAMPLING_RATE = 25;
	private static float X_SENSITIVITY = 125; // percentage
	private static float Y_SENSITIVITY = 75; // percentage
	public static float X_FACTOR = X_SENSITIVITY/100; // multiplier
	public static float Y_FACTOR = Y_SENSITIVITY/100; // multiplier
	public static int promptTime = 10; //seconds
	public static final int RANDOM_WALK_TIME = 5; // seconds
	public static final int RANDOM_SIT_TIME = 1; // seconds
	public static boolean SHOW_TOUCH_ZONE = false;
}