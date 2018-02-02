import csv
import random as rand
import numpy as np
import pandas as pd
import pprint as pp
import matplotlib.pyplot as pplot
import os
import seaborn as sns
import sys
from matplotlib import rcParams
from scipy.stats.stats import pearsonr
from scipy import signal
from scipy.stats import norm
from scipy.stats import gaussian_kde
from scipy.stats import linregress
import scipy.integrate as integrate
import shutil
from scipy.signal import detrend
from scipy.signal import correlate
from scipy.signal import resample
from scipy import signal
from functools import reduce

def magv(v):
    return np.sqrt(v.dot(v))
def tupleStat(tupleList, meanMode=False):
    leftTuples = tupleList[:,0]
    rightTuples = tupleList[:,1]
    if meanMode:
        leftMean = np.mean(leftTuples)
        rightMean = np.mean(rightTuples)
        diffMean = leftMean - rightMean
        return diffMean
    else:
        diffs = [leftTuples[i] - rightTuples[i] for i in range(len(leftTuples))]
        return np.std(np.array(diffs))

def tupleStatCombine(tupleList, meanMode=False, projectLeft=False):
    leftTuples = tupleList[:,0]
    rightTuples = tupleList[:,1]
    leftTuples = np.array(leftTuples)
    rightTuples = np.array(rightTuples)
    if meanMode:
        lt = leftTuples.mean(axis=0)
        rt = rightTuples.mean(axis=0)
        dotp = lt.dot(rt)/(magv(lt) if projectLeft else magv(rt))
        return dotp
    else:
        lt = leftTuples.std(axis=0)
        rt = rightTuples.std(axis=0)
        dotp = lt.dot(rt)/(magv(lt) if projectLeft else magv(rt))
        return dotp
    
def flip2Tuple(t):
    return (t[1], t[0])

def itemFlipper(tupleList, flipList):
    newList = []
    for i in range(len(tupleList)):
        if flipList[i]:
            newList.append(flip2Tuple(tupleList[i]))
        else:
            newList.append(tupleList[i])
    return newList
            

def comboIterator(tupleList):
    tupleLength = len(tupleList)
    combos = []
    for i in range(2**tupleLength-1):
        numerical = i+1
        flipList = [True if d == '1' else False for d in "{0:b}".format(numerical).zfill(tupleLength)]
        combos.append(itemFlipper(tupleList, flipList))
    return combos

def mag(x, y, z):
    return np.sqrt(x**2 + y**2 + z**2)

def plot_distribution(ax, data, bins, actual=None, color="blue", drawStds=3, vlines=None, vlinesColor="black", xLabel="x", yLabel="y", title="Title"):
    sns.distplot(data, rug=True, bins=bins, color=color, ax=ax)
    ax.set_title(title)
    ax.set_xlabel(xLabel)
    ax.set_ylabel(yLabel)
    if actual is not None:
        ax.axvline(x=actual, color=vlinesColor, linewidth=2, clip_on=False)
    if vlines is not None:
        for vline in vlines:
            ax.axvline(x=vline, color=vlinesColor, linewidth=0.5, linestyle="dashed")
    if drawStds > 0:
        for i in range(drawStds):
            stdLine = i + 1
            ax.axvline(x=np.mean(np.array(data)) - np.std(np.array(data)) * stdLine, linestyle="dashed", linewidth=0.5, color=color)
            ax.axvline(x=np.mean(np.array(data)) + np.std(np.array(data)) * stdLine, linestyle="dashed", linewidth=0.5, color=color)
    ax.axvline(x=np.mean(data), color=color, linewidth=1)

def diff_csv(csvData, rightMinusLeft=False):
    output = csvData.copy()
    lbx = 0
    lby = 0
    lbz = 0
    if rightMinusLeft:
        for i in range(len(csvData['lx'])):
            dax = csvData['rx'][i] - lbx
            day = csvData['ry'][i] - lby
            daz = csvData['rz'][i] - lbz
            dbx = csvData['lx'][i] - csvData['rx'][i]
            dby = csvData['ly'][i] - csvData['ry'][i]
            dbz = csvData['lz'][i] - csvData['rz'][i]
            lbx = csvData['lx'][i]
            lby = csvData['ly'][i]
            lbz = csvData['lz'][i]
            ddx = dbx - dax
            ddy = dby - day
            ddz = dbz - daz
            if i == 0:
                continue
            output['rx'][i] = dax
            output['ry'][i] = day
            output['rz'][i] = daz
            output['lx'][i] = dbx
            output['ly'][i] = dby
            output['lz'][i] = dbz
            output['dx'][i] = ddx
            output['dy'][i] = ddy
            output['dz'][i] = ddz
    else:
        for i in range(len(csvData['lx'])):
            dax = -csvData['rx'][i] + lbx
            day = -csvData['ry'][i] + lby
            daz = -csvData['rz'][i] + lbz
            dbx = -csvData['lx'][i] + csvData['rx'][i]
            dby = -csvData['ly'][i] + csvData['ry'][i]
            dbz = -csvData['lz'][i] + csvData['rz'][i]
            lbx = csvData['lx'][i]
            lby = csvData['ly'][i]
            lbz = csvData['lz'][i]
            ddx = dbx - dax
            ddy = dby - day
            ddz = dbz - daz
            if i == 0:
                continue
            output['rx'][i] = dax
            output['ry'][i] = day
            output['rz'][i] = daz
            output['lx'][i] = dbx
            output['ly'][i] = dby
            output['lz'][i] = dbz
            output['dx'][i] = ddx
            output['dy'][i] = ddy
            output['dz'][i] = ddz
    return output

def get_ewma(df, alpha=1):
    # Compute alpha and exponential moving average
    span = (2-alpha)/alpha
    df_smooth = pd.ewma(df, span=span, adjust=False)
    return df_smooth

def data_kde(data):
    return gaussian_kde(data, bw_method="scott")

def data_grid(data, kde, bw="scotts", cut=3, gridsize=500):
    adata = np.array(data)
    bw = getattr(kde, "%s_factor" % bw)() * np.std(adata)
    clip = (-np.inf, np.inf)
    support_min = max(adata.min() - bw * cut, clip[0])
    support_max = min(adata.max() + bw * cut, clip[1])
    return np.linspace(support_min, support_max, gridsize)

def univariate_kde(data):
    data = np.array(data)
    dkde = data_kde(data)
    dgrid = data_grid(data, dkde)
    return (dgrid, dkde)

def kde_intersect(u1, u2, middle_factor=0.1):
    d1 = u1[0]
    d2 = u2[0]
    d1a = d1.min()
    d1b = d1.max()
    d2a = d2.min()
    d2b = d2.max()
    if d1b < d2a or d2b < d1a:
        return None
    if d1a < d2a:
        db = (d2a, d1b)
    else:
        db = (d1a, d2b)
    ds = [x for x in np.concatenate((d1, d2)) if x >= db[0] and x <= db[1]]
    dslen = max(ds) - min(ds)
    ds = np.array([x for x in ds if x >= db[0] + middle_factor * dslen and x <= db[1] - middle_factor * dslen])
    kde1 = u1[1]
    kde2 = u2[1]
    diffs = np.abs(kde1(ds) - kde2(ds))
    mindiff = ds[np.argmin(diffs)]
    return (mindiff, db[0], db[1])
    
def distribution_accuracy(q, d):
    i = kde_intersect(q, d)
    if i is None:
        return (0, 0, 0)
    if q[0].min() < d[0].min():
        a = integrate.quad(d[1], i[1], i[0])[0]
        b = integrate.quad(q[1], i[0], i[2])[0]
        return (a + b, a, b)
    else:
        a = integrate.quad(q[1], i[1], i[0])[0]
        b = integrate.quad(d[1], i[0], i[2])[0]
        return (a + b, a, b)

def remove_trend(x):
    return detrend(x)

#http://stackoverflow.com/a/4690225
def find_timeshift(a, b):
    shiftFactor = a.size - 1
    correlateIndex = np.argmax(correlate(a, b))
    return shiftFactor - correlateIndex

def duty_function_permutation(permutation, repeat):
    d = []
    for p in permutation:
        d.extend([p] * repeat)
    return np.array(d)

def delta_space_transform(leftVector, rightVector, currentVector, lastLeft):
    deltaVector = leftVector - rightVector
    currentDeltaVector = currentVector - leftVector if lastLeft else currentVector - rightVector
    scalarProjection = currentDeltaVector.dot(deltaVector) / magv(deltaVector)
    return scalarProjection
    
# peak detection code taken from https://gist.github.com/sixtenbe/1178136

def _datacheck_peakdetect(x_axis, y_axis):
    if x_axis is None:
        x_axis = range(len(y_axis))
    
    if len(y_axis) != len(x_axis):
        raise ValueError( 
                "Input vectors y_axis and x_axis must have same length")
    
    #needs to be a numpy array
    y_axis = np.array(y_axis)
    x_axis = np.array(x_axis)
    return x_axis, y_axis

def peakdetect(y_axis, x_axis = None, lookahead = 200, delta=0):
    """
    Converted from/based on a MATLAB script at: 
    http://billauer.co.il/peakdet.html
    
    function for detecting local maxima and minima in a signal.
    Discovers peaks by searching for values which are surrounded by lower
    or larger values for maxima and minima respectively
    
    keyword arguments:
    y_axis -- A list containing the signal over which to find peaks
    
    x_axis -- A x-axis whose values correspond to the y_axis list and is used
        in the return to specify the position of the peaks. If omitted an
        index of the y_axis is used.
        (default: None)
    
    lookahead -- distance to look ahead from a peak candidate to determine if
        it is the actual peak
        (default: 200) 
        '(samples / period) / f' where '4 >= f >= 1.25' might be a good value
    
    delta -- this specifies a minimum difference between a peak and
        the following points, before a peak may be considered a peak. Useful
        to hinder the function from picking up false peaks towards to end of
        the signal. To work well delta should be set to delta >= RMSnoise * 5.
        (default: 0)
            When omitted delta function causes a 20% decrease in speed.
            When used Correctly it can double the speed of the function
    
    
    return: two lists [max_peaks, min_peaks] containing the positive and
        negative peaks respectively. Each cell of the lists contains a tuple
        of: (position, peak_value) 
        to get the average peak value do: np.mean(max_peaks, 0)[1] on the
        results to unpack one of the lists into x, y coordinates do: 
        x, y = zip(*max_peaks)
    """
    max_peaks = []
    min_peaks = []
    dump = []   #Used to pop the first hit which almost always is false
       
    # check input data
    x_axis, y_axis = _datacheck_peakdetect(x_axis, y_axis)
    # store data length for later use
    length = len(y_axis)
    
    
    #perform some checks
    if lookahead < 1:
        raise ValueError("Lookahead must be '1' or above in value")
    if not (np.isscalar(delta) and delta >= 0):
        raise ValueError("delta must be a positive number")
    
    #maxima and minima candidates are temporarily stored in
    #mx and mn respectively
    mn, mx = np.Inf, -np.Inf
    
    #Only detect peak if there is 'lookahead' amount of points after it
    for index, (x, y) in enumerate(zip(x_axis[:-lookahead], 
                                        y_axis[:-lookahead])):
        if y > mx:
            mx = y
            mxpos = x
        if y < mn:
            mn = y
            mnpos = x
        
        ####look for max####
        if y < mx-delta and mx != np.Inf:
            #Maxima peak candidate found
            #look ahead in signal to ensure that this is a peak and not jitter
            if y_axis[index:index+lookahead].max() < mx:
                max_peaks.append([mxpos, mx])
                dump.append(True)
                #set algorithm to only find minima now
                mx = np.Inf
                mn = np.Inf
                if index+lookahead >= length:
                    #end is within lookahead no more peaks can be found
                    break
                continue
            #else:  #slows shit down this does
            #    mx = ahead
            #    mxpos = x_axis[np.where(y_axis[index:index+lookahead]==mx)]
        
        ####look for min####
        if y > mn+delta and mn != -np.Inf:
            #Minima peak candidate found 
            #look ahead in signal to ensure that this is a peak and not jitter
            if y_axis[index:index+lookahead].min() > mn:
                min_peaks.append([mnpos, mn])
                dump.append(False)
                #set algorithm to only find maxima now
                mn = -np.Inf
                mx = -np.Inf
                if index+lookahead >= length:
                    #end is within lookahead no more peaks can be found
                    break
            #else:  #slows shit down this does
            #    mn = ahead
            #    mnpos = x_axis[np.where(y_axis[index:index+lookahead]==mn)]
    
    
    #Remove the false hit on the first value of the y_axis
    try:
        if dump[0]:
            max_peaks.pop(0)
        else:
            min_peaks.pop(0)
        del dump
    except IndexError:
        #no peaks were found, should the function return empty lists?
        pass
        
    return [max_peaks, min_peaks]

sns.set(color_codes=True)
rcParams.update({'figure.autolayout': True})
FIGURE_SIZE = (20, 20)
import warnings
warnings.filterwarnings("ignore")

MEAN_GRAPH = False
WINDOWS = False
FILE_FORMAT = ".pdf"

TAP_ACCEPT_THRESHOLD = 2.0
CORRELATION_ACCEPT_THRESHOLD = 0.7

pairNum = 4

alphaFactors = {"magnet":1, "accel":0.02, "gyro":0.003}

START_PADDING = 0.1
END_PADDING = 0.1

SAMPLING_PERIOD = 10
SAMPLING_WINDOW = 400

SHOW_CORRELATION_WINDOWS = False
CORRELATION_EWMA = True

CSV_PATH = "./" + sys.argv[1] + "/"
FOLDER_PREFIX = CSV_PATH + "/figures/"

CSV_PREFIX = sys.argv[1].split("/")[-1]
CSV_NOISE_PREFIX = CSV_PREFIX + "_noise"

CSV_MAG_SUFFIX = "_scombinedMag.csv"
CSV_ACCEL_SUFFIX = "_scombinedAccel.csv"
CSV_GYRO_SUFFIX = "_scombinedGyro.csv"
CSV_SENSOR_SUFFIX = "_ssensorData.csv"

USE_DELTAS = False 
USE_STD_ALGORITHM = False 
PLOT_SENSOR = "magnet"
NOISE_PERMUTATION = False
COMPLETE_SHUFFLE = False 

SAVE_FILES = True
FILE_OUTPUT = False

FIT_SQUARE = True

def generate_filename(basename):
    return FOLDER_PREFIX + CSV_PREFIX + "_" + (("" if USE_DELTAS else "d") + ("S" if USE_STD_ALGORITHM else "s")) + "_" + PLOT_SENSOR + "_" + basename + FILE_FORMAT

def initialize_environment():
    print("CSV_PREFIX", CSV_PREFIX)
    global CSV_MAG_FILE
    global CSV_ACCEL_FILE
    global CSV_GYRO_FILE
    global CSV_SENSOR_FILE
    global CSV_NOISE_MAG_FILE
    global CSV_NOISE_ACCEL_FILE
    global CSV_NOISE_GYRO_FILE
    global CSV_NOISE_SENSOR_FILE
    CSV_MAG_FILE = CSV_PATH + CSV_PREFIX + CSV_MAG_SUFFIX
    CSV_ACCEL_FILE = CSV_PATH + CSV_PREFIX + CSV_ACCEL_SUFFIX
    CSV_GYRO_FILE = CSV_PATH + CSV_PREFIX + CSV_GYRO_SUFFIX
    CSV_SENSOR_FILE = CSV_PATH + CSV_PREFIX + CSV_SENSOR_SUFFIX
    CSV_NOISE_MAG_FILE = CSV_PATH + CSV_NOISE_PREFIX + CSV_MAG_SUFFIX
    CSV_NOISE_ACCEL_FILE = CSV_PATH + CSV_NOISE_PREFIX + CSV_ACCEL_SUFFIX
    CSV_NOISE_GYRO_FILE = CSV_PATH + CSV_NOISE_PREFIX + CSV_GYRO_SUFFIX
    CSV_NOISE_SENSOR_FILE = CSV_PATH + CSV_NOISE_PREFIX + CSV_SENSOR_SUFFIX
    global csvMagData
    global csvSensorData
    global csvNoiseMagData
    global csvNoiseSensorData
    csvMagData = pd.read_csv(CSV_MAG_FILE, names=['lx','ly','lz','rx','ry','rz','dx','dy','dz', ''])
    csvSensorData = pd.read_csv(CSV_SENSOR_FILE, names=['time','sensor','x','y','z','timestamp','linecounter',''])
    csvNoiseMagData = pd.read_csv(CSV_NOISE_MAG_FILE, names=['lx','ly','lz','rx','ry','rz','dx','dy','dz', ''])
    csvNoiseSensorData = pd.read_csv(CSV_NOISE_SENSOR_FILE, names=['time','sensor','x','y','z','timestamp','linecounter',''])
    if not os.path.exists(FOLDER_PREFIX):
        os.mkdir(FOLDER_PREFIX)


def data_plot_distribution():
    csvData = csvMagData
    if USE_DELTAS:
        csvData = diff_csv(csvData)
    xResults_all = []
    yResults_all = []
    zResults_all = []
    magResults_all = []
    queryX_all = []
    queryY_all = []
    queryZ_all = []
    queryMag_all = []
    for window in range(len(csvData) - pairNum):
        resultsX = []
        resultsY = []
        resultsZ = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        for p in range(pairNum): # creates the tuples from the current window
            tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
            tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
            tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))
        queryX = tupleStat(np.array(tupleListX), not USE_STD_ALGORITHM)
        queryY = tupleStat(np.array(tupleListY), not USE_STD_ALGORITHM)
        queryZ = tupleStat(np.array(tupleListZ), not USE_STD_ALGORITHM)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        if WINDOWS:
            pplot.clf()
            f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
            f.set_size_inches(FIGURE_SIZE)
            plot_distribution(ax1, xResults_i, 10, queryX, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
            plot_distribution(ax2, yResults_i, 10, queryY, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
            plot_distribution(ax3, zResults_i, 10, queryZ, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
            plot_distribution(ax4, magResults_i, 10, query_mag, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
            pplot.savefig(generate_filename("dist_window" + str(window))) if SAVE_FILES else pplot.show()

        xResults_all.append(xResults_i)
        yResults_all.append(yResults_i)
        zResults_all.append(zResults_i)
        magResults_all.append(magResults_i)
        queryX_all.append(queryX)
        queryY_all.append(queryY)
        queryZ_all.append(queryZ)
        queryMag_all.append(query_mag)

    pplot.clf()
    f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
    f.set_size_inches(FIGURE_SIZE)
    xResults = [item for sublist in xResults_all for item in sublist]
    yResults = [item for sublist in yResults_all for item in sublist]
    zResults = [item for sublist in zResults_all for item in sublist]
    magResults = [item for sublist in magResults_all for item in sublist]
    plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, queryX_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax2, yResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax2, queryY_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax3, zResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax3, queryZ_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax4, magResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax4, queryMag_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    pplot.savefig(generate_filename("dist_summary_all")) if SAVE_FILES else pplot.show()
    if MEAN_GRAPH:
        pplot.clf()
        f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
        f.set_size_inches(FIGURE_SIZE)
        plot_distribution(ax1, np.mean(np.array(xResults_all), axis=0), 10, np.mean(np.array(queryX_all)), "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
        plot_distribution(ax2, np.mean(np.array(yResults_all), axis=0), 10, np.mean(np.array(queryY_all)), "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
        plot_distribution(ax3, np.mean(np.array(zResults_all), axis=0), 10, np.mean(np.array(queryZ_all)), "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
        plot_distribution(ax4, np.mean(np.array(magResults_all), axis=0), 10, np.mean(np.array(queryMag_all)), "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
        pplot.savefig(generate_filename("dist_summary_mean")) if SAVE_FILES else pplot.show()

def data_plot_raw_data():
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    plotT = []
    plotTL = []
    pplot.clf()
    lx = 0
    ly = 0
    lz = 0
    rx = 0
    ry = 0
    rz = 0
    lastLeft = None
    for rowI in range(len(csvData['sensor'])):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            plotX.append((t, x))
            plotY.append((t, y))
            plotZ.append((t, z))
            plotMag.append((t, mag(x, y, z)))
            if lx != 0 and ly != 0 and lz != 0 and rx != 0 and ry != 0 and rz != 0:
                lV = np.array([lx, ly, lz])
                rV = np.array([rx, ry, rz])
                cV = np.array([x, y, z])
                tS = delta_space_transform(lV, rV, cV, False)
                tSL = delta_space_transform(lV, rV, cV, lastLeft)
                plotT.append((t, tS))
                plotTL.append((t, tSL))
        elif row == 'left':
            lx = x
            ly = y
            lz = z
            lastLeft = True
            pplot.axvline(x=csvData['time'][rowI], color='red')
        elif row == 'right':
            rx = x
            ry = y
            rz = z
            lastLeft = False
            pplot.axvline(x=csvData['time'][rowI], color='black')
        elif row == 'vibrate':
            pplot.axvline(x=csvData['time'][rowI], color='green')
    plotX = np.array(plotX)
    plotY = np.array(plotY)
    plotZ = np.array(plotZ)
    plotMag = np.array(plotMag)
    plotT = np.array(plotT)
    plotTL = np.array(plotTL)
    pplot.plot(plotX[:,0], get_ewma(plotX[:,1], alphaFactors[PLOT_SENSOR]), color='red')
    pplot.plot(plotY[:,0], get_ewma(plotY[:,1], alphaFactors[PLOT_SENSOR]), color='green')
    pplot.plot(plotZ[:,0], get_ewma(plotZ[:,1], alphaFactors[PLOT_SENSOR]), color='blue')
    pplot.plot(plotMag[:,0], get_ewma(plotMag[:,1], 1), color='turquoise')
    pplot.plot(plotT[:,0], get_ewma(plotT[:,1], alphaFactors[PLOT_SENSOR]), color='magenta')
    pplot.plot(plotTL[:,0], get_ewma(plotTL[:,1], alphaFactors[PLOT_SENSOR]), color='purple')
    pplot.savefig(generate_filename("raw_data")) if SAVE_FILES else pplot.show()

def data_xcorrelation_graph():
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = int(sensorDataLength * START_PADDING)
    endIndex = int(sensorDataLength - (sensorDataLength * END_PADDING))
    pplot.clf()
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            plotX.append((t, x))
            plotY.append((t, y))
            plotZ.append((t, z))
            plotMag.append((t, mag(x, y, z)))
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
    plotX = np.array(plotX)
    plotY = np.array(plotY)
    plotZ = np.array(plotZ)
    plotMag = np.array(plotMag)
    plotS = plotMag
    plotS[:,1] = get_ewma(plotS[:,1], alpha=0.1)
    plotS[:,1] = remove_trend(plotS[:,1])
    signal_range = plotS[:,0]
    signal_period = rightEvents[1] - rightEvents[0]
    reference_signal = signal.square((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*leftEvents[0])) if FIT_SQUARE else np.sin((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*leftEvents[0])) # arcsin(1)
    rfactor = signal_range.size * 20
    rs2 = resample(reference_signal, rfactor, t=signal_range)
    ps2 = resample(plotS[:,1], rfactor, t=signal_range)
    resample_time = (signal_range[-1] - signal_range[0]) / rfactor
    rdiff = signal_range[-1] - signal_range[0]
    resample_range = np.arange(-rdiff, rdiff, resample_time)[:-1]
    rs2 = rs2[0]
    ps2 = ps2[0]
    a = rs2
    v = ps2
    a = (a - a.mean()) / (a.std() * a.size)
    v = (v - v.mean()) / v.std()
    plotX = np.array(resample_range)
    plotY = correlate(a, v)
    pplot.plot(plotX, plotY)
    pplot.savefig(generate_filename("xcorrelation_plot")) if SAVE_FILES else pplot.show()

def data_lag_calculation():
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = int(sensorDataLength * START_PADDING)
    endIndex = int(sensorDataLength - (sensorDataLength * END_PADDING))
    pplot.clf()
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            plotX.append((t, x))
            plotY.append((t, y))
            plotZ.append((t, z))
            plotMag.append((t, mag(x, y, z)))
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
            pplot.axvline(x=csvData['time'][rowI], color='red')
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
            pplot.axvline(x=csvData['time'][rowI], color='black')
    plotX = np.array(plotX)
    plotY = np.array(plotY)
    plotZ = np.array(plotZ)
    plotMag = np.array(plotMag)
    plotS = plotMag
    plotS[:,1] = get_ewma(plotS[:,1], alpha=0.1)
    plotS[:,1] = remove_trend(plotS[:,1])
    pplot.plot(plotS[:,0], plotS[:,1])
    signal_range = plotS[:,0]
    signal_period = rightEvents[1] - rightEvents[0]
    reference_signal = signal.square((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*leftEvents[0])) if FIT_SQUARE else np.sin((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*leftEvents[0])) # arcsin(1)
    pplot.plot(signal_range, reference_signal + np.mean(plotS[:,1]), color='g', linewidth=0.5)
    maxesData, minsData = peakdetect(plotS[:,1], plotS[:,0], lookahead=1, delta=1)
    for m in minsData:
        x ,y = m
        pplot.plot(x,y,'ro')
    combinedData = minsData
    lags = []
    for i in range(len(rightEvents)):
        distances = [abs(rightEvents[i] - p[0]) for p in combinedData]
        lags.append(min(distances))
    print("lags")
    print(lags)
    print("avg lag time ", np.mean(np.array(lags)))
    lag_reference_signal = signal.square((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*(leftEvents[0]-np.mean(np.array(lags))))) if FIT_SQUARE else np.sin((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*(leftEvents[0]-np.mean(np.array(lags))))) # arcsin(1)
    # pplot.plot(signal_range, lag_reference_signal + np.mean(plotS[:,1]), color='blue', linewidth=0.5)
    rfactor = signal_range.size * 2
    rs2 = resample(reference_signal, rfactor, t=signal_range)
    ps2 = resample(plotS[:,1], rfactor, t=signal_range)
    ts2 = rs2[1]
    rs2 = rs2[0]
    ps2 = ps2[0]
    resample_time = (signal_range[-1] - signal_range[0]) / rfactor
    lag_factor = find_timeshift(rs2, ps2)
    print(np.max(abs(correlate(rs2, ps2)))/ rfactor)
    lag_time = resample_time * lag_factor
    correlate_reference_signal = signal.square((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*(leftEvents[0]+lag_time))) if FIT_SQUARE else np.sin((2*np.pi/signal_period)*signal_range + (1.57079633 - (2*np.pi/signal_period)*(leftEvents[0]+lag_time))) # arcsin(1)
    pplot.plot(signal_range, correlate_reference_signal + np.mean(plotS[:,1]), color='red', linewidth=0.5)
    pplot.savefig(generate_filename("lags_plot")) if SAVE_FILES else pplot.show()
    print("correlation against reference, ref - lag, ref + max correlation")
    print(pearsonr(reference_signal,plotS[:,1]))
    print(pearsonr(lag_reference_signal,plotS[:,1]))
    print(pearsonr(correlate_reference_signal,plotS[:,1]))
    
def data_correlation_graphs():
    csvData = csvSensorData
    valueX = []
    valueY = []
    valueZ = []
    valueMag = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = 0
    endIndex = sensorDataLength
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            valueX.append((t, x))
            valueY.append((t, y))
            valueZ.append((t, z))
            valueMag.append((t, mag(x, y, z)))
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
    valueX = np.array(valueX)
    valueY = np.array(valueY)
    valueZ = np.array(valueZ)
    valueMag = np.array(valueMag)
    signalPeriod = rightEvents[1] - rightEvents[0]
    referenceSignal = lambda x : signal.square((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) # arcsin(1)
    windowTime = signalPeriod * pairNum
    valueS = valueMag
    correlationCoefficients = []
    totalTime = leftEvents[-1] - leftEvents[0]
    waveLengths = [] 
    for rowI in range(0, totalTime, totalTime // 150):
        startWindow = leftEvents[0]
        endWindow = leftEvents[0] + rowI
        print(startWindow, endWindow)
        window = []
        for v in valueS:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                window.append(v)
        if len(window) < 2:
            continue
        waveLengths.append(rowI)
        print(len(window))
        window = np.array(window)
        generatedSignal = referenceSignal(window[:,0])
        actualSignal = remove_trend(window[:,1])
        signal_range = window[:,0]
        rfactor = signal_range.size * 2
        rs2 = resample(generatedSignal, rfactor, t=signal_range)
        ps2 = resample(actualSignal, rfactor, t=signal_range)
        ts2 = rs2[1]
        rs2 = rs2[0]
        ps2 = ps2[0]
        resample_time = (signal_range[-1] - signal_range[0]) / rfactor
        lag_factor = find_timeshift(rs2, ps2)
        lag_time = resample_time * lag_factor
        generatedSignal = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_time))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_time))) # arcsin(1)
        if CORRELATION_EWMA:
            actualSignal = get_ewma(actualSignal, alpha=0.1)
        cc = pearsonr(generatedSignal, actualSignal)
        correlationCoefficients.append(cc)
        print("correlation for window ", rowI)
        print(cc[0])
        if SHOW_CORRELATION_WINDOWS:
            pplot.clf()
            pplot.plot(window[:,0], window[:,1] - np.mean(window[:,1]), color="purple")
            pplot.plot(window[:,0], actualSignal)
            pplot.plot(window[:,0], generatedSignal + np.mean(actualSignal))
            for i in range(pairNum + 1):
                pplot.axvline(x=leftEvents[rowI + i], color='red')
            for i in range(pairNum + 1):
                pplot.axvline(x=rightEvents[rowI + i], color='black')
            pplot.savefig(generate_filename("correlate_window" + str(rowI))) if SAVE_FILES else pplot.show()

    correlationCoefficients = np.array(correlationCoefficients)
    pplot.clf()
    pplot.plot(np.array(waveLengths), correlationCoefficients[:,0])
    pplot.savefig(generate_filename("correlate_window_summary")) if SAVE_FILES else pplot.show()

def data_online_performance():
    permutationSDScores = []
    permutationSScores = []
    permutationMDScores = []
    permutationMScores = []
    correlationScores = []

    csvData = csvSensorData
    valueX = []
    valueY = []
    valueZ = []
    valueMag = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = 0
    endIndex = sensorDataLength
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            valueX.append((t, x))
            valueY.append((t, y))
            valueZ.append((t, z))
            valueMag.append((t, mag(x, y, z)))
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
    valueX = np.array(valueX)
    valueY = np.array(valueY)
    valueZ = np.array(valueZ)
    valueMag = np.array(valueMag)
    signalPeriod = rightEvents[1] - rightEvents[0]
    referenceSignal = lambda x : signal.square((2*np.pi/signalPeriod)*x + (-1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) # arcsin(-1) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*x + (-1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) # arcsin(-1)
    windowTime = signalPeriod * pairNum
    valueS = valueX
    for rowI in range(len(leftEvents) - pairNum):
        startWindow = leftEvents[rowI]
        endWindow = leftEvents[rowI + pairNum]
        window = []
        for v in valueS:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                window.append(v)
        window = np.array(window)
        generatedSignal = referenceSignal(window[:,0])
        actualSignal = remove_trend(window[:,1])
        signal_range = window[:,0]
        rfactor = (np.max(signal_range) - np.min(signal_range)) // signal_range.size
        ts2 = resample(signal_range, rfactor)
        rs2 = resample(generatedSignal, rfactor)
        ps2 = resample(actualSignal, rfactor)
        lag_factor = find_timeshift(rs2, ps2)
        lag_time = rfactor * lag_factor
        generatedSignal = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_time))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_time))) # arcsin(1)
        if CORRELATION_EWMA:
            actualSignal = get_ewma(actualSignal, alpha=0.1)
        cc = pearsonr(generatedSignal, actualSignal)
        score = abs(cc[0])
        direction = "left" if cc[0] > 0 else "right"
        correlationScores.append((score, direction))

    csvData = csvMagData
    csvData = diff_csv(csvData)
    for window in range(len(csvData) - pairNum + 1):
        resultsX = []
        resultsY = []
        resultsZ = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        for p in range(pairNum):
            tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
            tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
            tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))

        queryX = tupleStat(np.array(tupleListX), False)
        queryY = tupleStat(np.array(tupleListY), False)
        queryZ = tupleStat(np.array(tupleListZ), False)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), False))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), False))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), False))
        query_mag = mag(queryX, queryY, queryZ)
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        queryDistance = abs((query_mag - results_i_mean_mag) / results_i_std_mag)
        direction = "left" if csvMagData['ly'][window + p] - csvMagData['ry'][window + p] < 0 else "right"
        score = norm.cdf(queryDistance) - norm.cdf(-queryDistance)
        permutationSDScores.append((score, direction))

    csvData = csvMagData
    for window in range(len(csvData) - pairNum + 1):
        resultsX = []
        resultsY = []
        resultsZ = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        for p in range(pairNum):
            tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
            tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
            tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))

        queryX = tupleStat(np.array(tupleListX), False)
        queryY = tupleStat(np.array(tupleListY), False)
        queryZ = tupleStat(np.array(tupleListZ), False)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), False))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), False))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), False))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        queryDistance = abs((query_mag - results_i_mean_mag) / results_i_std_mag)
        direction = "left" if csvMagData['ly'][window + p] - csvMagData['ry'][window + p] < 0 else "right"
        score = norm.cdf(queryDistance) - norm.cdf(-queryDistance)
        permutationSScores.append((score, direction))

    csvData = csvMagData
    csvData = diff_csv(csvData)
    for window in range(len(csvData) - pairNum + 1):
        resultsX = []
        resultsY = []
        resultsZ = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        for p in range(pairNum):
            tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
            tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
            tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))

        queryX = tupleStat(np.array(tupleListX), True)
        queryY = tupleStat(np.array(tupleListY), True)
        queryZ = tupleStat(np.array(tupleListZ), True)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), True))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), True))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), True))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        queryDistance = abs((query_mag - results_i_mean_mag) / results_i_std_mag)
        direction = "left" if csvMagData['ly'][window + p] - csvMagData['ry'][window + p] < 0 else "right"
        score = norm.cdf(queryDistance) - norm.cdf(-queryDistance)
        permutationMDScores.append((score, direction))    

    csvData = csvMagData
    for window in range(len(csvData) - pairNum + 1):
        resultsX = []
        resultsY = []
        resultsZ = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        for p in range(pairNum):
            tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
            tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
            tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))

        queryX = tupleStat(np.array(tupleListX), True)
        queryY = tupleStat(np.array(tupleListY), True)
        queryZ = tupleStat(np.array(tupleListZ), True)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), True))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), True))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), True))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        queryDistance = abs((query_mag - results_i_mean_mag) / results_i_std_mag)
        direction = "left" if csvMagData['ly'][window + p] - csvMagData['ry'][window + p] < 0 else "right"
        score = norm.cdf(queryDistance) - norm.cdf(-queryDistance)
        permutationMScores.append((score, direction))      

    def printResultRow(row):
        return "{0:.4f}".format(row[0]) + " " + ("L" if row[1] == "left" else "R")
    print("online comparison scores")
    for i in range(len(correlationScores)):
        print("c:", printResultRow(correlationScores[i]), "psd:", printResultRow(permutationSDScores[i]), "ps:", printResultRow(permutationSScores[i]), "pmd:", printResultRow(permutationMDScores[i]), "pm:", printResultRow(permutationMScores[i]))

    pplot.clf()
    SCORE_DIRECTION = False
    if SCORE_DIRECTION:
        cGraph = [-x[0] if x[1] == "left" else x[0] for x in correlationScores]
        psdGraph = [-x[0] if x[1] == "left" else x[0] for x in permutationSDScores]
        psGraph = [-x[0] if x[1] == "left" else x[0] for x in permutationSScores]
        pmdGraph = [-x[0] if x[1] == "left" else x[0] for x in permutationMDScores]
        pmGraph = [-x[0] if x[1] == "left" else x[0] for x in permutationMScores]
        pplot.plot(np.array(cGraph))
        pplot.plot(np.array(psdGraph))
        pplot.plot(np.array(psGraph))
        pplot.plot(np.array(pmdGraph))
        pplot.plot(np.array(pmGraph))
    else:
        correlationScores = np.array(correlationScores)
        permutationSDScores = np.array(permutationSDScores)
        permutationSScores = np.array(permutationSScores)
        permutationMDScores = np.array(permutationMDScores)
        permutationMScores = np.array(permutationMScores)
        pplot.plot(correlationScores[:,0])
        pplot.plot(permutationSDScores[:,0])
        pplot.plot(permutationSScores[:,0])
        pplot.plot(permutationMDScores[:,0])
        pplot.plot(permutationMScores[:,0])

    pplot.legend(["Correlation", "Permutation Std. Dev. w/ Deltas", "Permutation Std. Dev.", "Permutation Mean w/ Deltas", "Permutation Mean"])
    pplot.savefig(generate_filename("online_comparison")) if SAVE_FILES else pplot.show()

def data_distribution_comparison():
    xResults_all = []
    yResults_all = []
    zResults_all = []
    magResults_all = []
    combineResults_all = []
    queryX_all = []
    queryY_all = []
    queryZ_all = []
    queryMag_all = []
    queryCombine_all = []
    xResults_all_noise = []
    yResults_all_noise = []
    zResults_all_noise = []
    magResults_all_noise = []
    combineResults_all_noise = []
    queryX_all_noise = []
    queryY_all_noise = []
    queryZ_all_noise = []
    queryMag_all_noise = []
    queryCombine_all_noise = []

    csvData = csvNoiseMagData
    if USE_DELTAS:
        csvData = diff_csv(csvData)
    for window in range(len(csvData) - pairNum):
        resultsX = []
        resultsY = []
        resultsZ = []
        resultsCombine = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        tupleListCombine = []
        for p in range(pairNum): # creates the tuples from the current window
            tx = (csvData['lx'][window + p], csvData['rx'][window + p])
            ty = (csvData['ly'][window + p], csvData['ry'][window + p])
            tz = (csvData['lz'][window + p], csvData['rz'][window + p])
            tupleListX.append(tx)
            tupleListY.append(ty)
            tupleListZ.append(tz)
            tupleListCombine.append(((tx[0],ty[0],tz[0]), (tx[1],ty[1],tz[1])))
        queryX = tupleStat(np.array(tupleListX), not USE_STD_ALGORITHM)
        queryY = tupleStat(np.array(tupleListY), not USE_STD_ALGORITHM)
        queryZ = tupleStat(np.array(tupleListZ), not USE_STD_ALGORITHM)
        queryCombine = tupleStatCombine(np.array(tupleListCombine), not USE_STD_ALGORITHM)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        windowCombosCombine = comboIterator(tupleListCombine)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        combineResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosCombine:
            combineResults_i.append(tupleStatCombine(np.array(i), not USE_STD_ALGORITHM))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_combine = np.std(combineResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        query_combine = queryCombine
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        xResults_all_noise.append(xResults_i)
        yResults_all_noise.append(yResults_i)
        zResults_all_noise.append(zResults_i)
        magResults_all_noise.append(magResults_i)
        combineResults_all_noise.append(combineResults_i)
        queryX_all_noise.append(queryX)
        queryY_all_noise.append(queryY)
        queryZ_all_noise.append(queryZ)
        queryMag_all_noise.append(query_mag)
        queryCombine_all_noise.append(query_combine)

    csvData = csvMagData
    if USE_DELTAS:
        csvData = diff_csv(csvData)
    for window in range(len(csvData) - pairNum):
        resultsX = []
        resultsY = []
        resultsZ = []
        resultsCombine = []
        tupleListX = []
        tupleListY = []
        tupleListZ = []
        tupleListCombine = []
        for p in range(pairNum): # creates the tuples from the current window
            tx = (csvData['lx'][window + p], csvData['rx'][window + p])
            ty = (csvData['ly'][window + p], csvData['ry'][window + p])
            tz = (csvData['lz'][window + p], csvData['rz'][window + p])
            tupleListX.append(tx)
            tupleListY.append(ty)
            tupleListZ.append(tz)
            tupleListCombine.append(((tx[0],ty[0],tz[0]), (tx[1],ty[1],tz[1])))
        queryX = tupleStat(np.array(tupleListX), not USE_STD_ALGORITHM)
        queryY = tupleStat(np.array(tupleListY), not USE_STD_ALGORITHM)
        queryZ = tupleStat(np.array(tupleListZ), not USE_STD_ALGORITHM)
        queryCombine = tupleStatCombine(np.array(tupleListCombine), not USE_STD_ALGORITHM)
        windowCombosX = comboIterator(tupleListX)
        windowCombosY = comboIterator(tupleListY)
        windowCombosZ = comboIterator(tupleListZ)
        windowCombosCombine = comboIterator(tupleListCombine)
        xResults_i = []
        yResults_i = []
        zResults_i = []
        combineResults_i = []
        for i in windowCombosX:
            xResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosY:
            yResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosZ:
            zResults_i.append(tupleStat(np.array(i), not USE_STD_ALGORITHM))
        for i in windowCombosCombine:
            combineResults_i.append(tupleStatCombine(np.array(i), not USE_STD_ALGORITHM))
        std_distance_x = np.std(xResults_i)
        std_distance_y = np.std(yResults_i)
        std_distance_z = np.std(zResults_i)
        std_distance_combine = np.std(combineResults_i)
        std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
        query_mag = mag(queryX, queryY, queryZ)
        query_combine = queryCombine
        results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
        results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
        magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
        xResults_all.append(xResults_i)
        yResults_all.append(yResults_i)
        zResults_all.append(zResults_i)
        magResults_all.append(magResults_i)
        combineResults_all.append(combineResults_i)
        queryX_all.append(queryX)
        queryY_all.append(queryY)
        queryZ_all.append(queryZ)
        queryMag_all.append(query_mag)
        queryCombine_all.append(query_combine)
    pplot.clf()
    f, (ax1, ax2, ax3, ax4, ax5) = pplot.subplots(5)
    f.set_size_inches(FIGURE_SIZE)
    xResults = [item for sublist in xResults_all for item in sublist]
    yResults = [item for sublist in yResults_all for item in sublist]
    zResults = [item for sublist in zResults_all for item in sublist]
    magResults = [item for sublist in magResults_all for item in sublist]
    combineResults = [item for sublist in combineResults_all for item in sublist]
    xnResults = [item for sublist in xResults_all_noise for item in sublist]
    ynResults = [item for sublist in yResults_all_noise for item in sublist]
    znResults = [item for sublist in zResults_all_noise for item in sublist]
    magnResults = [item for sublist in magResults_all_noise for item in sublist]
    combinenResults = [item for sublist in combineResults_all_noise for item in sublist]
    plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, queryX_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, queryX_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax2, yResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax2, queryY_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax2, queryY_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax3, zResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax3, queryZ_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax3, queryZ_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax4, magResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax4, queryMag_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax4, queryMag_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax5, combineResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude 2")
    plot_distribution(ax5, queryCombine_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude 2")
    plot_distribution(ax5, queryCombine_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Combined Axes Dot Product")
    
    if NOISE_PERMUTATION:
        plot_distribution(ax1, xnResults, 10, None, "yellow", xLabel="Mean", yLabel="Density", title="Query X Distribution")
        plot_distribution(ax2, ynResults, 10, None, "yellow", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
        plot_distribution(ax3, znResults, 10, None, "yellow", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
        plot_distribution(ax4, magnResults, 10, None, "yellow", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
        plot_distribution(ax5, combinenResults, 10, None, "yellow", xLabel="Mean", yLabel="Density", title="Combined Axes Projection")
    pplot.savefig(generate_filename("dist_comparison")) if SAVE_FILES else pplot.show()
    
    rl = []

    xrkde = univariate_kde(xResults)
    xqkde = univariate_kde(queryX_all)
    xnkde = univariate_kde(queryX_all_noise)
    xqra = distribution_accuracy(xqkde, xrkde)
    xqna = distribution_accuracy(xqkde, xnkde)
    rl.append(("X-axis", xqra[0], xqra[1], xqra[2], xqna[0], xqna[1], xqna[2]))

    yrkde = univariate_kde(yResults)
    yqkde = univariate_kde(queryY_all)
    ynkde = univariate_kde(queryY_all_noise)
    yqra = distribution_accuracy(yqkde, yrkde)
    yqna = distribution_accuracy(yqkde, ynkde)
    rl.append(("Y-axis", yqra[0], yqra[1], yqra[2], yqna[0], yqna[1], yqna[2]))

    zrkde = univariate_kde(zResults)
    zqkde = univariate_kde(queryZ_all)
    znkde = univariate_kde(queryZ_all_noise)
    zqra = distribution_accuracy(zqkde, zrkde)
    zqna = distribution_accuracy(zqkde, znkde)
    rl.append(("Z-axis", zqra[0], zqra[1], zqra[2], zqna[0], zqna[1], zqna[2]))

    mrkde = univariate_kde(magResults)
    mqkde = univariate_kde(queryMag_all)
    mnkde = univariate_kde(queryMag_all_noise)
    mqra = distribution_accuracy(mqkde, mrkde)
    mqna = distribution_accuracy(mqkde, mnkde)
    rl.append(("Combined Magnitude", mqra[0], mqra[1], mqra[2], mqna[0], mqna[1], mqna[2]))
    
    crkde = univariate_kde(combineResults)
    cqkde = univariate_kde(queryCombine_all)
    cnkde = univariate_kde(queryCombine_all_noise)
    cqra = distribution_accuracy(cqkde, crkde)
    cqna = distribution_accuracy(cqkde, cnkde)
    rl.append(("Combined Projection", cqra[0], cqra[1], cqra[2], cqna[0], cqna[1], cqna[2]))

    fs = "{0:.5f}"
    print("error vs permutation test and noise")
    for r in rl:
        print(r[0], "pe", fs.format(r[1]), "pfp", fs.format(r[2]), "pfn", fs.format(r[3]), "ne", fs.format(r[4]), "nfp", fs.format(r[5]), "nfn", fs.format(r[6]))
        
def data_correlation_distribution():
    ccX = []
    ccY = []
    ccZ = []
    ccMag = []
    ccP = []
    ccXn = []
    ccYn = []
    ccZn = []
    ccMagn = []
    ccPn=[]
    xResults_all = []
    yResults_all = []
    zResults_all = []
    magResults_all = []
    pResults_all = []
    csvData = csvSensorData
    vectors = []
    startIndex = 0
    sensorDataLength = len(csvData['sensor'])
    endIndex = sensorDataLength
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            vectors.append([x,y,z])

    from sklearn.decomposition import PCA
    pca = PCA(n_components=1)
    vectors = np.array(vectors)
    pca.fit(vectors)
    print(pca.components_[0])
    csvData = csvNoiseSensorData
    valueX = []
    valueY = []
    valueZ = []
    valueMag = []
    valueP = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = 0
    endIndex = sensorDataLength
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            valueX.append((t, x))
            valueY.append((t, y))
            valueZ.append((t, z))
            valueP.append([x,y,z])
            valueMag.append((t, mag(x, y, z)))
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
    valueX = np.array(valueX)
    valueY = np.array(valueY)
    valueZ = np.array(valueZ)
    valueMag = np.array(valueMag)
    valueP = np.array(valueP)
    valueP = pca.transform(valueP)
    signalPeriod = rightEvents[1] - rightEvents[0]
    defaultDuty = [(1,0)] * pairNum
    referenceDuty = lambda x : duty_function_permutation(reduce(lambda x, y : x + y, defaultDuty), int(x.size / (pairNum * 2)))
    referenceSignal = lambda x : signal.square((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0]), duty=referenceDuty(x)) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) # arcsin(1)
    windowTime = signalPeriod * pairNum
    dutyPermutes = [reduce(lambda x, y : x + y, r) for r in comboIterator(defaultDuty)]
    for rowI in range(len(leftEvents) - pairNum):
        startWindow = leftEvents[rowI]
        endWindow = leftEvents[rowI + pairNum]
        windowX = []
        windowY = []
        windowZ = []
        windowMag = []
        windowP = []
        for v in valueX:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowX.append(v)
        for v in valueY:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowY.append(v)
        for v in valueZ:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowZ.append(v)
        for v in valueMag:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowMag.append(v)
        for v in range(len(valueP)):
            valueTime = valueX[v][0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowP.append(valueP[v])
        windowX = np.array(windowX)
        windowY = np.array(windowY)
        windowZ = np.array(windowZ)
        windowMag = np.array(windowMag)
        windowP = np.array(windowP)
        signal_range = windowX[:,0]
        repeats = int(signal_range.size / (pairNum * 2))
        slength = repeats * pairNum * 2
        signal_range = signal_range[:slength]
        generatedSignal = referenceSignal(signal_range)
        actualSignalX = remove_trend(windowX[:,1][:slength])
        actualSignalY = remove_trend(windowY[:,1][:slength])
        actualSignalZ = remove_trend(windowZ[:,1][:slength])
        actualSignalMag = remove_trend(windowMag[:,1][:slength])
        actualSignalP = remove_trend(windowP[:,0][:slength])
        if CORRELATION_EWMA:
            actualSignalX = get_ewma(actualSignalX, alpha=0.1)
            actualSignalY = get_ewma(actualSignalY, alpha=0.1)
            actualSignalZ = get_ewma(actualSignalZ, alpha=0.1)
            actualSignalMag = get_ewma(actualSignalMag, alpha=0.1)
            actualSignalP = get_ewma(actualSignalP, alpha=0.1)
        rfactor = signal_range.size * 2
        rs2 = resample(generatedSignal, rfactor, t=signal_range)
        ps2x = resample(actualSignalX, rfactor, t=signal_range)[0]
        ps2y = resample(actualSignalY, rfactor, t=signal_range)[0]
        ps2z = resample(actualSignalZ, rfactor, t=signal_range)[0]
        ps2m = resample(actualSignalMag, rfactor, t=signal_range)[0]
        ps2p = resample(actualSignalP, rfactor, t=signal_range)[0]
        ts2 = rs2[1]
        rs2 = rs2[0]
        resample_time = (signal_range[-1] - signal_range[0]) / rfactor
        lag_factorx = find_timeshift(rs2, ps2x)
        lag_timex = resample_time * lag_factorx
        generatedSignalx = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timex))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timex))) # arcsin(1)
        lag_factory = find_timeshift(rs2, ps2y)
        lag_timey = resample_time * lag_factory
        generatedSignaly = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timey))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timey))) # arcsin(1)
        lag_factorz = find_timeshift(rs2, ps2z)
        lag_timez = resample_time * lag_factorz
        generatedSignalz = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timez))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timez))) # arcsin(1)
        lag_factorm = find_timeshift(rs2, ps2m)
        lag_timem = resample_time * lag_factorm
        generatedSignalm = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timem))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timem))) # arcsin(1)
        lag_factorp = find_timeshift(rs2, ps2p)
        lag_timep = resample_time * lag_factorp
        generatedSignalp = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timep))) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timep))) # arcsin(1)

        ccXn.append(pearsonr(generatedSignalx, actualSignalX))
        ccYn.append(pearsonr(generatedSignaly, actualSignalY))
        ccZn.append(pearsonr(generatedSignalz, actualSignalZ))
        ccMagn.append(pearsonr(generatedSignalm, actualSignalMag))
        ccPn.append(pearsonr(generatedSignalp, actualSignalP))
    
    csvData = csvSensorData
    valueX = []
    valueY = []
    valueZ = []
    valueMag = []
    valueP = []
    leftEvents = []
    rightEvents = []
    sensorDataLength = len(csvData['sensor'])
    startIndex = 0
    endIndex = sensorDataLength
    for rowI in range(startIndex, endIndex):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            valueX.append((t, x))
            valueY.append((t, y))
            valueZ.append((t, z))
            valueMag.append((t, mag(x, y, z)))
            valueP.append([x,y,z])
        elif row == 'left':
            leftEvents.append(csvData['time'][rowI])
        elif row == 'right':
            rightEvents.append(csvData['time'][rowI])
    valueX = np.array(valueX)
    valueY = np.array(valueY)
    valueZ = np.array(valueZ)
    valueMag = np.array(valueMag)
    valueP = np.array(valueP)
    valueP = pca.transform(valueP)
    windowTime = signalPeriod * pairNum
    for rowI in range(len(leftEvents) - pairNum):
        startWindow = leftEvents[rowI]
        endWindow = leftEvents[rowI + pairNum]
        windowX = []
        windowY = []
        windowZ = []
        windowMag = []
        windowP = []
        for v in valueX:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowX.append(v)
        for v in valueY:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowY.append(v)
        for v in valueZ:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowZ.append(v)
        for v in valueMag:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowMag.append(v)
        for v in range(len(valueP)):
            valueTime = valueX[v][0]
            if valueTime >= startWindow and valueTime <= endWindow:
                windowP.append(valueP[v])
        windowX = np.array(windowX)
        windowY = np.array(windowY)
        windowZ = np.array(windowZ)
        windowMag = np.array(windowMag)
        windowP = np.array(windowP)
        signal_range = windowX[:,0]
        repeats = int(signal_range.size / (pairNum * 2))
        slength = repeats * pairNum * 2
        signal_range = signal_range[:slength]
        generatedSignal = referenceSignal(signal_range)
        actualSignalX = remove_trend(windowX[:,1][:slength])
        actualSignalY = remove_trend(windowY[:,1][:slength])
        actualSignalZ = remove_trend(windowZ[:,1][:slength])
        actualSignalMag = remove_trend(windowMag[:,1][:slength])
        actualSignalP = remove_trend(windowP[:,0][:slength])
        dfuncs = [duty_function_permutation(d, repeats) for d in dutyPermutes]
        
        if CORRELATION_EWMA:
            actualSignalX = get_ewma(actualSignalX, alpha=0.1)
            actualSignalY = get_ewma(actualSignalY, alpha=0.1)
            actualSignalZ = get_ewma(actualSignalZ, alpha=0.1)
            actualSignalMag = get_ewma(actualSignalMag, alpha=0.1)
            actualSignalP = get_ewma(actualSignalP, alpha=0.1)
        rfactor = signal_range.size * 2
        rs2 = resample(generatedSignal, rfactor, t=signal_range)
        ps2x = resample(actualSignalX, rfactor, t=signal_range)[0]
        ps2y = resample(actualSignalY, rfactor, t=signal_range)[0]
        ps2z = resample(actualSignalZ, rfactor, t=signal_range)[0]
        ps2m = resample(actualSignalMag, rfactor, t=signal_range)[0]
        ps2p = resample(actualSignalP, rfactor, t=signal_range)[0]
        ts2 = rs2[1]
        rs2 = rs2[0]
        resample_time = (signal_range[-1] - signal_range[0]) / rfactor
        df = referenceDuty(signal_range)
        lag_factorx = find_timeshift(rs2, ps2x)
        lag_timex = resample_time * lag_factorx
        generatedSignalx = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timex)), duty=df) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timex))) # arcsin(1)
        lag_factory = find_timeshift(rs2, ps2y)
        lag_timey = resample_time * lag_factory
        generatedSignaly = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timey)), duty=df) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timey))) # arcsin(1)
        lag_factorz = find_timeshift(rs2, ps2z)
        lag_timez = resample_time * lag_factorz
        generatedSignalz = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timez)), duty=df) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timez))) # arcsin(1)
        lag_factorm = find_timeshift(rs2, ps2m)
        lag_timem = resample_time * lag_factorm
        generatedSignalm = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timem)), duty=df) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timem))) # arcsin(1)
        lag_factorp = find_timeshift(rs2, ps2p)
        lag_timep = resample_time * lag_factorp
        generatedSignalp = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timep)), duty=df) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timep))) # arcsin(1)
        ccX.append(pearsonr(generatedSignalx, actualSignalX))
        ccY.append(pearsonr(generatedSignaly, actualSignalY))
        ccZ.append(pearsonr(generatedSignalz, actualSignalZ))
        ccMag.append(pearsonr(generatedSignalm, actualSignalMag))
        ccP.append(pearsonr(generatedSignalp, actualSignalP))
        if COMPLETE_SHUFFLE:
            shuffleX = np.random.permutation(actualSignalX)
            shuffleY = np.random.permutation(actualSignalY)
            shuffleZ = np.random.permutation(actualSignalZ)
            shuffleMag = np.random.permutation(actualSignalMag)
            shuffleP = np.random.permutation(actualSignalP)
            xResults_all.append(pearsonr(generatedSignalx, shuffleX))
            yResults_all.append(pearsonr(generatedSignaly, shuffleY))
            zResults_all.append(pearsonr(generatedSignalz, shuffleZ))
            magResults_all.append(pearsonr(generatedSignalm, shuffleMag))
            pResults_all.append(pearsonr(generatedSignalp, shuffleP))
            if SHOW_CORRELATION_WINDOWS:
                    pplot.clf()
                    pplot.plot(signal_range, shuffleX,color="red")
                    pplot.plot(signal_range, gsx,color="green")
                    pplot.plot(signal_range, generatedSignalx,color="blue")
                    pplot.show()
        else:
            for df in dfuncs:
                gsx = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timex)), duty=df)
                gsy = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timey)), duty=df)
                gsz = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timez)), duty=df)
                gsm = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timem)), duty=df)
                gsp = signal.square((2*np.pi/signalPeriod)*signal_range + (1.57079633 - (2*np.pi/signalPeriod)*(leftEvents[0]+lag_timep)), duty=df)
                xResults_all.append(pearsonr(gsx, actualSignalX))
                yResults_all.append(pearsonr(gsy, actualSignalY))
                zResults_all.append(pearsonr(gsz, actualSignalZ))
                magResults_all.append(pearsonr(gsm, actualSignalMag))
                pResults_all.append(pearsonr(gsp, actualSignalP))
                if SHOW_CORRELATION_WINDOWS:
                    pplot.clf()
                    pplot.plot(signal_range, actualSignalX,color="red")
                    pplot.plot(signal_range, gsx,color="green")
                    pplot.plot(signal_range, generatedSignalx,color="blue")
                    pplot.show()
    
    ccX = np.array(ccX)
    ccY = np.array(ccY)
    ccZ = np.array(ccZ)
    ccMag = np.array(ccMag)
    ccP = np.array(ccP)
    ccXn = np.array(ccXn)
    ccYn = np.array(ccYn)
    ccZn = np.array(ccZn)
    ccMagn = np.array(ccMagn)
    ccPn = np.array(ccPn)
    queryX_all = ccX[:,0]
    queryY_all = ccY[:,0]
    queryZ_all = ccZ[:,0]
    queryMag_all = ccMag[:,0]
    queryP_all = ccP[:,0]
    queryX_all_noise = ccXn[:,0]
    queryY_all_noise = ccYn[:,0]
    queryZ_all_noise = ccZn[:,0]
    queryMag_all_noise = ccMagn[:,0]
    queryP_all_noise = ccPn[:,0]
    pplot.clf()
    f, (ax1, ax2, ax3, ax4, ax5) = pplot.subplots(5)
    f.set_size_inches(FIGURE_SIZE)
    xResults = [item for sublist in xResults_all for item in sublist]
    yResults = [item for sublist in yResults_all for item in sublist]
    zResults = [item for sublist in zResults_all for item in sublist]
    magResults = [item for sublist in magResults_all for item in sublist]
    pResults = [item for sublist in pResults_all for item in sublist]
    plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, queryX_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax1, queryX_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query X Distribution")
    plot_distribution(ax2, yResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax2, queryY_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax2, queryY_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
    plot_distribution(ax3, zResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax3, queryZ_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax3, queryZ_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
    plot_distribution(ax4, magResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax4, queryMag_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax4, queryMag_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
    plot_distribution(ax5, pResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="PCA Transform")
    plot_distribution(ax5, queryP_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="PCA Transform")
    plot_distribution(ax5, queryP_all_noise, 10, None, "red", xLabel="Mean", yLabel="Density", title="PCA Transform")
    pplot.savefig(generate_filename("correlate_distribution")) if SAVE_FILES else pplot.show()

    
    rl = []

    xrkde = univariate_kde(xResults)
    xqkde = univariate_kde(queryX_all)
    xnkde = univariate_kde(queryX_all_noise)
    xqra = distribution_accuracy(xqkde, xrkde)
    xqna = distribution_accuracy(xqkde, xnkde)
    rl.append(("X-axis", xqra[0], xqra[1], xqra[2], xqna[0], xqna[1], xqna[2]))

    yrkde = univariate_kde(yResults)
    yqkde = univariate_kde(queryY_all)
    ynkde = univariate_kde(queryY_all_noise)
    yqra = distribution_accuracy(yqkde, yrkde)
    yqna = distribution_accuracy(yqkde, ynkde)
    rl.append(("Y-axis", yqra[0], yqra[1], yqra[2], yqna[0], yqna[1], yqna[2]))

    zrkde = univariate_kde(zResults)
    zqkde = univariate_kde(queryZ_all)
    znkde = univariate_kde(queryZ_all_noise)
    zqra = distribution_accuracy(zqkde, zrkde)
    zqna = distribution_accuracy(zqkde, znkde)
    rl.append(("Z-axis", zqra[0], zqra[1], zqra[2], zqna[0], zqna[1], zqna[2]))

    mrkde = univariate_kde(magResults)
    mqkde = univariate_kde(queryMag_all)
    mnkde = univariate_kde(queryMag_all_noise)
    mqra = distribution_accuracy(mqkde, mrkde)
    mqna = distribution_accuracy(mqkde, mnkde)
    rl.append(("Combined Magnitude", mqra[0], mqra[1], mqra[2], mqna[0], mqna[1], mqna[2]))

    prkde = univariate_kde(pResults)
    pqkde = univariate_kde(queryP_all)
    pnkde = univariate_kde(queryP_all_noise)
    pqra = distribution_accuracy(pqkde, prkde)
    pqna = distribution_accuracy(pqkde, pnkde)
    rl.append(("PCA Transform", pqra[0], pqra[1], pqra[2], pqna[0], pqna[1], pqna[2]))

    fs = "{0:.5f}"
    print("error vs permutation test and noise")
    for r in rl:
        print(r[0], "pe", fs.format(r[1]), "pfp", fs.format(r[2]), "pfn", fs.format(r[3]), "ne", fs.format(r[4]), "nfp", fs.format(r[5]), "nfn", fs.format(r[6]))
        

def data_3d_plot():
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    plotVector = []
    vectors = []
    timevectors = []
    pplot.clf()
    for rowI in range(len(csvData['sensor'])):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            plotX.append((t, x))
            plotY.append((t, y))
            plotZ.append((t, z))
            plotMag.append((t, mag(x, y, z)))
            plotVector.append([0, 0, 0, x, y, z])
            vectors.append([x, y, z])
            timevectors.append([t, x, y, z])
        elif row == 'left':
            #pplot.axvline(x=csvData['time'][rowI], color='red')
            pass
        elif row == 'right':
            #pplot.axvline(x=csvData['time'][rowI], color='black')
            pass
        elif row == 'vibrate':
            #pplot.axvline(x=csvData['time'][rowI], color='green')
            pass
    import matplotlib.pyplot as plt
    from mpl_toolkits.mplot3d import Axes3D
    import matplotlib.animation as animation
    import numpy as np

    vectors = np.array(vectors)
    timevectors = np.array(timevectors)
    # from sklearn.decomposition import PCA
    # pca = PCA(n_components=1)
    # pca.fit(vectors)
    # pa = pca.components_[0]
    # pvectors = [[0,0,0,pa[0],pa[1],pa[2]]]
    # pvectors = np.array(pvectors)
    soa =np.array(plotVector) 

    X,Y,Z,U,V,W = zip(*soa)
    # fig = plt.figure()
    # ax = fig.add_subplot(111, projection='3d')
    # ax.quiver(X,Y,Z,U,V,W, pivot="tail", arrow_length_ratio=0.05)
    # X2,Y2,Z2,U2,V2,W2 = zip(*pvectors)
    # ax.quiver(X2,Y2,Z2,U2,V2,W2, pivot="tail", arrow_length_ratio=0.05,color="red")
    # ax.set_xlim([-1,1])
    # ax.set_ylim([-1,1])
    # ax.set_zlim([-1,1])
    
    print (vectors.shape[0])

    plt.ion()
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim(-50, 50)
    ax.set_ylim(-50, 50)
    ax.set_zlim(-50, 50)
    prev_time = timevectors[0][0] - .001
    total_time = timevectors[timevectors.shape[0]-1][0] - timevectors[0][0]
    print (total_time)
    print ('dividing total_time by a factor of 100000')
    # for i in range(timevectors.shape[0]):
    i = 0
    import time as system_time
    while i < timevectors.shape[0]:
        # print (timevectors[i])
        time = timevectors[i][0]
        x = timevectors[i][1]
        y = timevectors[i][2]
        z = timevectors[i][3]
        ax.scatter(x, y, z)
        pause_time = time - prev_time
        pause_time /= 100000
        if pause_time == 0.0:
            pause_time = .00001
        print (pause_time)
        # system_time.sleep(pause_time)
        plt.pause(pause_time)
        prev_time = timevectors[i][0]
        i += 5
    while True:
        plt.pause(0.05)

initialize_environment()
# data_plot_distribution()
# data_plot_raw_data()
# data_lag_calculation()
# data_correlation_graphs()
#data_online_performance()
# data_distribution_comparison()
# data_correlation_distribution()
data_3d_plot()