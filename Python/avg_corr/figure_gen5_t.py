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

def delta_space_transform(leftVector, rightVector, currentVector, lastLeft):
    deltaVector = leftVector - rightVector
    currentDeltaVector = currentVector - leftVector if lastLeft else currentVector - rightVector
    scalarProjection = currentDeltaVector.dot(deltaVector) / magv(deltaVector)
    return scalarProjection
    

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

alphaFactors = {"magnet":0.15, "accel":0.02, "gyro":0.003}

START_PADDING = 0.1
END_PADDING = 0.1

SAMPLING_PERIOD = 10
SAMPLING_WINDOW = 400

SHOW_CORRELATION_WINDOWS = False
CORRELATION_EWMA = True

SAVE_FILES = True

FIT_SQUARE = False if (len(sys.argv) == 3 or sys.argv[3] == "False") else True 
GROUP_FREQUENCY = "1000" 
SKIP_CYCLES = float(sys.argv[2])

TARGET_DIR = sys.argv[1] + "/"

PLOT_SENSOR = "magnet"
def generate_filename(basename):
    return FOLDER_PREFIX + CSV_PREFIX + "_" + (("" if USE_DELTAS else "d") + ("S" if USE_STD_ALGORITHM else "s")) + "_" + PLOT_SENSOR + "_" + basename + FILE_FORMAT

def initialize_environment():
    global csvSensorDatas
    global GROUP_FREQUENCY
    cmd = r'find "' + TARGET_DIR + r'" -name "*"' + GROUP_FREQUENCY + r'"*ssensorData.csv" | grep -v "noise"'
    import subprocess
    csvSensorDataFiles = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE).stdout.read().decode('utf-8').split('\n')[:-1]
    csvSensorDatas = []
    for f in csvSensorDataFiles:
        csvSensorData = pd.read_csv(f, names=['time','sensor','x','y','z','timestamp','linecounter',''])
        csvSensorDatas.append(csvSensorData)


def data_correlation_graphs2(csvData):
    valueX = []
    valueY = []
    valueZ = []
    valueMag = []
    valueT = []
    valueTL = []
    leftEvents = []
    rightEvents = []
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
            valueX.append((t, x))
            valueY.append((t, y))
            valueZ.append((t, z))
            valueMag.append((t, mag(x, y, z)))
            if lx != 0 and ly != 0 and lz != 0 and rx != 0 and ry != 0 and rz != 0:
                lV = np.array([lx, ly, lz])
                rV = np.array([rx, ry, rz])
                cV = np.array([x, y, z])
                tS = delta_space_transform(lV, rV, cV, False)
                tSL = delta_space_transform(lV, rV, cV, lastLeft)
                valueT.append((t, tS))
                valueTL.append((t, tSL))
        elif row == 'left':
            lx = x
            ly = y
            lz = z
            lastLeft = True
            leftEvents.append(t)
        elif row == 'right':
            rx = x
            ry = y
            rz = z
            lastLeft = False
            rightEvents.append(t)
    valueX = np.array(valueX)
    valueY = np.array(valueY)
    valueZ = np.array(valueZ)
    valueMag = np.array(valueMag)
    valueT = np.array(valueT)
    valueTL = np.array(valueTL)
    signalPeriod = rightEvents[1] - rightEvents[0]
    print("FIT_SQUARE", FIT_SQUARE)
    referenceSignal = lambda x : signal.square((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) if FIT_SQUARE else np.sin((2*np.pi/signalPeriod)*x + (1.57079633 - (2*np.pi/signalPeriod)*leftEvents[0])) # arcsin(1)
    windowTime = signalPeriod * pairNum
    valueS = valueT
    valueS = get_ewma(valueS, 0.15)
    correlationCoefficients = []
    discardNum = SKIP_CYCLES 
    totalTime = int(leftEvents[-1] - (leftEvents[0] + discardNum*signalPeriod))
    waveLengths = [] 
    for rowI in range(0, totalTime, totalTime // 150):
        startWindow = leftEvents[0] + discardNum*signalPeriod
        endWindow = leftEvents[0] + rowI
        window = []
        for v in valueS:
            valueTime = v[0]
            if valueTime >= startWindow and valueTime <= endWindow:
                window.append(v)
        if len(window) < 2:
            continue
        waveLengths.append(rowI)
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
        correlationCoefficients.append((rowI, cc[0]))
    correlationCoefficients = np.array(correlationCoefficients)
    return correlationCoefficients
def plot_category(ax, category, category_color):
    global GROUP_FREQUENCY
    GROUP_FREQUENCY = category
    initialize_environment()
    mcs = []
    for sdi in range(len(csvSensorDatas)):
        sd = csvSensorDatas[sdi]
        mc = data_correlation_graphs2(sd)
        mcs.append(mc)

    minLength = min([i.shape[0] for i in mcs])
    mcs = [cc[:minLength] for cc in mcs]
    avgCCs = np.array(mcs).mean(axis=0)
    ax1.plot(avgCCs[:,0], avgCCs[:,1], color=category_color)


f, (ax1) = pplot.subplots(1)

ax1.set_title("Average Correlation Over Time")
ax1.set_xlabel("Time")
ax1.set_ylabel("Correlation Coefficient")
frequencies = [("750", "red"), ("1000", "green"), ("1250", "blue")]
for freq in frequencies:
    plot_category(ax1, freq[0], freq[1])

folderName = sys.argv[1].split("/")[-1]
print("folder name:", folderName)
pplot.savefig(TARGET_DIR + folderName + "_correlation_time_avg_t" + str(SKIP_CYCLES).replace(".", "_") + "_" + ("square" if FIT_SQUARE else "sine") + FILE_FORMAT)