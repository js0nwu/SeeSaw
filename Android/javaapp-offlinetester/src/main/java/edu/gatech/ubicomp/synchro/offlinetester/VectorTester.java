package edu.gatech.ubicomp.synchro.offlinetester;

import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Created by gareyes on 3/20/17.
 */

public class VectorTester {

	public static void main(String[] args) {
		Vector testing = new Vector3D(0, 3, 4);
		System.out.println(testing.getNorm());
		System.out.println(testing.getNorm1());
		System.out.println(testing.getNormInf());
		System.out.println(testing.getNormSq());
		System.out.println(testing.getZero());
		System.out.println(testing.dotProduct(new Vector3D(-1, -1, -1)));
		System.out.println(testing.dotProduct(new Vector3D(1, 1,  1)));
	}
}
