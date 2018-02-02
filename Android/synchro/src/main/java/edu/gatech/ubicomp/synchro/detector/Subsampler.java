package edu.gatech.ubicomp.synchro.detector;

import edu.gatech.ubicomp.synchro.detector.Tuple2;

public interface Subsampler {
    Tuple2<double[]> processSample(double time, double[] sample);
}
