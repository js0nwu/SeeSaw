Scripts used to generate per-file graphs (figures for a single trial). Most functions are copied from individual analysis files
Scripts meant to be run from the Magnetics/Python/Magnetics folder

file: figure_gen3.py
description: script to generate several figures for a particular trial file. All functions are at the bottom of the file and can be commented/uncommented
settings:
FIT_SQUARE - fit to a square wave instead of sine wave
PLOT_SENSOR - sensor to plot values from
SHOW_CORRELATION_WINDOWS - generate a correlation graph for every window
pairNum - the window size in cycles
USE_DELTAS - use delta algorithm for permutation thresholding
USE_STD_ALGORITHM - using standard deviation algorithm for permutation thresholding
NOISE_PERMUTATION - in the correlation distribution graphs, apply the permutation test onto the noise data and add that to the figure
COMPLETE_SHUFFLE - if false, correlation permutation test is generated by swapping the order of cycles. if true, uses a permutation of all of the data points
functions:
data_plot_distribution() - used with timer-sampled files - mainly for permutation thresholding. compares the distribution of the query values to distribution generated by permutation test
data_plot_raw_data() - used to generate figure of raw sensor data
data_lag_calculation() - find lags by using minima/maxima detection then find the lag between a reference wave and the signal data
data_correlation_graphs() - generate the correlation over time graph of the particular file - automatically uses cross correlation to adjust the reference signal to fit best
data_distribution_comparison() - plots the distribution similar to data_plot_distribution(), but also plots the distribution of a noise file
data_correlation_distribution() - compares the distribution of the noise file to the actual file, can also plot the permuted distribution of the actual data
data_3d_plot() - generates a 3d visualization of the vectors in a data file

usage: python figure_gen3.py [TARGET FOLDER w/o trailing slash]
examples:
generate figures for person 1 second browsing 1000 trial
python figure_gen3.py "User Study/Raw/P1/p1_t1478897598685_xbrowsing_1000_2"
generate figures for all experiments
find "User Study/Raw" -name "p*_*" -type d -exec figure_gen3.py {} \;

file: figure_gen4.py
description: script to generate the threshold graph of a particular trial file. By default, uses vector magnitude feature
usage: python figure_gen4.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files]
example:
generate figures for person 1 second browsing 1000 trial
python figure_gen4.py "User Study/Raw/P1/p1_t1478897598685_xbrowsing_1000_2" 0.0

file: figure_gen43.py
description: script to generate the threshold graph of a particular trial file. By default, uses diff-triangular feature.
usage: python figure_gen43.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files]
example:
generate figures for person 1 second browsing 1000 trial
python figure_gen43.py "User Study/Raw/P1/p1_t1478897598685_xbrowsing_1000_2" 0.0

file: figure_gen42.py
description: script to generate the threshold graph of a particular trial file. By default, uses diff-sine feature.
usage: python figure_gen2.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files]
example:
generate figures for person 1 second browsing 1000 trial
python figure_gen42.py "User Study/Raw/P1/p1_t1478897598685_xbrowsing_1000_2" 0.0

file: figure_gen4_noise.py
description: script to generate the threshold graph of a particular trial file. By default, uses magnitude feature.
usage: python figure_gen4_noise.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files]
example:
generate figures for person 1 second browsing 1000 trial
python figure_gen4_noise.py "User Study/Raw/P1/p1_t1478897598685_xbrowsing_1000_2" 0.0
generate figures for all noise files
python figure_gen4_noise.py "User Study/Raw" 0.0