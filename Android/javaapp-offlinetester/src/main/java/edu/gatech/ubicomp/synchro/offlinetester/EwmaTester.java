package edu.gatech.ubicomp.synchro.offlinetester;

import java.util.Random;

import edu.gatech.ubicomp.synchro.detector.ExponentialMovingAverage;
import edu.gatech.ubicomp.synchro.detector.Subsampler;
import edu.gatech.ubicomp.synchro.detector.TimeSubsampler;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

/**
 * Created by gareyes on 3/3/17.
 */

public class EwmaTester {

	// NOTE: This will (intentionally) not run as written so that folks
	// copy-pasting have to think about how to initialize their
	// Random instance.  Initialization of the Random instance is outside
	// the main scope of the question, but some decent options are to have
	// a field that is initialized once and then re-used as needed or to
	// use ThreadLocalRandom (if using at least Java 1.7).
	private static Random rand = new Random();
	private static int NUM_POINTS = 10;
	private static double ALPHA = 0.1;

	public static double randInt(int min, int max) {
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		double randomNum = (double) rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	public static void main(String[] args) {

		double[] values = new double[NUM_POINTS];

		for (int i = 0; i < NUM_POINTS; i++) {
			values[i] = randInt(1, 10);
			System.out.print(values[i] + ",");
		}
		System.out.println();

		double[] javaValues = getEWMA(values, ALPHA);

		for (int i = 0; i < javaValues.length; i++) {
			System.out.print(javaValues[i] + ",");
		}
		System.out.println();

		double[] androidValues = getAndroidEWMA(values, ALPHA);

		for (int i = 0; i < androidValues.length; i++) {
			System.out.print(androidValues[i] + ",");
		}
		System.out.println();
	}

	private static double[] getEWMA(double[] dataPoints, double alpha) {
		double[] ewmaPoints = new double[dataPoints.length];
		ExponentialMovingAverage ewma = new ExponentialMovingAverage(alpha);
		for (int i = 0; i < dataPoints.length; i++) {
			ewmaPoints[i] = ewma.average(dataPoints[i]);
		}
		return ewmaPoints;
	}

	private static double[] getAndroidEWMA(double[] dataPoints, double alpha) {
		double[] ewmaPoints = new double[dataPoints.length];
		double avgValue = 0;
		for (int i = 0; i < dataPoints.length; i++) {
			if(i==0) {
				avgValue = dataPoints[i];
			} else {
				avgValue = (alpha * avgValue) + ((1 - alpha) * dataPoints[i]);
			}
			ewmaPoints[i] = avgValue;
		}
		return ewmaPoints;
	}
}
