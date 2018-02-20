import random
import os.path
import os
import pandas as pd
from lib_util import *
import matplotlib.pyplot as plt

def segmentNoiseFile(filePath):
    dataPath = filePath + "/data/"
    inputPath = dataPath
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("raw.csv"):
                dataFile = inputPath + f[:-4]
#                 print(dataFile)
                try:
                    rawDataFrame = pd.read_csv(dataFile + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                rawDataBegin = rawDataFrame[rawDataFrame.sensor.str.contains("begin")]
                if len(rawDataBegin) < 1:
                    continue
                st = rawDataBegin.system_time.values[0]
                rawDataNoise = rawDataFrame[rawDataFrame.system_time < st]
                tdiff = rawDataNoise.system_time.values[-1] - rawDataNoise.system_time.values[0]
                ticknum = 6
                if tdiff // 1000 < 6:
                    continue
                startend = rawDataNoise.system_time.values[-1] - ticknum * 1000
                startendindex = len(rawDataNoise[rawDataNoise.system_time < startend])
                for ri in range(10):
                    dfCopy = rawDataNoise.copy()
                    startindex = random.randint(0, startendindex)
                    rst = rawDataNoise.system_time.values[startindex]
                    rsta = rawDataNoise.adjusted_time.values[startindex]
                    tickleft = True
                    tickrows = []
                    for ti in range(ticknum * 2):
                        tdiff = ti * 500
                        ticktime = rst + tdiff
                        ticktimea = rsta + tdiff
                        tickline = {'system_time' : ticktime, 'sensor' : "left" if tickleft else "right", 'x' : ticktimea}
                        tickleft = not tickleft
                        tickrows.append(tickline)
                    for rowI in range(len(dfCopy)):
                        rowtime = dfCopy.system_time.values[rowI]
                        if len(tickrows) < 1:
                            continue
                        if tickrows[0]['system_time'] < rowtime:
                            trdf = pd.DataFrame(tickrows[0], index=[rowI + 1])
                            dfCopy = pd.concat([dfCopy.ix[:rowI], trdf, dfCopy.ix[rowI + 1:]]).reset_index(drop=True)
                            del tickrows[0]
                    dfCopy = dfCopy[(dfCopy.system_time >= rst) & (dfCopy.system_time <= rst + ticknum * 1000)]
                    column_order = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                    dfCopy[column_order].to_csv(dataFile.replace("raw", "noise") + str(ri) + ".csv", header=None, index=False)

# import seaborn as sns
from scipy import stats

def generateAccelDist(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    axs = []
    ays = []
    azs = []
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("_raw.csv"):
                dataFile = inputPath + f[:-4]
#                 print(dataFile)
                try:
                    rawDataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    dataFrame = pd.read_csv(dataFile.replace("raw", "corr") + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                if len(dataFrame) < 1:
                    continue
                if dataFrame.shape[1] == 3:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif dataFrame.shape[1] == 4:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                if noise:
                    rawDataSync = rawDataFrame
                else:
                    rawDataBegin = rawDataFrame[rawDataFrame.sensor.str.contains("begin")]
                    if len(rawDataBegin) < 1:
                        print("no begin in " + f)
                        continue
                    st = rawDataBegin.system_time.values[0]
                    sync_threshold = 0.9
                    dfsync = dataFrame[(dataFrame.correlation >= sync_threshold) & (dataFrame.timestamp - st > IGNORE_TIME)]
                    et = dfsync.timestamp.values[-1] if len(dfsync) > 0 else dataFrame.timestamp.values[-1]
                    rawDataSync = rawDataFrame[(rawDataFrame.system_time >= (st + IGNORE_TIME)) & (rawDataFrame.adjusted_time <= et)]
                accel_mask = (rawDataSync.sensor == "accel")
                rdsa = rawDataSync[accel_mask]
                axs.extend(list(rdsa.x.values))
                ays.extend(list(rdsa.y.values))
                azs.extend(list(rdsa.z.values))
#     plt.clf()
#     sns.distplot(axs, color="r")
#     sns.distplot(ays, color="g")
#     sns.distplot(azs, color="b")
#     xg = stats.gaussian_kde(axs)
#     yg = stats.gaussian_kde(ays)
#     zg = stats.gaussian_kde(azs)
#     xs  = np.linspace(-20, 20, 201)
#     plt.plot(xs, xg(xs), color="red")
#     plt.plot(xs, yg(xs), color="green")
#     plt.plot(xs, zg(xs), color="blue")
#     plt.show()
#     return (xg, yg, zg)
    return [axs, ays, azs]

# import seaborn as sns
from scipy import stats
import sys

def generateMagDist(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    magnitudes = []
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("mag.csv"):
                dataFile = inputPath + f[:-4]
#                 print(dataFile)
                try:
                    magDataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    dataFrame = pd.read_csv(dataFile.replace("mag", "corr") + ".csv", header=None)
                    rawDataFrame = pd.read_csv(dataFile.replace("mag", "raw") + ".csv", header=None)
                except:
                    print("error in " + f)
#                     print(sys.exc_info()[0])
                    continue
                magDataFrame.columns = ['timestamp', 'magnitude', 'linecounter']
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                if len(dataFrame) < 1:
                    continue
                if dataFrame.shape[1] == 3:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif dataFrame.shape[1] == 4:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                    
                
                if noise:
                    magDataSync = magDataFrame
                else:
                    rawDataBegin = rawDataFrame[rawDataFrame.sensor.str.contains("begin")]
                    if len(rawDataBegin) < 1:
                        continue
                    st = rawDataBegin.adjusted_time.values[0]
                    sync_threshold = 0.9
                    dfsync = dataFrame[(dataFrame.correlation >= sync_threshold) & (dataFrame.timestamp - st > IGNORE_TIME)]
                    et = dfsync.timestamp.values[-1] if len(dfsync) > 0 else dataFrame.timestamp.values[-1]
                    magDataSync = magDataFrame[(magDataFrame.timestamp >= (st + IGNORE_TIME))]
                
                magnitudes.extend(magDataSync.magnitude.values)
    return magnitudes


def generateDeltaDist(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    deltas = []
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("delta.csv"):
                dataFile = inputPath + f[:-4]
#                 print(dataFile)
                try:
                    deltaDataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    dataFrame = pd.read_csv(dataFile.replace("delta", "corr") + ".csv", header=None)
                    rawDataFrame = pd.read_csv(dataFile.replace("delta", "raw") + ".csv", header=None)
                except:
                    print("error in " + f)
#                     print(sys.exc_info()[0])
                    continue
                deltaDataFrame.columns = ['timestamp', 'delta']
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                if len(dataFrame) < 1:
                    continue
                if dataFrame.shape[1] == 3:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif dataFrame.shape[1] == 4:
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                    
                
                if noise:
                    deltaDataSync = deltaDataFrame
                else:
                    rawDataBegin = rawDataFrame[rawDataFrame.sensor.str.contains("begin")]
                    if len(rawDataBegin) < 1:
                        continue
                    st = rawDataBegin.adjusted_time.values[0]
                    sync_threshold = 0.9
                    dfsync = dataFrame[(dataFrame.correlation >= sync_threshold) & (dataFrame.timestamp - st > IGNORE_TIME)]
                    et = dfsync.timestamp.values[-1] if len(dfsync) > 0 else dataFrame.timestamp.values[-1]
                    deltaDataSync = deltaDataFrame[(deltaDataFrame.timestamp >= (st + IGNORE_TIME))]
                
                deltas.extend(deltaDataSync.delta.values)
    return deltas

def processSwipeFile(folderPath):    
    dataPath = folderPath + "data/"
    if not os.path.exists(dataPath):
        os.makedirs(dataPath)

    totalTimes = []
    # Walk the datapath
    for root, folders, files in os.walk(dataPath):
        for f in files:
            if "Store" not in f:

                # parse filename        
                filePath = dataPath + f

                if "swipe" in f:

                    # print f

                    # Load data
                    swipeData = pd.read_csv(filePath, sep=',', header=None, low_memory=False)
                    swipeData.columns = ['tsystem', 'action', 'x', 'y']

                    # Total Length
                    totalTime = calculateTotalTime(swipeData)
                    totalTimes.append(totalTime)
                    # print totalTime

                    #todo: figure out time from buzz to swipedetected
                    #todo: figure out time from to lift to touchdown
                    #todo: figure out time from to touchdown to touchup
                    #todo: figure out time from touchup to swipedetected

                    # Normalize time data
    #                 swipeData = normalizeSystemTime(swipeData)

                    # Plot
                    startTime = swipeData.tsystem[swipeData[swipeData.action == "vibrate"].index.tolist()[0]]
                    downTime = swipeData.tsystem[swipeData[swipeData.action == "touch_down"].index.tolist()[0]]
                    endTime = swipeData.tsystem[swipeData[(swipeData.action == "swipe left") | (swipeData.action == "end") | (swipeData.action== "swipe right")].index.tolist()[0]]    
                    fig = plt.figure()
    #                 fig.set_size_inches(40,40)
    #                 plt.xlim(-100,4000)

                elif "wrist" in f:


                    # Load data
                    wristData = pd.read_csv(filePath, sep=',', header=None, low_memory=False)
                    wristData.columns = ['sensor', 'index', 'tevent', 'tsystem', 'x', 'y', 'z']

                    # Total Length
    #                 print calculateTotalTime(wristData)

                    # Normalize time data
    #                 wristData = normalizeSystemTime(wristData
                    startTime = swipeData.tsystem[swipeData[swipeData.action == "vibrate"].index.tolist()[0]]
                    downTime = swipeData.tsystem[swipeData[swipeData.action == "touch_down"].index.tolist()[0]]
                    endTime = swipeData.tsystem[swipeData[(swipeData.action == "swipe left") | (swipeData.action == "end") | (swipeData.action== "swipe right")].index.tolist()[0]]    

        # print "mean: " + str(np.mean(totalTimes)/1000) + "s" + ", stdev: " + str(np.std(totalTimes)/1000) + "s"
        return np.mean(totalTimes) / 1000

# Generate synthetic data using actual timestamps

def generateSignal(time, period, offset):
    return np.sin(np.array(time) * (2 * 3.14 / period) + (1.57079 * (2 * 3.14 / period) * offset))
def writeGeneratedSignal(dataPath):
        inputPath = dataPath
        rawDataSet = {}
        for root, folders, files in os.walk(inputPath):
            for f in files:    
                if f.endswith("raw.csv"):
                    print(f)
                    dataFile = inputPath + f            
                    x = []
                    y = []
                    z = []
                    tsystem = []
                    timestamp = []
                    linecounter = []
                    left = []
                    right = []
                    lefts = []
                    rights = []
                    with open(dataFile, 'rb') as inputFile:
                        reader = csv.reader(inputFile)
                        inputData = list(reader)
                    for line in inputData:
                        if line[1] == "magnet":
                            tsystem.append(int(line[0]))
                            x.append(float(line[2]))
                            y.append(float(line[3]))
                            z.append(float(line[4]))
                            timestamp.append(float(line[5]))
                            linecounter.append(int(line[7]))
                            left.append(0)
                            right.append(0)
                        elif line[1] == "left":
                            del left[-1]
                            left.append(float(lastline[5]))
                            lefts.append(float(lastline[5]))
                            if len(lefts) > 1:
                                print(lefts[-1] - lefts[-2])
                        elif line[1] == "right":
                            del right[-1]
                            right.append(float(lastline[5]))
                            rights.append(float(lastline[5]))
                        lastline = line
                    data = {'tsystem' : tsystem, 'x' : x, 'y' : y, 'z' : z, 'timestamp' : timestamp, 'linecounter' : linecounter, 'left' : left, 'right' : right}
                    dataFrame = pd.DataFrame.from_dict(data)

                    # add dataframe to dictionary
                    rawDataSet[f] = dataFrame
                    
                    # save to csv
                    cols = ['tsystem', 'sensor', 'x', 'y', 'z', 'convertedts', 'timestamp', 'linecounter']
                    synthdata = pd.read_csv(dataFile, names=cols, header=None)
                    sdf = synthdata
                    sdfMagnetMask = sdf['sensor'] == 'magnet'
                    sdf.ix[sdfMagnetMask, 'x'] = 0
                    sdf.ix[sdfMagnetMask, 'y'] = 0
                    sdf.ix[sdfMagnetMask, 'z'] = generateSignal(sdf.ix[sdfMagnetMask, 'tsystem'], 1000, lefts[-1])
                    sdf.ix[sdfMagnetMask, 'linecounter'] = [int(l) for l in sdf.ix[sdfMagnetMask, 'linecounter']]
                    sdf[cols].to_csv(synthDataPath + f, header=False, index=False,)


# Generate noise data using actual timestamps

def generateNoiseSignal(time, period, offset):
#     return np.sin(np.array(time) * (2 * 3.14 / period) + (1.57079 * (2 * 3.14 / period) * offset))
    return np.random.rand(*time.shape)
def writeGeneratedNoiseSignal(dataPath):
    inputPath = dataPath
    rawDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("raw.csv"):
                print(f)
                dataFile = inputPath + f            
                x = []
                y = []
                z = []
                tsystem = []
                timestamp = []
                linecounter = []
                left = []
                right = []
                lefts = []
                rights = []
                with open(dataFile, 'rb') as inputFile:
                    reader = csv.reader(inputFile)
                    inputData = list(reader)
                for line in inputData:
                    if line[1] == "magnet":
                        tsystem.append(int(line[0]))
                        x.append(float(line[2]))
                        y.append(float(line[3]))
                        z.append(float(line[4]))
                        timestamp.append(float(line[5]))
                        linecounter.append(int(line[7]))
                        left.append(0)
                        right.append(0)
                    elif line[1] == "left":
                        del left[-1]
                        left.append(float(lastline[5]))
                        lefts.append(float(lastline[5]))
                        if len(lefts) > 1:
                            print(lefts[-1] - lefts[-2])
                    elif line[1] == "right":
                        del right[-1]
                        right.append(float(lastline[5]))
                        rights.append(float(lastline[5]))
                    lastline = line
                data = {'tsystem' : tsystem, 'x' : x, 'y' : y, 'z' : z, 'timestamp' : timestamp, 'linecounter' : linecounter, 'left' : left, 'right' : right}
                dataFrame = pd.DataFrame.from_dict(data)

                # add dataframe to dictionary
                rawDataSet[f] = dataFrame
                
                # save to csv
                cols = ['tsystem', 'sensor', 'x', 'y', 'z', 'convertedts', 'sensorts', 'linecounter']
                synthdata = pd.read_csv(dataFile, names=cols, header=None)
                sdf = synthdata
                sdfMagnetMask = sdf['sensor'] == 'magnet'
                sdf.ix[sdfMagnetMask, 'x'] = generateSignal(sdf.ix[sdfMagnetMask, 'tsystem'], 1000, lefts[-1])
                sdf.ix[sdfMagnetMask, 'y'] = 0
                sdf.ix[sdfMagnetMask, 'z'] = 0
                sdf.ix[sdfMagnetMask, 'linecounter'] = [int(l) for l in sdf.ix[sdfMagnetMask, 'linecounter']]
                sdf[cols].to_csv(synthDataPath + f, header=False, index=False,)
