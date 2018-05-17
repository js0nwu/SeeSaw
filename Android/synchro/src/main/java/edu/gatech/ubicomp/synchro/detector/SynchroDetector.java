package edu.gatech.ubicomp.synchro.detector;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static edu.gatech.ubicomp.synchro.detector.Utils.logOut;

public class SynchroDetector extends Detector<Tuple2<double[]>> implements Runnable {

	private String TAG = this.getClass().getSimpleName();

	private double inputEWMA = 0.04;

	private boolean debugMode = false;

	private List<Double> sensorTimes = new ArrayList<>();
	private List<double[]> sensorWindow = new ArrayList<>();

	private List<Double> leftSyncs = new ArrayList<>();
	private List<Double> rightSyncs = new ArrayList<>();

	private List<double[]> nextLefts = new ArrayList<>();
	private List<double[]> nextRights = new ArrayList<>();

	private boolean leftSynced = false;
	private boolean rightSynced = false;

	private double detectThreshold = 1.0;

	private double vectorLength = 0;

	private final double TWO_PI = 2 * Math.PI;
	private final double ARCSIN_ONE = Math.PI / 2;

	public boolean isRunning = true;
	private boolean detectorStatus;

	public int referencePeriod = -1;
	public int windowSize = Config.WINDOW_SIZE;

	private Map<Double, Double> featureMap = new LinkedHashMap<>();
	private Map<Double, String[]> windowsMap = new LinkedHashMap<>();
	private Map<Double, String> lagMap = new LinkedHashMap<>();
	private Map<Double, Double> lagTimeMap = new LinkedHashMap<>();
	private Map<Double, String> subsampleMap = new LinkedHashMap<>();
	private Map<Double, String> correlationMap = new LinkedHashMap<>();
	private Map<Double, String> magnitudeMap = new LinkedHashMap<>();
	private Map<Double, String> syncThresholdMap = new LinkedHashMap<>();
	private Map<Double, String> windowTimeMap = new LinkedHashMap<>();

	private double projectMagnitude = 0;
	private String windowTimeString = null;

	public BlockingQueue<Tuple2<double[]>> dataBuffer;

	private EventRecognitionListener listener;

	private Subsampler detectorSampler;
	private int correlationCounter = 0;

	private Vector projectLeftVector = null;
	private Vector projectRightVector = null;
	private Vector projectMeanVector = null;

	private Double projectLeftVectorTimestamp = 0.0;
	private Double projectRightVectorTimestamp = 0.0;

	private List<Double> windowTimestamps = new ArrayList<>();
	private List<double[]> windowVectors = new ArrayList<>();
	private List<String> directions = new ArrayList<>();

	private String syncDirection = "null";

	private ExponentialMovingAverage corrEWMA = new ExponentialMovingAverage(0.35);

	private boolean keepData = true;

	public SynchroDetector(boolean isOnline) {
		this.detectorStatus = isOnline;

		dataBuffer = new LinkedBlockingQueue<>();
		listener = null;

		if (Config.SUBSAMPLE_TIME > 0) {
			if (debugMode) logOut(TAG, "New timesubsampler");
			detectorSampler = new TimeSubsampler(Config.SUBSAMPLE_TIME);
		} else if (Config.SUBSAMPLE_SIZE > 0) {
			if (debugMode) logOut(TAG, "New countsubsampler");
			detectorSampler = new CountSubsampler(Config.SUBSAMPLE_SIZE);
		} else {
			if (debugMode) logOut(TAG, "Null subsampler");
			detectorSampler = null;
		}
	}

	public void setActivationMode(boolean b) {
		Config.AUTOCORRELATION_MODE = b;
		if (b) {
			Config.X_MIN_VAR = 1;
		} else {
			Config.X_MIN_VAR = 0.2;
		}
	}

	private double clipValue(double value, double min, double max) {
		return Math.min(Math.max(value, min), max);
	}

	// an inefficient way to shuffle a list, instead of fisher-yates
	private double[] shuffleArray(double[] array) {
		List<Double> arr = new ArrayList<>();
		for (int i = 0; i < array.length; i++) {
			arr.add(array[i]);
		}
		Collections.shuffle(arr);
		double[] shuffledArray = new double[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			shuffledArray[i] = arr.get(i).doubleValue();
		}
		return shuffledArray;
	}

	private double[][] generateRandomPermutations(double[] originalArray, int N) {
		double[][] randomPermutations = new double[N][];
		for (int i = 0; i < N; i++) {
			double[] shuffledCopy = shuffleArray(originalArray);
			randomPermutations[i] = shuffledCopy;
		}
		return randomPermutations;
	}
	private double angleBetween(Vector a, Vector b) {
		return Math.acos(clipValue((a.dotProduct(b)) / (a.getNorm() * b.getNorm()), -1.0, 1.0));
	}

	public double detectValue(Tuple2<double[]> data) {
		if (Double.isNaN(data.getX()[0])) {
			return 0;
		}
		for (double d : data.getY()) {
			if (Double.isNaN(d)) {
				return 0;
			}
		}

		// Add timestamp and vectors for xcorr window
		sensorTimes.add(data.getX()[0]);
		sensorWindow.add(data.getY());

		// Sliding window
		while (sensorTimes.size() > windowSize) {
			sensorTimes.remove(0);
			sensorWindow.remove(0);
		}


//		 Add minimum window size
//		if (sensorTimes.size() < 30) {
//			return 0;
//		}

		// Get timestamps for window of data
		double[] windowTimes = new double[sensorTimes.size()];
		for (int i = 0; i < windowTimes.length; i++) {
			windowTimes[i] = sensorTimes.get(i);
		}
		if (windowTimes.length > 0) {
		    double minWindow = windowTimes[0];
		    double maxWindow = windowTimes[0];
		    for (int i = 0; i < windowTimes.length; i++) {
		        double currentVal = windowTimes[i];
		        if (currentVal < minWindow) {
		        	minWindow = currentVal;
				}
				if (currentVal > maxWindow) {
		        	maxWindow = currentVal;
				}
			}
			windowTimeString = "" + minWindow + "," + maxWindow;
		}

		// Get values for window of data
		double[][] windowValues = new double[sensorWindow.size()][];
		for (int i = 0; i < windowValues.length; i++) {
			windowValues[i] = sensorWindow.get(i);
		}

		// Add timestamp and vectors for vector calculation window
		windowTimestamps.add(data.getX()[0]);
		windowVectors.add(data.getY());

		// Data for left and right syncs
		if (leftSynced) {
			nextLefts.add(data.getY());
			if(debugMode) System.out.println("left synced");

			if(leftSyncs.size() > 1) {
				if(debugMode) System.out.println("left sync after first cycle");

//				System.out.println(windowVectors.size());

				// calculate the projection vector
				double[] meanVectorArray = new double[]{0.0, 0.0, 0.0};
				for (int i = 0; i < windowVectors.size(); i++) {
					meanVectorArray[0] += windowVectors.get(i)[0];
					meanVectorArray[1] += windowVectors.get(i)[1];
					meanVectorArray[2] += windowVectors.get(i)[2];
				}
				meanVectorArray[0] /= windowVectors.size();
				meanVectorArray[1] /= windowVectors.size();
				meanVectorArray[2] /= windowVectors.size();
				Vector meanVector = new Vector3D(meanVectorArray);

//				double maxAngle = -1;
//				Vector firstVector = null;
//				Double firstVectorTime = 0.0;
//				for (int i = 0; i < windowVectors.size() / 2; i++) {
//					Vector currentVector = new Vector3D(windowVectors.get(i));
//					Double currentVectorTime = new Double(windowTimestamps.get(i));
//					double currentAngle = angleBetween(meanVector, currentVector);
//					if (currentAngle > maxAngle) {
//						maxAngle = currentAngle;
//						firstVector = currentVector;
//						firstVectorTime = currentVectorTime;
//					}
//				}
//
//				maxAngle = -1;
//				Vector secondVector = null;
//				Double secondVectorTime = 0.0;
//				for (int i = windowVectors.size() / 2; i < windowVectors.size(); i++) {
//					Vector currentVector = new Vector3D(windowVectors.get(i));
//					Double currentVectorTime = new Double(windowTimestamps.get(i));
//					double currentAngle = angleBetween(meanVector, currentVector);
//					if (currentAngle > maxAngle) {
//						maxAngle = currentAngle;
//						secondVector = currentVector;
//						secondVectorTime = currentVectorTime;
//					}
//				}

				double maxAngle = -1;
				Vector firstVector = null;
				Double firstVectorTime = 0.0;
				for (int i = 0; i < windowVectors.size(); i++) {
					Vector currentVector = new Vector3D(windowVectors.get(i));
					Double currentVectorTime = new Double(windowTimestamps.get(i));
					double currentAngle = angleBetween(meanVector, currentVector);
					if (currentAngle > maxAngle) {
						maxAngle = currentAngle;
						firstVector = currentVector;
						firstVectorTime = currentVectorTime;
					}
				}

				double secondMaxAngle = -1;
				Vector secondVector = null;
				Double secondVectorTime = 0.0;
				for (int i = 0;  i < windowVectors.size(); i++) {
					Vector currentVector = new Vector3D(windowVectors.get(i));
					Double currentVectorTime = new Double(windowTimestamps.get(i));
					double currentAngle = angleBetween(firstVector, currentVector);
					if (currentAngle > secondMaxAngle) {
						secondMaxAngle = currentAngle;
						secondVector = currentVector;
						secondVectorTime = currentVectorTime;
					}
				}

				if (secondVectorTime < firstVectorTime) {
					Vector tmpFirstVector = firstVector;
					Double tmpFirstVectorTime = firstVectorTime;
					firstVector = secondVector;
					firstVectorTime = secondVectorTime;
					secondVector = tmpFirstVector;
					secondVectorTime = tmpFirstVectorTime;
				}

				projectLeftVector = firstVector;
				projectRightVector = secondVector;
				projectMeanVector = meanVector;

				projectLeftVectorTimestamp = firstVectorTime;
				projectRightVectorTimestamp = secondVectorTime;

				if (projectLeftVector == null || projectRightVector == null || projectMeanVector == null) {
					return 0;
				}
				windowVectors.clear();

				if(debugMode) System.out.println("left sync inside feature");

				for (int i = 0; i < windowValues.length; i++) {
					Double featureKey = new Double(windowTimes[i]);
					if (featureMap.containsKey(featureKey)) {
						Vector windowVector;
						windowVector = new Vector3D(windowValues[i]);
						double featureValue = deltaProjectTransform(projectLeftVector, projectRightVector, projectMeanVector, windowVector);
						double syncThresholdValue = deltaVectorThreshold(projectLeftVector, projectRightVector, projectMeanVector);
						featureMap.put(featureKey, featureValue);
						syncThresholdMap.put(featureKey, ""+syncThresholdValue);
					}
				}
			}
			leftSynced = false;
		}
		if (Config.FEATURE_MODE.equals("X") && (projectLeftVector == null || projectRightVector == null || projectMeanVector == null)) {
		    return 0;
		}
		if (rightSynced) {
			if(debugMode) System.out.println("right synced");
			nextRights.add(data.getY());
			rightSynced = false;

		}

		// Return at beginning with empty buffers
		if (nextLefts.isEmpty() || nextRights.isEmpty()) {
			return 0;
		}

		// Calculate signal period
		double signalPeriod = Math.abs(leftSyncs.get(leftSyncs.size() - 1) - rightSyncs.get(rightSyncs.size() - 1)) * 2;
		if (debugMode) System.out.println("signal period" + "" + signalPeriod);

		// Get offset using last left sync
		double offset = leftSyncs.get(leftSyncs.size() - 1);
		if (debugMode) System.out.println("signal offset" + "" + offset);

		projectMagnitude = (projectLeftVector.subtract(projectRightVector)).getNorm();

		double detectResult = detectSync(windowTimes, windowValues, signalPeriod, offset + 0.15 * signalPeriod);
		if (Config.USE_CORR_EWMA) {
			detectResult = corrEWMA.average(detectResult);
		}
//		System.out.println("dr: " + detectResult);
		return detectResult;
	}

	private double deltaSpaceTransform(Vector leftVector, Vector rightVector, Vector currentVector, boolean lastLeft) {

		// Feature using left minus right delta vector
		Vector deltaVector = leftVector.subtract(rightVector);
		Vector currentDeltaVector = null;
		if (lastLeft) {
			currentDeltaVector = currentVector.subtract(leftVector);
		} else {
			currentDeltaVector = currentVector.subtract(rightVector);
		}
		double scalarProjection = currentDeltaVector.dotProduct(deltaVector) / deltaVector.getNorm();
		return scalarProjection;
	}

	private double deltaSpaceMeanTransform(Vector leftVector, Vector rightVector, Vector currentVector, boolean lastLeft) {

		// Feature using mean of left and right vector
		Vector deltaVector = leftVector.subtract(rightVector);
		Vector meanVector = leftVector.add(rightVector).scalarMultiply(0.5);
		Vector currentDeltaVector = null;
		currentDeltaVector = currentVector.subtract(meanVector);
		double scalarProjection = currentDeltaVector.dotProduct(deltaVector) / deltaVector.getNorm();
		return scalarProjection;
	}

	private double scalarProject(Vector a, Vector b) {
		return a.dotProduct(b) / a.getNorm();
	}

	private double deltaProjectTransform(Vector leftVector, Vector rightVector, Vector meanVector, Vector currentVector) {
		if (Config.FEATURE_MODE.equals("X")) {
			Vector3D currentVector3D = (Vector3D) currentVector;
			return currentVector3D.getX();
		} else {
			// Feature using the average vector sweep and recalculated right/left vectors
			Vector deltaVector = leftVector.subtract(rightVector);
			double leftMost = scalarProject(meanVector, leftVector.subtract(meanVector));
			double rightMost = scalarProject(meanVector, rightVector.subtract(meanVector));
			vectorLength = Math.abs(leftMost - rightMost);
			Vector currentDeltaVector = null;
			currentDeltaVector = currentVector.subtract(meanVector);
			double scalarProjection = scalarProject(meanVector, currentDeltaVector);
			return scalarProjection;

		}
	}

	private double deltaVectorThreshold(Vector leftVector, Vector rightVector, Vector meanVector) {
		Vector leftSide = leftVector.subtract(meanVector);
		Vector rightSide = meanVector.subtract(rightVector);
		Vector indicatorVector = leftSide.subtract(rightSide);
		return indicatorVector.getNorm();
	}

	private double[] removeTrend(double[] xPoints, double[] yPoints) {
		SimpleRegression simpleRegression = new SimpleRegression(true);
		for (int i = 0; i < xPoints.length; i++) {
			simpleRegression.addData(xPoints[i], yPoints[i]);
		}
		double[] detrendedPoints = new double[xPoints.length];
		for (int i = 0; i < xPoints.length; i++) {
			detrendedPoints[i] = yPoints[i] - (simpleRegression.getSlope() * xPoints[i] + simpleRegression.getIntercept());
		}
		return detrendedPoints;
	}

	private double findTimeshiftXcorrRatio(double[] referenceTS, double[] inputTS, int limit) {
		if(Config.USE_XCORR) {
			double[] crossCorrelation = DSP.xcorr(referenceTS, inputTS);
//			System.out.println("cclength: " + crossCorrelation.length + " rtslength: " + referenceTS.length + " itslength: " + inputTS.length);
			int startIndex = crossCorrelation.length / 2 - limit + 1;
			int maxIndex = startIndex;
			int minIndex = startIndex;
			double maxValue = crossCorrelation[startIndex];
			double minValue = crossCorrelation[startIndex];
			for (int i = 0; i < limit * 2 - 1; i++) {
				int corrIndex = startIndex + i;
//				System.out.println("corrIndex: " + corrIndex);
				double currentValue = crossCorrelation[corrIndex];
				if (currentValue > maxValue) {
					maxValue = currentValue;
					maxIndex = corrIndex;
				}
				if (currentValue < minValue) {
					minValue = currentValue;
					minIndex = corrIndex;
				}
			}
			int shiftFactor = referenceTS.length - 1;
			int maxShift = shiftFactor - maxIndex;
			int minShift = shiftFactor - minIndex;
			double ratio1 = Math.abs(maxValue / minValue);
			double ratio2 = Math.abs(minValue / maxValue);
			double ratioThreshold = 1.2;
			if (ratio1 > ratioThreshold) {
				return maxShift;
			} else if (ratio2 > ratioThreshold) {
				return minShift;
			} else {
				if (Math.abs(maxShift) == Math.abs(minShift)) {
					if (ratio1 > ratio2) {
						return maxShift;
					} else {
						return minShift;
					}
				}
				else if (Math.abs(maxShift) < Math.abs(minShift)) {
					return maxShift;
				} else {
					return minShift;
				}
			}
		}
		else {
			return 0;
		}
	}


	private double findTimeshiftXcorr(double[] referenceTS, double[] inputTS, int limit) {
		if(Config.USE_XCORR) {
			double[] crossCorrelation = DSP.xcorr(referenceTS, inputTS);
//			System.out.println("cclength: " + crossCorrelation.length + " rtslength: " + referenceTS.length + " itslength: " + inputTS.length);
			int startIndex = crossCorrelation.length / 2 - limit + 1;
			int maxIndex = startIndex;
			int minIndex = startIndex;
			if (startIndex < 0) {
				startIndex = 0;
			}
			double maxValue = crossCorrelation[startIndex];
			double minValue = crossCorrelation[startIndex];
			for (int i = 0; i < limit * 2 - 1; i++) {
				int corrIndex = startIndex + i;
				if (corrIndex >= crossCorrelation.length) {
					corrIndex = crossCorrelation.length - 1;
				}
//				System.out.println("corrIndex: " + corrIndex);
				double currentValue = Math.abs(crossCorrelation[corrIndex]);
				if (currentValue > maxValue) {
					maxValue = currentValue;
					maxIndex = corrIndex;
				}
				if (currentValue < minValue) {
					minValue = currentValue;
					minIndex = corrIndex;
				}
			}
//			int correlateIndex = DSP.argmax(crossCorrelation);
			int shiftFactor = referenceTS.length - 1;
			int maxShift = shiftFactor - maxIndex;
			return maxShift;

		}
		else {
			return 0;
		}
	}

	public double[] generateReferenceSignal(double[] x, double signalPeriod, double offset) {
		double[] signalValues = new double[x.length];
		double twoPiOverPeriod = TWO_PI / signalPeriod;
		for (int i = 0; i < x.length; i++) {
			signalValues[i] = 10 * Math.sin(twoPiOverPeriod * x[i] + (ARCSIN_ONE - twoPiOverPeriod * offset));
		}
		return signalValues;
	}

	private double[] resampleValues(double[] x, double[] y, int num) {
		double[] newX = new double[num];
		double m = (DSP.max(x) - DSP.min(x)) / (double) num;
		newX[0] = DSP.min(x);
		for (int i = 0; i < num - 1; i++) {
			newX[i + 1] = newX[0] + m * (i + 1);
		}
		double[] newY = new double[num];
		LinearInterpolator linearInterpolator = new LinearInterpolator();
		PolynomialSplineFunction polynomialSplineFunction = linearInterpolator.interpolate(x, y);
		for (int i = 0; i < num; i++) {
			newY[i] = polynomialSplineFunction.value(newX[i]);
		}
		return newY;
	}

	private double[] getEWMA(double[] dataPoints, double alpha) {
		double[] ewmaPoints = new double[dataPoints.length];
		ExponentialMovingAverage ewma = new ExponentialMovingAverage(1 - alpha);
		for (int i = 0; i < dataPoints.length; i++) {
			ewmaPoints[i] = ewma.average(dataPoints[i]);
		}
		return ewmaPoints;
	}

	// https://stackoverflow.com/a/12453487
	/**
	 * This is a "wrapped" signal processing-style autocorrelation.
	 * For "true" autocorrelation, the data must be zero padded.
	 */
	private void bruteForceAutoCorrelation(double [] x, double [] ac) {
	    Mean m = new Mean();
	    Variance v = new Variance();
	    double mean = m.evaluate(x);
	    double var = v.evaluate(x);
		Arrays.fill(ac, 0);
		int n = x.length;
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				ac[j] += ((x[i] - mean) * (x[(n + i - j) % n] - mean));
			}
		}
		for (int j = 0; j < n; j++) {
			ac[j] /= var * n;
		}
	}

	private double sqr(double x) {
		return x * x;
	}

	private double[] autocorrelation(double[] x) {
		double[] ac = new double[x.length];
//		fftAutoCorrelation(x, ac);
        bruteForceAutoCorrelation(x, ac);
		return ac;
	}

	private double pmcc(double[] xArray, double[] yArray) {
		if (xArray.length < 2 || yArray.length < 2) {
			return 0;
		}
		PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
		double correlationCoefficient = pearsonsCorrelation.correlation(xArray, yArray);
		return correlationCoefficient;
	}

	private double detectSync(double[] windowTimes, double[][] windowValues, double signalPeriod, double offset) {

		// Feature calculation
		double[] featureSignal = new double[windowValues.length];
		for (int i = 0; i < windowValues.length; i++) {
			Double featureKey = new Double(windowTimes[i]);
			if (featureMap.containsKey(featureKey)) {
				featureSignal[i] = featureMap.get(featureKey);
			} else {
				Vector windowVector;
				windowVector = new Vector3D(windowValues[i]);
				double featureValue = deltaProjectTransform(projectLeftVector, projectRightVector, projectMeanVector, windowVector);
				double syncThresholdValue = deltaVectorThreshold(projectLeftVector, projectRightVector, projectMeanVector);
				featureMap.put(featureKey, featureValue);
				if (keepData) {
					syncThresholdMap.put(featureKey, "" + syncThresholdValue);
				}
				featureSignal[i] = featureValue;
			}
		}

		// Set reference period
		if (referencePeriod > 0) {
			signalPeriod = referencePeriod;
		}

		// Generate reference signal using projection or sine wave
		//double[] generatedSignal = generateProjectedReferenceSignal(windowTimes, signalPeriod, offset);
		double[] generatedSignal = generateReferenceSignal(windowTimes, signalPeriod, offset);

		// Detrend the feature signal
		double[] actualSignal = null;
		actualSignal = removeTrend(windowTimes, featureSignal);
//        actualSignal = featureSignal;

		// Interpolation
		int rFactor = windowTimes.length;
		double[] rs2 = null;
		double[] ps2 = null;
		if (rFactor == windowTimes.length) {
			rs2 = generatedSignal;
			ps2 = actualSignal;
		} else {
			rs2 = resampleValues(windowTimes, generatedSignal, rFactor);
			ps2 = resampleValues(windowTimes, actualSignal, rFactor);
		}

		// Calculate time between each element in window times
		double resampleTime = (windowTimes[windowTimes.length - 1] - windowTimes[0]) / (double) rFactor;
		//System.out.println("rFactor: " + rFactor + " resampleTime: " + resampleTime + " magLag: " + (int) (signalPeriod / (resampleTime * 2)) + " rs2len: " + rs2.length);
		double originalCorr = pmcc(rs2, ps2);
		if (originalCorr > 0) {
		    if (!Config.AUTOCORRELATION_MODE) {
		    	syncDirection = "bottom";
			} else {
		    	syncDirection = "auto";
			}
			directions.add("bottom");
		} else {
		    if (!Config.AUTOCORRELATION_MODE) {
		    	syncDirection = "top";
			} else {
		    	syncDirection = "auto";
			}
			directions.add("top");
		}
		while (directions.size() > Config.X_WINDOW_SIZE) {
			directions.remove(0);
		}
		// Cross correlation
//		int xcorrLimit = 5;
        int xcorrLimit = 2;
		double lagFactor = findTimeshiftXcorr(rs2, ps2, xcorrLimit);
		double lagTime = lagFactor * resampleTime;
		//System.out.println("lagFactor: " + lagFactor + " lagTime: " + lagTime);

//		double lagTime = findLagTime();

		// Adds all lag data to lag map
		if (keepData) {
			lagMap.put(windowTimes[windowTimes.length - 1], "" + lagFactor + "," + lagTime);
		}
        lagTimeMap.put(windowTimes[windowTimes.length - 1], lagTime);
//		lagMap.put(windowTimes[windowTimes.length - 1], "" + lagFactor + "," + lagTimeXcorr);

		// Shift the adjusted signal (or not)
//        double lagTime = 0;
		double[] adjustedSignal = null;
		if(lagTime == 0) {
			adjustedSignal = generatedSignal;
		} else {
			//adjustedSignal = generateProjectedReferenceSignal(windowTimes, signalPeriod, offset + lagTime);
			adjustedSignal = generateReferenceSignal(windowTimes, signalPeriod, offset + lagTime);
		}

		// Gets all data to write to file
		String[] windowResults = new String[windowTimes.length];
		for (int i = 0; i < windowTimes.length; i++) {
			windowResults[i] = "" + windowTimes[i] + "," + featureSignal[i] + "," + generatedSignal[i] + "," + adjustedSignal[i];
		}

		// Adds all data to reference map
		if (keepData) {
			windowsMap.put(windowTimes[windowTimes.length - 1], windowResults);
		}

		// Smoothing the signal
		if (Config.USE_INPUTSIGNAL_EWMA) {
			actualSignal = getEWMA(actualSignal, inputEWMA);
		}

		// Calculate correlation coefficient
		double correlationCoefficient = 0;

		// Calculate variance of feature signal
        if (Config.XCORR_WINDOW_SIZE != 0 && windowTimes.length < Config.XCORR_WINDOW_SIZE) {
        	return 0;
		} else {
            List<Double> windowLagTimes = new ArrayList<>();
            for (int i = 0; i < Config.XCORR_WINDOW_SIZE; i++) {
                double windowTime = windowTimes[windowTimes.length - 1 - i];
                if (!lagTimeMap.containsKey(windowTime)) {
                    continue;
                }
                double windowLagTime = lagTimeMap.get(windowTime);
                windowLagTimes.add(windowLagTime);
            }
            if (windowLagTimes.size() < Config.XCORR_WINDOW_SIZE && Config.XCORR_MAX_VAR != 0) {
                return 0;
            }
            Variance lagFactorVariance = new Variance();
            double[] windowLagTimeArray = new double[windowLagTimes.size()];
            for (int i = 0; i < windowLagTimes.size(); i++) {
                windowLagTimeArray[i] = windowLagTimes.get(i);
            }
            double windowLagVariance = lagFactorVariance.evaluate(windowLagTimeArray);

            if (!Config.AUTOCORRELATION_MODE && Config.XCORR_MAX_VAR != 0 && windowLagVariance > Config.XCORR_MAX_VAR) {
//    			System.out.println("rejected var: " + windowLagVariance);
                return 0;
            }
		}
		Variance xVariance = new Variance();
		double[] xArray = new double[Config.X_WINDOW_SIZE];
		for (int i = 0; i < xArray.length; i++) {
			xArray[i] = windowValues[windowValues.length - i - 1][0];
		}
		double[] yArray = new double[Config.X_WINDOW_SIZE];
		for (int i = 0; i < yArray.length; i++) {
			yArray[i] = windowValues[windowValues.length - i - 1][1];
		}
		double[] zArray = new double[Config.X_WINDOW_SIZE];
		for (int i = 0; i < zArray.length; i++) {
			zArray[i] = windowValues[windowValues.length - i - 1][2];
		}
		double xFeatureVariance = xVariance.evaluate(xArray);
		Variance yVariance = new Variance();
		Variance zVariance = new Variance();
		double yFeatureVariance = yVariance.evaluate(yArray);
		double zFeatureVariance = zVariance.evaluate(zArray);
		if (Config.X_MAX_VAR != 0) {
            if (featureSignal.length < Config.X_WINDOW_SIZE) {
            	return 0;
			}
        	if (xFeatureVariance > Config.X_MAX_VAR || xFeatureVariance < Config.X_MIN_VAR) {
//        		System.out.println("reject x var: " + xFeatureVariance);
        		return 0;
			}
		}

		if (Config.X_VAR_FACTOR != 0) {
		    double totalAbs = xFeatureVariance + yFeatureVariance + zFeatureVariance;
		    if (xFeatureVariance < Config.X_VAR_FACTOR * totalAbs) {
		    	return 0;
			}
        }

        if (directions.size() < Config.X_WINDOW_SIZE) {
			return 0;
		} else {
			String firstDirection = directions.get(0);
			for (int i = 0; i < directions.size(); i++) {
				String iDirection = directions.get(i);
				if (!iDirection.equals(firstDirection)) {
					return 0;
				}
			}
		}

		if (Config.USE_FEATURE_VARIANCE) {
			double variance = 0;
			double mean = DSP.mean(featureSignal);
			double temp = 0;
			for (double a : featureSignal) {
				temp += (a - mean) * (a - mean);
			}
			variance = temp / featureSignal.length;

			// If var=low, corr=0 / var=high, corr=actual
			if (variance < Config.VARIANCE_THRESHOLD) {
				// Return nothing
				correlationCoefficient = 0;
			} else {
				// Pearson correlation
				correlationCoefficient = pmcc(adjustedSignal, actualSignal);
			}
		} else if (Config.USE_TTEST) {
			correlationCoefficient = pmcc(adjustedSignal, actualSignal);
			double tScore = correlationCoefficient * Math.sqrt((featureSignal.length - 2) / (1 - Math.pow(correlationCoefficient, 2)));
			System.out.println(tScore);
		} else {
//		    double timeDistance = zbsigncluster(windowTimes, actualSignal);
//			double timeDistance = permutationEvaluateCluster(windowTimes, actualSignal, 100);
//		    correlationCoefficient = timeDistance;
			// Pearson correlation

//            correlationCoefficient = permutationEvaluateSnr(adjustedSignal, actualSignal, 100);
//            correlationCoefficient = zbsnr(adjustedSignal, actualSignal);
//            actualSignal = fourierPassFilter(actualSignal, 2.7, 1.1, 10);
//			actualSignal = fourierPassFilter(actualSignal, 2.4, 0, 10);
//      		correlationCoefficient = pmcc(adjustedSignal, actualSignal);
            if (Config.AUTOCORRELATION_MODE) {
//            	actualSignal = fourierPassFilter(actualSignal,2.4, 0, 10);
                double[] acSignal = new double[Config.AUTOCORRELATION_WINDOW_SIZE];
                for (int i = 0; i < Config.AUTOCORRELATION_WINDOW_SIZE; i++) {
                	acSignal[Config.AUTOCORRELATION_WINDOW_SIZE - 1 - i] = actualSignal[actualSignal.length - 1 - i];
				}
				double[] autocorrelatedSignal = autocorrelation(acSignal);
//				correlationCoefficient = (autocorrelatedSignal[autocorrelatedSignal.length - 1] - autocorrelatedSignal[(autocorrelatedSignal.length / 2)]) / 2;
                correlationCoefficient = Math.max(autocorrelatedSignal[autocorrelatedSignal.length - 1], -autocorrelatedSignal[(autocorrelatedSignal.length / 2)]);

			} else {
				correlationCoefficient = Math.abs(pmcc(adjustedSignal, actualSignal));
			}
//            correlationCoefficient = permutationEvaluateSSE(adjustedSignal, actualSignal, 100);
//            correlationCoefficient = permutationEvaluateCC(adjustedSignal, actualSignal, 100);
//            correlationCoefficient /= -1;
//			correlationCoefficient /= 2;
//            correlationCoefficient = zbccsnr(adjustedSignal, actualSignal);
//            correlationCoefficient = Math.max(Math.min(correlationCoefficient, 3), -3);
//			System.out.println("cc: " + correlationCoefficient + " rcc: " + pmcc(adjustedSignal, actualSignal));
//            System.out.println("ccpe: " + correlationCoefficient);
//            correlationCoefficient = Math.max(Math.min(correlationCoefficient, 1), -1);

//            System.out.println("normalized: " + correlationCoefficient);
//            correlationCoefficient = Math.abs(correlationCoefficient);
//            System.out.println("cc: " + correlationCoefficient);
		}
		return correlationCoefficient;
	}

	private void syncEvent(double timestamp, boolean isLeft) {
		if (isLeft) {
			leftSyncs.add(timestamp);
			leftSynced = true;
		} else {
			rightSyncs.add(timestamp);
			rightSynced = true;
		}
	}

	@Override
	public boolean detect(Tuple2<double[]> data) {
		return detectValue(data) >= detectThreshold;
	}

	@Override
	public String detectDirection(Tuple2<double[]> data) {
		return null;
	}

	public Map<Double, Double> getFeatureMap() {
		return featureMap;
	}

	public Map<Double, String[]> getWindowsMap() {
		return windowsMap;
	}

	public Map<Double, String> getLagMap() {
		return lagMap;
	}

	public Map<Double, String> getSubsampleMap() {
		return subsampleMap;
	}

	public Map<Double, String> getCorrelationMap() {
		return correlationMap;
	}

	public Map<Double, String> getMagnitudeMap() {
		return magnitudeMap;
	}

	public Map<Double, String> getSyncThresholdMap() { return syncThresholdMap; }

	public Map<Double, String> getWindowTimeMap() {
		return windowTimeMap;
	}

	public void setEventRecognitionListener(EventRecognitionListener listener) {
		this.listener = listener;
	}

	@Override
	public void run() {
		while ((detectorStatus && isRunning) || !dataBuffer.isEmpty()) {
			try {
				double correlationCoeff = 0.0;
				Tuple2<double[]> incomingData = dataBuffer.take();

				if (incomingData.getX() == null) {
					syncEvent(incomingData.getY()[1], incomingData.getY()[0] <= 0);
					continue;
				}

				if (Config.SUBSAMPLE_SIZE > 0 || Config.SUBSAMPLE_TIME > 0) {
					Tuple2<double[]> detectTuples = detectorSampler.processSample(incomingData.getX()[0], incomingData.getY());
					if (detectTuples != null) {
						String values = "";
						for (int i = 0; i < detectTuples.getY().length; i++) {
							values += Double.toString(detectTuples.getY()[i]);
							if (i != detectTuples.getY().length - 1) values += ",";
						}
						subsampleMap.put(detectTuples.getX()[0], values);
						correlationCoeff = detectValue(detectTuples);
						correlationMap.put(detectTuples.getX()[0], "" + correlationCoeff + "," + correlationCounter);
						magnitudeMap.put(detectTuples.getX()[0], "" + projectMagnitude + "," + correlationCounter);
						if (windowTimeString != null) {
							windowTimeMap.put(detectTuples.getX()[0], windowTimeString);
						}
						listener.onEventRecognized(""+ detectTuples.getX()[0] + "," + correlationCoeff + "," + correlationCounter + "," + syncDirection);
						correlationCounter++;
					}
				} else {
					correlationCoeff = detectValue(incomingData);
					correlationMap.put(incomingData.getX()[0], "" + correlationCoeff + "," + correlationCounter);
					magnitudeMap.put(incomingData.getX()[0], "" + projectMagnitude + "," + correlationCounter);
					if (windowTimeString != null) {
						windowTimeMap.put(incomingData.getX()[0], windowTimeString);
					}
					listener.onEventRecognized(""+ incomingData.getX()[0] + "," + correlationCoeff + "," + correlationCounter);
					correlationCounter++;
				}
			} catch (InterruptedException e) {
				System.out.println("error while processing sample from data buffer");
				e.printStackTrace();
			}
		}
	}
}