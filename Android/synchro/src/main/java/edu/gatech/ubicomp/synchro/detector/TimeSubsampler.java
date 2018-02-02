package edu.gatech.ubicomp.synchro.detector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwpilly on 2/16/17.
 */
public class TimeSubsampler implements Subsampler {

	private boolean debugMode = false;

	private List<double[]> sampleList;
    private double timeWindow;
    private double firstTime = -1;
	private double firstAvgTime = -1;
    private int windowCounter = 0;

    public TimeSubsampler(double timeWindow) {
        this.timeWindow = timeWindow;
        sampleList = new ArrayList<>();
    }

    public Tuple2<double[]> processSample(double time, double[] sample) {

        if (firstTime == -1) {
            firstTime = time;
        }
        double timeStart = firstTime + (windowCounter) * timeWindow;
        double timeEnd = firstTime + (windowCounter + 1) * timeWindow;

		if (time > timeEnd && sampleList.size() > 0) {

			double[] averageSample = new double[sampleList.get(0).length];
            for (int i = 0; i < sampleList.size(); i++) {
                for (int j = 0; j < averageSample.length; j++) {
                    averageSample[j] += sampleList.get(i)[j];
                }
            }
            for (int i = 0; i < averageSample.length; i++) {
                averageSample[i] /= sampleList.size();
            }

            double averageTime = timeStart + (timeWindow / 2);

			if (firstAvgTime == -1) {
				firstAvgTime = averageTime;
			}
			double deltaTime = averageTime - firstAvgTime;
			if(debugMode) System.out.println("averageTime : " + averageTime);

            sampleList = new ArrayList<>();
            sampleList.add(sample);
            windowCounter++;
            return new Tuple2<>(new double[] {averageTime}, averageSample);
        } else {
            sampleList.add(sample);
            return null;
        }
    }
}
