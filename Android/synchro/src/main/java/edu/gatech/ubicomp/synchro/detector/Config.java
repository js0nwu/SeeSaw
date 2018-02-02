package edu.gatech.ubicomp.synchro.detector;

/**
 * Created by gareyes on 3/1/17.
 */

public class Config {
	public static int SUBSAMPLE_SIZE = 0; // samples
	public static double SUBSAMPLE_TIME = 100; // ms
	public static int WINDOW_SIZE = 15; // samples
	public static boolean USE_FEATURE_VARIANCE = false;
	public static int VARIANCE_THRESHOLD = 85;
	public static boolean USE_TTEST = false;
	public static boolean USE_INPUTSIGNAL_EWMA = false;
	public static boolean USE_CORR_EWMA = true;
	public static boolean USE_XCORR = true;
	public static int CLUSTER_SIZE = 0;
	public static String FEATURE_MODE = "X";
	public static int XCORR_WINDOW_SIZE = WINDOW_SIZE / 2;
	public static double XCORR_MAX_VAR = 2000;
	public static double X_MIN_VAR = 0;
	public static double X_MAX_VAR = 100;
	public static double X_VAR_FACTOR = 0.4;
	public static int X_WINDOW_SIZE = WINDOW_SIZE / 2;
	public static boolean AUTOCORRELATION_MODE = false;
	public static int AUTOCORRELATION_WINDOW_SIZE = 10;
}