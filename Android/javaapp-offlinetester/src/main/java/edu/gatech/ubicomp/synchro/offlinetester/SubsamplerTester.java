package edu.gatech.ubicomp.synchro.offlinetester;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import edu.gatech.ubicomp.synchro.detector.Subsampler;
import edu.gatech.ubicomp.synchro.detector.TimeSubsampler;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

/**
 * Created by gareyes on 3/3/17.
 */

public class SubsamplerTester {

	// NOTE: This will (intentionally) not run as written so that folks
	// copy-pasting have to think about how to initialize their
	// Random instance.  Initialization of the Random instance is outside
	// the main scope of the question, but some decent options are to have
	// a field that is initialized once and then re-used as needed or to
	// use ThreadLocalRandom (if using at least Java 1.7).
	private static Random rand = new Random();

	public static double randInt(int min, int max) {
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		double randomNum = (double) rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	public static void main(String[] args) {

		Subsampler sampler = new TimeSubsampler(10);

		int min = 1;
		int max = 2;
		double lastNumber = 0;

		for(int i=0; i<100; i++) {

			double[] values = new double[3];

			// time
			double newNumber = i; //+ randInt(min,max);
			if(lastNumber >= newNumber) newNumber = lastNumber+randInt(min,max);
			lastNumber = newNumber;

			// sample
			for(int j=0; j<3; j++) {
				values[j] = randInt(1,10);
			}

			System.out.println("input: " + newNumber + "," + values[0] + "," + values[1] + "," + values[2]);
			Tuple2<double[]> result = sampler.processSample(newNumber, values);
			if(result != null) {
				System.out.print("output: ");
				for (int k = 0; k < result.getX().length; k++) {
					System.out.print(result.getX()[k]);
					System.out.print(",");
				}
				for (int k = 0; k < result.getY().length; k++) {
					System.out.print(result.getY()[k]);
					if(k!=result.getY().length-1) System.out.print(",");
				}
				System.out.println();
			} else {
				//System.out.println("null");
			}
			//System.out.println();
		}
	}
}
