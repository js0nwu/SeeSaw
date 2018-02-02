package classification.Synchro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import classification.Detector;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import util.Tuple2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SynchroDetector extends Detector<Tuple2<double[]>> {

    private List<Double> sensorTimes = new ArrayList<>();
    private List<double[]> sensorWindow = new ArrayList<>();

    private List<Double> leftSyncs = new ArrayList<>();
    private List<Double> rightSyncs = new ArrayList<>();

    private List<double[]> nextLefts = new ArrayList<>();
    private List<double[]> nextRights = new ArrayList<>();


    private boolean leftSynced = false;
    private boolean rightSynced = false;

    private double detectThreshold = 1.0;
    private boolean useEWMA = false;

    private final double TWO_PI = 2 * Math.PI;
    private final double ARCSIN_ONE = Math.PI / 2;

    private Map<Double, Double> featureMap = new HashMap<>();

    private ExponentialMovingAverage ccEWMA = new ExponentialMovingAverage(0.1);
    private double windowStd = 999;
    private int stdWindowCounter = 0;
    private final int stdWindowSize = 60;
    private PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();

//    private double[] stdWindow = new double[stdWindowSize];
    private FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

    private void logOut(String tag, String log) {
        System.out.println(tag + " " + log);
    }

    private Vector lvTest = new Vector3D(29.3715,12.2305, -64.6905);
    private Vector rvTest = new Vector3D(31.1405, 14.03,-63.9585);
    private Vector cvTest = new Vector3D(30.71,12.84,-64.69);
    private boolean llTest = false;

    private void addStdWindow(double value) {
        int windowIndex = stdWindowCounter % stdWindowSize;
        stdWindow[windowIndex] = value;
        stdWindowCounter++;
    }

    private FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

    private void logOut(String tag, String log) {
        System.out.println(tag + " " + log);
    }

    private Vector lvTest = new Vector3D(29.3715,12.2305, -64.6905);
    private Vector rvTest = new Vector3D(31.1405, 14.03,-63.9585);
    private Vector cvTest = new Vector3D(30.71,12.84,-64.69);
    private boolean llTest = false;

    public int windowSize = 0;
    public int referencePeriod = 0;
    public int generatePeriod = 0;

//    private void addStdWindow(double value) {
//        int windowIndex = stdWindowCounter % stdWindowSize;
//        stdWindow[windowIndex] = value;
//        stdWindowCounter++;
//    }

    private double deltaSpaceTransform(Vector leftVector, Vector rightVector, Vector currentVector, boolean lastLeft) {
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

    private double pmcc(double[] xArray, double[] yArray) {
        double correlationCoefficient = pearsonsCorrelation.correlation(xArray, yArray);
        return correlationCoefficient;
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

    private double[] zeroPadPower2(double[] x1) {
        int x1PadPower = 0;
        while (Math.pow(2., (double) x1PadPower) < x1.length) {
            x1PadPower++;
        }
        int x1PadLength = (int) Math.pow(2., x1PadPower);
        double[] x1Padded = new double[x1PadLength];
        for (int i = 0; i < x1PadLength; i++) {
            if (i < x1.length) {
                x1Padded[i] = x1[i];
            } else {
                x1Padded[0] = 0.;
            }
        }
        return x1Padded;
    }

    private double findTimeshift(double[] x1, double[] x2) {
        double[] crossCorrelation = DSP.xcorr(x1, x2);
        int correlateIndex = DSP.argmax(crossCorrelation);
        int shiftFactor = x1.length - 1;
        return shiftFactor - correlateIndex;
        // the used block size is N(x1) + N(x2)
//        int blockSize = (x1.length+x2.length-1);

        // create & calc FFT with zero padding & get result

        // FFT x1
//        FFT f1 = new FFT(x1, blockSize, 44100);
//        f1.execFFT();
//        Complex[] c1 = f1.getOutputData();


//        Complex[] c1 = fft.transform(zeroPadPower2(x1), TransformType.FORWARD);

        // FFT x2
//        FFT f2 = new FFT(x2, blockSize, 44100);
//        f2.execFFT();
//        Complex[] c2 = f2.getOutputData();
//        Complex[] c2 = fft.transform(zeroPadPower2(x2), TransformType.FORWARD);

        // create output for cross correlation by using fast convolution : c3 = c1 multiply c2*
//        Complex[] c3 = new Complex[c1.length];
//        for(int i=0; i<c1.length;i++)
//        {
//            c3[i] = c1[i].multiply(c2[i].conjugate());
//        }

        // create & calc IFFT  & get result
//        IFFT f3 = new IFFT(c3, c3.length, 44100);
//        f3.execIFFT();
//        Complex[] corrResult = f3.getOutputData();
//        Complex[] corrResult = fft.transform(c3, TransformType.INVERSE);

// search the maximum corrleation coefficient and his index
//        double maxVal = 0;
//        int maxIndex = 0;

//        for(int i=0; i<corrResult.length;i++)
//        {
//            if(corrResult[i].abs() > maxVal)
//            {
//                maxVal = corrResult[i].abs();
//                maxIndex = i;
//            }
//        }
//        return x1.length - maxIndex;
    }

    private double[] generateReferenceSignal(double[] x, double signalPeriod, double offset) {
        double[] signalValues = new double[x.length];
        double twoPiOverPeriod = TWO_PI / signalPeriod;
        for (int i = 0; i < x.length; i++) {
            signalValues[i] = Math.sin(twoPiOverPeriod * x[i] + (ARCSIN_ONE - twoPiOverPeriod * offset));
        }
        return signalValues;
    }

    private double[] generateNoiseSignal(double[] x, double min, double max) {
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
        ExponentialMovingAverage ewma = new ExponentialMovingAverage(alpha);
        for (int i = 0; i < dataPoints.length; i++) {
            ewmaPoints[i] = ewma.average(dataPoints[i]);
        }
        return ewmaPoints;
    }

    private double detectSync(double[] windowTimes, double[][] windowValues, double[] lVector, double[] rVector, boolean lastLeft, double signalPeriod, double offset) {
        long beforeDS = System.currentTimeMillis();
        long beforeFG = System.currentTimeMillis();
        double[] featureSignal = new double[windowValues.length];
        Vector lV = new Vector3D(lVector);
        Vector rV = new Vector3D(rVector);
        for (int i = 0; i < windowValues.length; i++) {
            Double featureKey = new Double(windowTimes[i]);
            if (featureMap.containsKey(featureKey)) {
                featureSignal[i] = featureMap.get(featureKey);
            } else {
                Vector windowVector = new Vector3D(windowValues[i]);
                double featureValue = deltaSpaceTransform(lV, rV, windowVector, lastLeft);
                featureMap.put(featureKey, featureValue);
                featureSignal[i] = featureValue;
            }
        }
        if (referencePeriod > 0) {
            signalPeriod = referencePeriod;
        }
        double[] generatedSignal = generateReferenceSignal(windowTimes, signalPeriod, offset);
        double[] actualSignal = null;
        if (generatePeriod == 0) {
            actualSignal = generateNoiseSignal(windowTimes, 0, 1);
        } else if (generatePeriod > 0){
            actualSignal = generateReferenceSignal(windowTimes, generatePeriod, offset);
        } else {
            actualSignal = removeTrend(windowTimes, featureSignal);
        }
        int rFactor = windowTimes.length;
        double[] rs2 = resampleValues(windowTimes, generatedSignal, rFactor);
        double[] ps2 = resampleValues(windowTimes, actualSignal, rFactor);
        double resampleTime = (windowTimes[windowTimes.length - 1] - windowTimes[0]) / (double) rFactor;
        long beforeAdjust = System.currentTimeMillis();
        double lagFactor = findTimeshift(rs2, ps2);
        double lagTime = lagFactor * resampleTime;
        double[] adjustedSignal = generateReferenceSignal(windowTimes, signalPeriod, offset + lagTime);
        long afterAdjust = System.currentTimeMillis();
//        logOut("detectsync xcorr", "" + (afterAdjust - beforeAdjust));
        if (useEWMA) {
            long beforeEWMA = System.currentTimeMillis();
            actualSignal = getEWMA(actualSignal, 0.1);
            long afterEWMA = System.currentTimeMillis();
//            logOut("detectsync ewma", "" + (afterEWMA - beforeEWMA));
        }
        long beforeCC = System.currentTimeMillis();
        double correlationCoefficient = pmcc(adjustedSignal, actualSignal);
        return correlationCoefficient;
    }

    public void syncEvent(double timestamp, boolean isLeft) {
        if (isLeft) {
            leftSyncs.add(timestamp);
            leftSynced = true;
        } else {
            rightSyncs.add(timestamp);
            rightSynced = true;
        }
    }

    public double getWindowStd() {
        return windowStd;
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
        sensorTimes.add(data.getX()[0]);
        sensorWindow.add(data.getY());

        if (leftSynced) {
            nextLefts.add(data.getY());
            leftSynced = false;
        }
        if (rightSynced) {
            nextRights.add(data.getY());
            rightSynced = false;
        }

//        logOut("sensortimes size before", "" + sensorTimes.size());
//        logOut("sensorwindo size before", "" + sensorWindow.size());

        if (windowSize > 0) {
            while(sensorTimes.size() > windowSize) {
                sensorTimes.remove(0);
                sensorWindow.remove(0);
            }
        }

//		while(data.getX()[0] - sensorTimes.get(0) > 2) {
//			if(sensorTimes.size() != sensorWindow.size()) {
//				logOut("tag", "times and data not equal size");
//			} else {
//				// remove data to maintain n second window size
//				sensorTimes.remove(0);
//				sensorWindow.remove(0);
//			}
//		}

//        logOut("sensortimes size after", "" + sensorTimes.size());
//        logOut("sensorwindow size after", "" + sensorWindow.size());

//        logOut("first time", "" + sensorTimes.get(0));
//        logOut("last time", "" + sensorTimes.get(sensorTimes.size()-1));

        if (nextLefts.isEmpty() || nextRights.isEmpty()) {
            return 0;
        }
        if (leftSyncs.size() < 1 || rightSyncs.size() < 1) {
            return 0;
        }

        double signalPeriod = Math.abs(leftSyncs.get(leftSyncs.size() - 1) - rightSyncs.get(rightSyncs.size() - 1)) * 2;
        double syncLeft = leftSyncs.get(leftSyncs.size() - 1);
        double syncRight = rightSyncs.get(rightSyncs.size() - 1);
        boolean lastLeft = syncLeft > syncRight;

        double[] windowTimes = new double[sensorTimes.size()];
        for (int i = 0; i < windowTimes.length; i++) {
            windowTimes[i] = sensorTimes.get(i);
        }
        double[][] windowValues = new double[sensorWindow.size()][];
        for (int i = 0; i < windowValues.length; i++) {
            windowValues[i] = sensorWindow.get(i);
        }
        double[] lVector = nextLefts.get(nextLefts.size() - 1);
        double[] rVector = nextRights.get(nextRights.size() - 1);
        double offset = leftSyncs.get(leftSyncs.size() - 1);
        return detectSync(windowTimes, windowValues, lVector, rVector, lastLeft, signalPeriod, offset);
    }

    @Override
    public boolean detect(Tuple2<double[]> data) {
        return detectValue(data) >= detectThreshold;
    }

    @Override
    public String detectDirection(Tuple2<double[]> data) {
        return null;
    }
}


