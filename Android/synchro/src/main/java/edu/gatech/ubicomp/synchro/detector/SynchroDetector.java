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

//	private double permutationEvaluateMean(double[] referenceSignal, double[] inputSignal, int N) {
//
//	}

	public double[] fourierPassFilter(double[] data, double lowPass, double highPass, double frequency){
		//data: input data, must be spaced equally in time.
		//lowPass: The cutoff frequency at which
		//frequency: The frequency of the input data.

		//The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
		int minPowerOf2 = 1;
		while(minPowerOf2 < data.length)
			minPowerOf2 = 2 * minPowerOf2;

		//pad with zeros
		double[] padded = new double[minPowerOf2];
		for(int i = 0; i < data.length; i++)
			padded[i] = data[i];


		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

		//build the frequency domain array
		double[] frequencyDomain = new double[fourierTransform.length];
		for(int i = 0; i < frequencyDomain.length; i++)
			frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

		//build the classifier array, 2s are kept and 0s do not pass the filter
		double[] keepPoints = new double[frequencyDomain.length];
		keepPoints[0] = 1;
		for(int i = 1; i < frequencyDomain.length; i++){
			if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass)
				keepPoints[i] = 2;
			else
				keepPoints[i] = 0;
		}

		//filter the fft
		for(int i = 0; i < fourierTransform.length; i++)
			fourierTransform[i] = fourierTransform[i].multiply((double)keepPoints[i]);

		//invert back to time domain
		Complex[] reverseFourier = transformer.transform(fourierTransform, TransformType.INVERSE);

		//get the real part of the reverse
		double[] result = new double[data.length];
		for(int i = 0; i< result.length; i++){
			result[i] = reverseFourier[i].getReal();
		}

		return result;
	}

	private double permutationEvaluateSnr(double[] referenceSignal, double[] inputSignal, int N) {
	    double[] ccResults = new double[N];
	    double[][] permutations = generateRandomPermutations(inputSignal, N);
	    for (int i = 0; i < N; i++) {
	    	if (Config.USE_INPUTSIGNAL_EWMA) {
	    		permutations[i] = getEWMA(permutations[i], inputEWMA);
			}
//	        double pResults = pmcc(referenceSignal, permutations[i]);
            double pResults = zbsnr(referenceSignal, permutations[i]);
	        ccResults[i] = pResults;
        }
        Mean m = new Mean();
        double mean = m.evaluate(ccResults);
		Variance variance = new Variance();
        double var = variance.evaluate(ccResults);
	    double std = Math.sqrt(var);
	    if (Config.USE_INPUTSIGNAL_EWMA) {
	    	inputSignal = getEWMA(inputSignal, inputEWMA);
		}
//	    double actualCC = pmcc(referenceSignal, inputSignal);
        double actualCC = zbsnr(referenceSignal, inputSignal);
	    double score = (actualCC - mean) / std;
		Kurtosis kurtosis = new Kurtosis();
		double kurt = kurtosis.evaluate(ccResults);
//		System.out.println("score: " + score + " kurt: " + kurt + " var: " + var + " mean: " + mean + " actual: " + actualCC);
	    return score;
    }


	private double permutationEvaluateSSE(double[] referenceSignal, double[] inputSignal, int N) {
		double[] ccResults = new double[N];
		double[][] permutations = generateRandomPermutations(inputSignal, N);
		for (int i = 0; i < N; i++) {
			if (Config.USE_INPUTSIGNAL_EWMA) {
				permutations[i] = getEWMA(permutations[i], inputEWMA);
			}
//	        double pResults = pmcc(referenceSignal, permutations[i]);
			double pResults = zbsse(referenceSignal, permutations[i]);
			ccResults[i] = pResults;
		}
		Mean m = new Mean();
		double mean = m.evaluate(ccResults);
		Variance variance = new Variance();
		double var = variance.evaluate(ccResults);
		double std = Math.sqrt(var);
		if (Config.USE_INPUTSIGNAL_EWMA) {
			inputSignal = getEWMA(inputSignal, inputEWMA);
		}
//	    double actualCC = pmcc(referenceSignal, inputSignal);
		double actualCC = zbsse(referenceSignal, inputSignal);
		double score = (actualCC - mean) / std;
		Kurtosis kurtosis = new Kurtosis();
		double kurt = kurtosis.evaluate(ccResults);
//		System.out.println("score: " + score + " kurt: " + kurt + " var: " + var + " mean: " + mean + " actual: " + actualCC);
		return score;
	}

	private double permutationEvaluateCluster(double[] inputTimes, double[] inputSignal, int N) {
		if (inputTimes.length != inputSignal.length) {
			System.out.println("inputTimes and inputSignal are not equal");
			return 0;
		}
		double[] ccResults = new double[N];
		double[] is = new double[inputTimes.length];
		for (int i = 0; i < is.length; i++) {
			is[i] = i;
		}
		double[][] permutations = generateRandomPermutations(is, N);
		for (int i = 0; i < N; i++) {
		    double[] permutedIndicies = permutations[i];
		    double[] permutedSignal = new double[permutedIndicies.length];
		    for (int j = 0; j < permutedIndicies.length; j++) {
		    	int pi = (int) permutedIndicies[j];
		    	permutedSignal[j] = inputSignal[pi];
			}
			double pResult = zbsigncluster(inputTimes, permutedSignal);
		    ccResults[i] = pResult;
		}
		Mean m = new Mean();
		double mean = m.evaluate(ccResults);
		Variance variance = new Variance();
		double var = variance.evaluate(ccResults);
		double std = Math.sqrt(var);
		double actualCC = zbsigncluster(inputTimes, inputSignal);
		double score = (actualCC - mean) / std;
		Kurtosis kurtosis = new Kurtosis();
		double kurt = kurtosis.evaluate(ccResults);
		System.out.println("score: " + score + " kurt: " + kurt + " var: " + var + " mean: " + mean + " actual: " + actualCC);
		return score;
	}

	private double permutationEvaluateSnrCC(double[] referenceSignal, double[] inputSignal, int N) {
		double[] ccResults = new double[N];
		double[][] permutations = generateRandomPermutations(inputSignal, N);
		for (int i = 0; i < N; i++) {
			if (Config.USE_INPUTSIGNAL_EWMA) {
				permutations[i] = getEWMA(permutations[i], inputEWMA);
			}
//	        double pResults = pmcc(referenceSignal, permutations[i]);
			double pResults = zbsnr(referenceSignal, permutations[i]);
			ccResults[i] = pResults;
		}
		Mean m = new Mean();
		double mean = m.evaluate(ccResults);
		Variance variance = new Variance();
		double var = variance.evaluate(ccResults);
		double std = Math.sqrt(var);
		if (Config.USE_INPUTSIGNAL_EWMA) {
			inputSignal = getEWMA(inputSignal, inputEWMA);
		}
//	    double actualCC = pmcc(referenceSignal, inputSignal);
		double actualCC = zbccsnr(referenceSignal, inputSignal);
		double score = (actualCC - mean) / std;
		Kurtosis kurtosis = new Kurtosis();
		double kurt = kurtosis.evaluate(ccResults);
//		System.out.println("score: " + score + " kurt: " + kurt + " var: " + var + " mean: " + mean + " actual: " + actualCC);
		return score;
	}

	private double permutationEvaluateCC(double[] referenceSignal, double[] inputSignal, int N) {
		double[] ccResults = new double[N];
		double[][] permutations = generateRandomPermutations(inputSignal, N);
		for (int i = 0; i < N; i++) {
			if (Config.USE_INPUTSIGNAL_EWMA) {
				permutations[i] = getEWMA(permutations[i], inputEWMA);
			}
//	        double pResults = pmcc(referenceSignal, permutations[i]);
			double pResults = zbsnr(referenceSignal, permutations[i]);
			ccResults[i] = pResults;
		}
		Mean m = new Mean();
		double mean = m.evaluate(ccResults);
		Variance variance = new Variance();
		double var = variance.evaluate(ccResults);
		double std = Math.sqrt(var);
		if (Config.USE_INPUTSIGNAL_EWMA) {
			inputSignal = getEWMA(inputSignal, inputEWMA);
		}
	    double actualCC = pmcc(referenceSignal, inputSignal);
//		double actualCC = zbsnr(referenceSignal, inputSignal);
		double score = (actualCC - mean) / std;
//		score /= 30;
		Kurtosis kurtosis = new Kurtosis();
		double kurt = kurtosis.evaluate(ccResults);
//		System.out.println("score: " + score + " kurt: " + kurt + " var: " + var + " mean: " + mean + " actual: " + actualCC);
		return score;
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
//		System.out.println("dv: " + deltaVector.getNorm() + " mv: " + meanVector.getNorm());
//        vectorLength = deltaVector.getNorm();
//		double leftMost = leftVector.subtract(meanVector).dotProduct(meanVector) / meanVector.getNorm();
//		double rightMost = rightVector.subtract(meanVector).dotProduct(meanVector) / meanVector.getNorm();
			double leftMost = scalarProject(meanVector, leftVector.subtract(meanVector));
			double rightMost = scalarProject(meanVector, rightVector.subtract(meanVector));
//		System.out.println("lm: " + leftMost + " rm: " + rightMost);
			vectorLength = Math.abs(leftMost - rightMost);
//        System.out.println("vl: " + vectorLength);
			Vector currentDeltaVector = null;
			currentDeltaVector = currentVector.subtract(meanVector);
//        currentDeltaVector = currentVector;
//		double scalarProjection = currentDeltaVector.dotProduct(meanVector) / meanVector.getNorm();
			double scalarProjection = scalarProject(meanVector, currentDeltaVector);
//		System.out.println("" + deltaVector + "," + currentDeltaVector + "," + scalarProjection);
//		  double scalarProjection = currentDeltaVector.dotProduct(deltaVector);
//        double scalarProjection = currentDeltaVector.dotProduct(meanVector);
//		System.out.println("dv " + deltaVector + " cdv " + currentDeltaVector + " sp " + scalarProjection);
//        System.out.println("dv " + deltaVector + " mv " + meanVector);


			return scalarProjection;

//        return currentVector.dotProduct(deltaVector);
//        return currentDeltaVector.getNorm();
//        return currentDeltaVector.dotProduct(deltaVector);
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
			double maxValue = crossCorrelation[startIndex];
			double minValue = crossCorrelation[startIndex];
			for (int i = 0; i < limit * 2 - 1; i++) {
				int corrIndex = startIndex + i;
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

	private double findLagTime() {

//		double firstLeftFlashTime = leftSyncs.get(leftSyncs.size()-2);
//		double secondLeftFlashTime = leftSyncs.get(leftSyncs.size()-1);
//		double rightFlashTime = rightSyncs.get(rightSyncs.size()-1);
//
//
//		double l1 = projectLeftVectorTimestamp - firstLeftFlashTime;
//		double l2 = projectRightVectorTimestamp - rightFlashTime;
////
//		double r1 = rightFlashTime - projectLeftVectorTimestamp;
//		double r2 = secondLeftFlashTime - projectRightVectorTimestamp;
//
////		System.out.println(l1 + "," + l2 + "," + r1 + "," + r2);
//
////		System.out.println("fl" + " " + firstLeftFlashTime + " sl " + secondLeftFlashTime + " r " + rightFlashTime + " pl " + projectLeftVectorTimestamp + " pr " + projectRightVectorTimestamp);
//
//		double leftDifferences = (l1 + l2)/2;
//		double rightDifferences = (r1 + r2)/2;
//
////		System.out.println(Math.abs(leftDifferences) >= Math.abs(rightDifferences));
//
////		System.out.println(Math.abs(leftDifferences) + "," + Math.abs(rightDifferences));
//
//		System.out.println("ld " + leftDifferences + " rd " + rightDifferences);
//
//		return 0;
		double leftFlashTime = leftSyncs.get(leftSyncs.size() - 1);
		double rightFlashTime = rightSyncs.get(rightSyncs.size() - 1);
		if (leftFlashTime > rightFlashTime) {
			double leftDiff = projectLeftVectorTimestamp - rightFlashTime;
			double rightDiff = projectRightVectorTimestamp - rightFlashTime;

			if (leftDiff > rightDiff) {
//				System.out.println("leftDiff");
				return leftDiff;
			} else {
//				System.out.println("rightDiff");
				return rightDiff;
			}
		}
		return 0;
	}



	public double[] generateReferenceSignal(double[] x, double signalPeriod, double offset) {
		double[] signalValues = new double[x.length];
		double twoPiOverPeriod = TWO_PI / signalPeriod;
		for (int i = 0; i < x.length; i++) {
			signalValues[i] = 10 * Math.sin(twoPiOverPeriod * x[i] + (ARCSIN_ONE - twoPiOverPeriod * offset));
		}
		return signalValues;
	}

	private double[] generateCosReferenceSignal(double[] x, double signalPeriod, double offset) {
		double[] signalValues = new double[x.length];
		double twoPiOverPeriod = TWO_PI / signalPeriod;
		for (int i = 0; i < x.length; i++) {
			signalValues[i] = 10 * Math.cos(twoPiOverPeriod * x[i] + (ARCSIN_ONE - twoPiOverPeriod * offset));
		}
		return signalValues;
	}

	public double[] generateProjectedReferenceSignal(double[] x, double signalPeriod, double offset) {
		double[] signalValues = new double[x.length];
		double twoPiOverPeriod = TWO_PI / signalPeriod;
		for (int i = 0; i < x.length; i++) {
			double t = x[i];
			double originalValue = Math.sin(twoPiOverPeriod * t + (ARCSIN_ONE - twoPiOverPeriod * offset));
			boolean lastLeft = (int) (Math.floor((t - offset) / (signalPeriod / 2))) % 2 == 0;
			signalValues[i] = deltaSpaceTransform(new Vector3D(1, 0, 0), new Vector3D(-1, 0, 0), new Vector3D(originalValue, 0, 0), lastLeft);
		}
		return signalValues;
	}

	public double[] generateNoiseSignal(double[] x, double min, double max) {
		Random r = new Random();
		double[] signalValues = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			signalValues[i] = min + (max - min) * r.nextDouble();
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

	private void fftAutoCorrelation(double [] x, double [] ac) {
		int n = x.length;
		// Assumes n is even.
		DoubleFFT_1D fft = new DoubleFFT_1D(n);
		fft.realForward(x);
		ac[0] = sqr(x[0]);
		// ac[0] = 0;  // For statistical convention, zero out the mean
		ac[1] = sqr(x[1]);
		for (int i = 2; i < n; i += 2) {
			ac[i] = sqr(x[i]) + sqr(x[i+1]);
			ac[i+1] = 0;
		}
		DoubleFFT_1D ifft = new DoubleFFT_1D(n);
		ifft.realInverse(ac, true);
		// For statistical convention, normalize by dividing through with variance
		//for (int i = 1; i < n; i++)
		//    ac[i] /= ac[0];
		//ac[0] = 1;
	}

	private double[] autocorrelation(double[] x) {
		double[] ac = new double[x.length];
//		fftAutoCorrelation(x, ac);
        bruteForceAutoCorrelation(x, ac);
		return ac;
	}

	private double pmcc(double[] xArray, double[] yArray) {
		PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
		double correlationCoefficient = pearsonsCorrelation.correlation(xArray, yArray);
		return correlationCoefficient;
	}

	private double zbsigncluster(double[] t, double[] x) {
		List<Double> positiveTimes = new ArrayList<>();
		List<Double> negativeTimes = new ArrayList<>();
		for (int i = 0; i < t.length; i++) {
			if (x[i] >= 0) {
				positiveTimes.add(t[i]);
			} else {
				negativeTimes.add(t[i]);
			}
		}
		double[] positiveTimesA = new double[positiveTimes.size()];
		double[] negativeTimesA = new double[negativeTimes.size()];
		for (int i = 0; i < positiveTimes.size(); i++) {
			positiveTimesA[i] = positiveTimes.get(i);
		}
		for (int i = 0; i < negativeTimes.size(); i++) {
			negativeTimesA[i] = negativeTimes.get(i);
		}
//		System.out.println("pt: " + positiveTimes.size() + " nt: " + negativeTimes.size());
		Mean m = new Mean();
		double averagePositiveTime = m.evaluate(positiveTimesA);
		double averageNegativeTime = m.evaluate(negativeTimesA);
		double averageTimeDifference = averageNegativeTime - averagePositiveTime;
		return averageTimeDifference;
	}

	private double zbsnr(double[] xArray, double[] yArray) {
	    double[] totalSignal = yArray;
	    double[] actualSignal = xArray;
	    double xmin = xArray[0];
	    double xmax = xArray[0];
	    for (int i = 0; i < xArray.length; i++) {
	        if (xArray[i] < xmin) {
	            xmin = xArray[i];
            }
            if (xArray[i] > xmax) {
	            xmax = xArray[i];
            }
        }

        double xAmp = xmax - xmin;
	    List<Double> tHighs = new ArrayList<>();
	    List<Double> tLows = new ArrayList<>();
	    for (int i = 0; i < xArray.length; i++) {
	        if (xArray[i] == xmin) {
	            tLows.add(yArray[i]);
            }
            if (xArray[i] == xmax) {
	            tHighs.add(yArray[i]);
            }
        }
        double[] tHighA = new double[tHighs.size()];
	    double[] tLowA = new double[tLows.size()];
	    for (int i = 0; i < tHighs.size(); i++) {
	        tHighA[i] = tHighs.get(i);
        }
        for (int i = 0; i < tLows.size(); i++) {
	        tLowA[i] = tLows.get(i);
        }
        Mean m = new Mean();
	    double tAmp = Math.abs(m.evaluate(tHighA) - m.evaluate(tLowA));
//        double tAmp = vectorLength;
//		System.out.println("tAmp: " + tAmp + " vtAmp: " + vtAmp);
	    double scale = tAmp / xAmp;
	    for (int i = 0; i < actualSignal.length; i++) {
	        actualSignal[i] *= scale;
        }
	    double[] noiseSignal = new double[yArray.length];
	    for (int i = 0; i < totalSignal.length; i++) {
	        noiseSignal[i] = totalSignal[i] - actualSignal[i];
        }
        Variance var = new Variance();
	    double signalVariance = var.evaluate(actualSignal);
	    double noiseVariance = var.evaluate(noiseSignal);
	    double noiseCC = pmcc(actualSignal, noiseSignal);
//	    System.out.println("nsc: " + pmcc(actualSignal, noiseSignal));
//	    double snr = signalVariance / noiseVariance * (1 - Math.min(0, noiseCC));
        double snr = signalVariance / noiseVariance;
//        double snr = pmcc(xArray, yArray) / pmcc(xArray, noiseSignal);
//		snr /= 4;
	    return snr;
    }

	private double zbsse(double[] xArray, double[] yArray) {
		double[] totalSignal = yArray;
		double[] actualSignal = xArray;
		double xmin = xArray[0];
		double xmax = xArray[0];
		for (int i = 0; i < xArray.length; i++) {
			if (xArray[i] < xmin) {
				xmin = xArray[i];
			}
			if (xArray[i] > xmax) {
				xmax = xArray[i];
			}
		}

		double xAmp = xmax - xmin;
		List<Double> tHighs = new ArrayList<>();
		List<Double> tLows = new ArrayList<>();
		for (int i = 0; i < xArray.length; i++) {
			if (xArray[i] == xmin) {
				tLows.add(yArray[i]);
			}
			if (xArray[i] == xmax) {
				tHighs.add(yArray[i]);
			}
		}
		double[] tHighA = new double[tHighs.size()];
		double[] tLowA = new double[tLows.size()];
		for (int i = 0; i < tHighs.size(); i++) {
			tHighA[i] = tHighs.get(i);
		}
		for (int i = 0; i < tLows.size(); i++) {
			tLowA[i] = tLows.get(i);
		}
		Mean m = new Mean();
		double tAmp = Math.abs(m.evaluate(tHighA) - m.evaluate(tLowA));
//		double tAmp = vectorLength;
//		System.out.println("tAmp: " + tAmp + " vtAmp: " + vtAmp);
		double scale = tAmp / xAmp;
		for (int i = 0; i < actualSignal.length; i++) {
			actualSignal[i] *= scale;
		}
		double sse = 0;
		for (int i = 0; i < actualSignal.length; i++) {
			double error = (actualSignal[i] - totalSignal[i]) * (actualSignal[i] - totalSignal[i]);
			sse += error;
		}
		return sse;
	}

	private double zbccsnr(double[] xArray, double[] yArray) {
		double[] totalSignal = yArray;
		double[] actualSignal = xArray;
		double xmin = xArray[0];
		double xmax = xArray[0];
		for (int i = 0; i < xArray.length; i++) {
			if (xArray[i] < xmin) {
				xmin = xArray[i];
			}
			if (xArray[i] > xmax) {
				xmax = xArray[i];
			}
		}

		double xAmp = xmax - xmin;
		List<Double> tHighs = new ArrayList<>();
		List<Double> tLows = new ArrayList<>();
		for (int i = 0; i < xArray.length; i++) {
			if (xArray[i] == xmin) {
				tLows.add(yArray[i]);
			}
			if (xArray[i] == xmax) {
				tHighs.add(yArray[i]);
			}
		}
		double[] tHighA = new double[tHighs.size()];
		double[] tLowA = new double[tLows.size()];
		for (int i = 0; i < tHighs.size(); i++) {
			tHighA[i] = tHighs.get(i);
		}
		for (int i = 0; i < tLows.size(); i++) {
			tLowA[i] = tLows.get(i);
		}
		Mean m = new Mean();
//	    double tAmp = Math.abs(m.evaluate(tHighA) - m.evaluate(tLowA));
		double tAmp = vectorLength;
//	    System.out.println("tAmp: " + tAmp);
		double scale = tAmp / xAmp;
		for (int i = 0; i < actualSignal.length; i++) {
			actualSignal[i] *= scale;
		}
		double[] noiseSignal = new double[yArray.length];
		for (int i = 0; i < totalSignal.length; i++) {
			noiseSignal[i] = totalSignal[i] - actualSignal[i];
		}
		double totalCorr = pmcc(actualSignal, totalSignal);
		double noiseCorr = pmcc(actualSignal, noiseSignal);
//		System.out.println("tc: " + totalCorr + " nc: " + noiseCorr);
//		double snr = totalCorr / noiseCorr;
        double snr = totalCorr - Math.abs(noiseCorr);
//        System.out.println("snr: " + snr);
//		snr /= 2;
		return snr;
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
//				double featureValue = deltaSpaceTransform(lV, rV, windowVector, lastLeft);
//				double featureValue = deltaSpaceMeanTransform(lV, rV, windowVector, lastLeft);
				double featureValue = deltaProjectTransform(projectLeftVector, projectRightVector, projectMeanVector, windowVector);
				double syncThresholdValue = deltaVectorThreshold(projectLeftVector, projectRightVector, projectMeanVector);
				featureMap.put(featureKey, featureValue);
				syncThresholdMap.put(featureKey, ""+syncThresholdValue);
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
		lagMap.put(windowTimes[windowTimes.length - 1], "" + lagFactor + "," + lagTime);
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
		windowsMap.put(windowTimes[windowTimes.length - 1], windowResults);

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

//	public void reset() {
//		windowVectors.clear();
//		projectMeanVector = null;
//		projectLeftVector = null;
//		projectRightVector = null;
//		sensorTimes.clear();
//		sensorWindow.clear();
//		nextLefts.clear();
//		nextRights.clear();
//		leftSyncs.clear();
//		rightSyncs.clear();
//		leftSynced = false;
//		rightSynced = false;
//		dataBuffer.clear();
//		featureMap.clear();
//		correlationMap.clear();
//		lagMap.clear();
//		subsampleMap.clear();
//		windowsMap.clear();
//		if (debugMode) System.out.println("db s" + "" + dataBuffer.size());
//	}

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