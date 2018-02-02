package edu.gatech.ubicomp.synchro.livedatacollect;


import android.Manifest;

/**
 * Created by gareyes on 11/5/16.
 */

public class Config {
	public static int promptTime = 60; //seconds
	public static int[] flashFreq = {750, 1000, 1250};
	public static int numOfCycles = 15;
	public static String[] conditions = {"browsing", "sitting", "walking"};
	public static int numOfReps = 5;
	public static String[] APP_PERMISSIONS = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE};
}