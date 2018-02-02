package edu.gatech.ubicomp.synchro.detector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwpilly on 9/8/16.
 */
public class MeanTapDetector extends Detector<Tuple2<NVector>> {

    private static final int TAP_WINDOW_SIZE = 4;
    private static final float TAP_ACCEPT_THRESHOLD = 3.0F;

    private Tuple2<Float>[] tupleListX = new Tuple2[TAP_WINDOW_SIZE];
    private Tuple2<Float>[] tupleListY = new Tuple2[TAP_WINDOW_SIZE];
    private Tuple2<Float>[] tupleListZ = new Tuple2[TAP_WINDOW_SIZE];

    private int received = 0;

    private int windowPointer = 0;

    private void updateWindow(Tuple2<NVector> data) {
        tupleListX[windowPointer] = new Tuple2<>(new Float(data.getX().getValue(0)), new Float(data.getY().getValue(0)));
        tupleListY[windowPointer] = new Tuple2<>(new Float(data.getX().getValue(1)), new Float(data.getY().getValue(1)));
        tupleListZ[windowPointer] = new Tuple2<>(new Float(data.getX().getValue(2)), new Float(data.getY().getValue(2)));
        windowPointer++;
        windowPointer = windowPointer % TAP_WINDOW_SIZE;
    }

    private float tupleStat(Tuple2<Float>[] tupleList) {
        List<Float> aList = new ArrayList<>();
        List<Float> bList = new ArrayList<>();
        for (Tuple2<Float> tuple : tupleList) {
            aList.add(tuple.getX());
            bList.add(tuple.getY());
        }
        float aMean = new NVector(aList).getMean();
        float bMean = new NVector(bList).getMean();
        return aMean - bMean;
    }

    private Tuple2[] itemFlipper(Tuple2[] tupleList, boolean[] flipList) {
        Tuple2[] newList = new Tuple2[tupleList.length];
        for (int i = 0; i < tupleList.length; i++) {
            if (flipList[i]) {
                newList[i] = tupleList[i].reversed();
            } else {
                newList[i] = tupleList[i];
            }
        }
        return newList;
    }

    private boolean[] intToBinary(int number, int length) {
        boolean[] bits = new boolean[length];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = ((0x1 << i) & number) != 0;
        }
        return bits;
    }

    private List<Tuple2[]> comboIterator(Tuple2[] tupleList) {
        int tupleLength = tupleList.length;
        List<Tuple2[]> combos = new ArrayList<>();
        int comboRange = (int) Math.pow(2, tupleLength);
        for (int i = 0; i < comboRange - 1; i++) {
            int numerical = i + 1;
            boolean[] flipList = intToBinary(numerical, tupleLength);
            combos.add(itemFlipper(tupleList, flipList));
        }
        return combos;
    }

    @Override
    public boolean detect(Tuple2<NVector> data) {
        updateWindow(data);
        received++;
        if (received < TAP_WINDOW_SIZE) {
            return false;
        } else {
            List<Tuple2[]> windowCombosX = comboIterator(tupleListX);
            List<Tuple2[]> windowCombosY = comboIterator(tupleListY);
            List<Tuple2[]> windowCombosZ = comboIterator(tupleListZ);
            float[] xResults = new float[windowCombosX.size()];
            float[] yResults = new float[windowCombosY.size()];
            float[] zResults = new float[windowCombosZ.size()];
            for (int i = 0; i < windowCombosX.size(); i++) {
                xResults[i] = tupleStat(windowCombosX.get(i));
            }
            for (int i = 0; i < windowCombosY.size(); i++) {
                yResults[i] = tupleStat(windowCombosY.get(i));
            }
            for (int i = 0; i < windowCombosZ.size(); i++) {
                zResults[i] = tupleStat(windowCombosZ.get(i));
            }
            float queryX = tupleStat(tupleListX);
            float queryY = tupleStat(tupleListY);
            float queryZ = tupleStat(tupleListZ);
            float[] queryData = { queryX, queryY, queryZ };
            NVector queryVector = new NVector(queryData);
            float queryMag = queryVector.magnitude();
            NVector xVector = new NVector(xResults);
            NVector yVector = new NVector(yResults);
            NVector zVector = new NVector(zResults);
            float xMean = xVector.getMean();
            float yMean = yVector.getMean();
            float zMean = zVector.getMean();
            float xStd = xVector.getStd();
            float yStd = yVector.getStd();
            float zStd = zVector.getStd();
            float xStat = tupleStat(tupleListX);
            float yStat = tupleStat(tupleListY);
            float zStat = tupleStat(tupleListZ);
            float[] resultsMeanVector = { xVector.getMean(), yVector.getMean(), zVector.getMean() };
            float resultsMeanMag = new NVector(resultsMeanVector).magnitude();
            float[] resultsStdVector = { xStd, yStd, zStd };
            float resultsMeanStd = new NVector(resultsStdVector).magnitude();
            float[] stdDistanceVector = { Math.abs(xStat - xMean) / xStd, Math.abs(yStat - yMean) / yStd, Math.abs(zStat - zMean) / zStd };
            float stdDistanceMag = new NVector(stdDistanceVector).magnitude();
            System.out.println("mean tap detector: " + "qMag " + queryMag + " rMeanMag " + resultsMeanMag + " rStdMag " + resultsMeanStd + " stdDistanceMag " + stdDistanceMag + " pass " + ((stdDistanceMag > TAP_ACCEPT_THRESHOLD) ? "True" : "False"));
            if (stdDistanceMag > TAP_ACCEPT_THRESHOLD) {
                this.listener.onEventRecognized(detectDirection(data));
            }
            return stdDistanceMag > TAP_ACCEPT_THRESHOLD;
        }
    }
    @Override
    public String detectDirection(Tuple2<NVector> data) {
        String direction = data.getX().getValue(1) - data.getY().getValue(1) < 0 ? "left" : "right";
        System.out.println("meantapdetector " + direction);
        return direction;
    }
}
