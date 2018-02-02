package edu.gatech.ubicomp.synchro.livedatacollect;

import java.util.Random;

/**
 * Created by gareyes on 2/21/17.
 */

public class Utils {

	public static int getPoissonRandom(double seconds) {
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
}
