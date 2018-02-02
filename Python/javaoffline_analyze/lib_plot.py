import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import os
import scipy
import csv

# folderPath = "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/User Study/Flat/sync1000/"
# folderPath = "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/Testing/2017_03_17/syncdata/"
# folderPath = "/Users/gareyes/Downloads/Pilot/P1/synchro/t1490393232950_p1_sit/"
# folderPath = "/Users/jwpilly/Downloads/2017_03_06/syntheticdata/"
# folderPath = "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/Testing/2017_03_04/syncdata1/"
# folderPath = "/Users/jwpilly/Documents/Research/Synchro/Study_v2/P1/synchro/t20170401112834_p1_sit/"
# folderPath = "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/Testing/2017_03_19/syncdata1/"
# folderPath = "/Users/jwpilly/Research/Synchro/figuredata/figure5/syncdata/"
# folderPath = "/media/jwpilly/PillowDisk/Research/Synchro/figuredata/figure5/syncdata/"
# folderPath = "/media/jwpilly/PillowDisk/Research/Synchro/synchrowatch/2017-10-24/Synchro/P12/synchro/t20171024215804_p12_prep/"
# folderPath = "/media/jwpilly/PillowDisk/Research/Synchro/synchrowatch/2017-10-24/Synchro/P3/synchro/t20171025002658_p3_prep/"

folderPath = "/media/jwpilly/PillowDisk/Research/Synchro/synchrowatch/2018-01-10/Synchro/P2/synchro/all/"
# folderPath = "/media/jwpilly/PillowDisk/Research/Synchro/synchrowatch/2018-01-09/Synchro/P21/synchro/t20180109202830_p21_noise/"

# folderPath = "/Users/jwpilly/Downloads/2017_03_21/syncdata/"
# folderPath = "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/Testing/2017_05_01/syncdata1/"

useSynthData = False
synthDataPath = folderPath + "synthdata/"

if useSynthData:
    dataPath = synthDataPath
else:
    dataPath = folderPath + "data/"

outputPath = folderPath + "output/"
figurePath = folderPath + "figures/"
windowPath = folderPath + "windows/"
createPaths = [figurePath]
for path in createPaths:
    if not os.path.exists(path):
        os.makedirs(path)

def plot_corr(df, fileName=None, saveToFile=True):
    fig, axs = plt.subplots(1, 1, sharex=True)
    fig.set_size_inches(15, 15) 
    df.timestamp = df.timestamp - df.timestamp[0]
    axs.plot(df.timestamp, df.correlation)
    axs.set_ylim(-1,1)
    if(saveToFile):
        plt.savefig(fileName)
    else:
        plt.show()
    plt.close()
    
def plot_feature(df, fileName=None, saveToFile=False):
    fig, axs = plt.subplots(1, 1, sharex=True)
    fig.set_size_inches(15, 15) 
    axs.plot(df.timestamp, df.feature)
    axs.set_ylim(-60,60)    
    if(saveToFile):
        plt.savefig(fileName)
    else:
        plt.show() 
    plt.close()
    
def plot_corrs(df1, df2, fileName=None, saveToFile=False):
    fig, axs = plt.subplots(1, 1, sharex=True)
    fig.set_size_inches(15, 15)    
    axs.plot(df1.timestamp, df1.correlation)
    axs.plot(df2.timestamp, df2.correlation)
    axs.set_ylim(-1,1)
    if(saveToFile):
        plt.savefig(fileName)
    else:
        plt.show()
    plt.close()   
    
def plot_3axis(df, fileName=None, saveToFile=False):
    fig, axs = plt.subplots(3, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    axs[0].plot(df.timestamp, df.x, '-b')
    axs[1].plot(df.timestamp, df.y, '-b')
    axs[2].plot(df.timestamp, df.z, '-b')    
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()

def plot_3axis_withticks(df, fileName=None, saveToFile=False):
    fig, axs = plt.subplots(3, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    axs[0].plot(df.timestamp, df.x, '-b')
    axs[1].plot(df.timestamp, df.y, '-b')
    axs[2].plot(df.timestamp, df.z, '-b')    
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    left = [x for x in df.left if x != 0]
    right = [x for x in df.right if x != 0]
    for i in left:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')
    for i in right:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r') 
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()

def plot_3axis_withmag(inputDf, fileName=None, saveToFile=False):
    df = inputDf.copy()
    df['magnitude'] = np.sqrt(np.square(df.x)+np.square(df.y)+np.square(df.z))
    fig, axs = plt.subplots(4, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    axs[0].plot(df.timestamp, df.x, '-b')
    axs[1].plot(df.timestamp, df.y, '-b')
    axs[2].plot(df.timestamp, df.z, '-b')
    axs[3].plot(df.timestamp, df.magnitude, '-b')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('magnitude')
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()
    
def plot_3axis_withmagticks(inputDf, fileName=None, saveToFile=False):
    df = inputDf.copy()
    df['magnitude'] = np.sqrt(np.square(df.x)+np.square(df.y)+np.square(df.z))
    fig, axs = plt.subplots(4, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)    
    axs[0].plot(df.timestamp, df.x, '-b')
    axs[1].plot(df.timestamp, df.y, '-b')
    axs[2].plot(df.timestamp, df.z, '-b')
    axs[3].plot(df.timestamp, df.magnitude, '-b')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('magnitude')
    left = [x for x in df.left if x != 0]
    right = [x for x in df.right if x != 0]
    for i in left:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')
        axs[3].axvline(i, color='b')        
    for i in right:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r') 
        axs[3].axvline(i, color='r') 
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close() 

def plot_3axis_subwithmagticks(rawDf, subDf, fileName=None, saveToFile=False):

    left = []
    right = []
    df1 = []
    df2 = []
    df1=rawDf.copy()
    df2=subDf.copy()
    
    left = [x for x in df1.left if x != 0]
    right = [x for x in df1.right if x != 0]    
    df2['magnitude'] = np.sqrt(np.square(df2.x)+np.square(df2.y)+np.square(df2.z))

    fig, axs = plt.subplots(4, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)    
    axs[0].plot(df2.timestamp, df2.x, '-b')
    axs[1].plot(df2.timestamp, df2.y, '-b')
    axs[2].plot(df2.timestamp, df2.z, '-b')
    axs[3].plot(df2.timestamp, df2.magnitude, '-b')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('magnitude')
    for i in left:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')
        axs[3].axvline(i, color='b')        
    for i in right:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r') 
        axs[3].axvline(i, color='r') 
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close() 
    
#df1 = rawdata, df2 = featuremap
def plot_3axis_withfeature(df1, df2, fileName=None, saveToFile=False):
    fig, axs = plt.subplots(4, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    axs[0].plot(df1.timestamp, df1.x, '-b')
    axs[1].plot(df1.timestamp, df1.y, '-b')
    axs[2].plot(df1.timestamp, df1.z, '-b')
    axs[3].plot(df2.timestamp, df2.feature, '-b')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('feature')
    rawLeft = [x for x in df1.left if x != 0]
    rawRight = [x for x in df1.right if x != 0]    
    for i in rawLeft:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')       
        axs[3].axvline(i, color='b')     
    for i in rawRight:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r')     
        axs[3].axvline(i, color='r')                     
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()

#df1 = rawdata, df2 = featuremap
def plot_3axis_withfeatureandmag(inputDf1, inputDf2, fileName=None, saveToFile=False):
    df1 = inputDf1.copy()
    df2 = inputDf2.copy()
    df1['magnitude'] = np.sqrt(np.square(df1.x)+np.square(df1.y)+np.square(df1.z))
    fig, axs = plt.subplots(5, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    axs[0].plot(df1.timestamp, df1.x, '-b')
    axs[1].plot(df1.timestamp, df1.y, '-b')
    axs[2].plot(df1.timestamp, df1.z, '-b')
    axs[3].plot(df1.timestamp, df1.magnitude, '-b')    
    axs[4].plot(df2.timestamp, df2.feature, '-b')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('magnitude')
    axs[4].set_title('feature')
    rawLeft = [x for x in df1.left if x != 0]
    rawRight = [x for x in df1.right if x != 0]    
    for i in rawLeft:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')       
        axs[3].axvline(i, color='b')     
        axs[4].axvline(i, color='b')             
    for i in rawRight:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r')     
        axs[3].axvline(i, color='r')     
        axs[4].axvline(i, color='r')             
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()    
    
def plot_window(df1, fileName=None, saveToFile=False):
    df = df1.copy()
    fig, axs = plt.subplots(2, 1, sharex=True, sharey=False)
    fig.set_size_inches(15,15)
    df.timestamp = df.timestamp - df.timestamp[0]
    axs[0].plot(df.timestamp, df.feature, '-r', label="feature")
    axs[0].plot(df.timestamp, df.reference, '-b', label="reference")
    axs[1].plot(df.timestamp, df.feature, '-g', label="feature")
    axs[1].plot(df.timestamp, df.adjusted, '-b', label="adjusted")    
    axs[0].set_title("feat-ref: " + str(scipy.stats.pearsonr(df.feature, df.reference)[0]))
    axs[1].set_title("feat-adjref: " + str(scipy.stats.pearsonr(df.feature, df.adjusted)[0]))
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close() 
    
#df1 = rawdata, df2 = subsampledata, df3 = featuremap, df4 = offlinecorr, df5 = lag
def plot_overview(df1, df2, df3, df4, df5, fileName=None, saveToFile=False, plotSub=True, compact=False, excerpt=False, smooth=True):
    def get_ewma(df, alpha=0.05):
        # Compute alpha and exponential moving average
        span = (2-alpha)/alpha
        df_smooth = pd.ewma(df, span=span, adjust=False)
        return df_smooth
    if excerpt:
        tstart = df1.timestamp.min()
        tend = df1.timestamp.max()
        tmid = (tstart + tend) / 2
        trange = (tend - tstart) * 0.05
        ts = tmid + 12 + trange * 1.25
        te = tmid + 12 + trange * 2.75
        df1 = df1[df1.timestamp >= ts]
        df1 = df1[df1.timestamp <= te]
        df2 = df2[df2.timestamp >= ts]
        df2 = df2[df2.timestamp <= te]
        df3 = df3[df3.timestamp >= ts]
        df3 = df3[df3.timestamp <= te]
        df4 = df4[df4.timestamp >= ts]
        df4 = df4[df4.timestamp <= te]
        df5 = df5[df5.timestamp >= ts]
        df5 = df5[df5.timestamp <= te]
    if not compact:
        fig, axs = plt.subplots(7, 1, sharex=True, sharey=False)
    else:
        fig, axs = plt.subplots(5, 1, sharex=True, sharey=False)
    if smooth:
        df1.x = get_ewma(df1.x)
        df1.y = get_ewma(df1.y)
        df1.z = get_ewma(df1.z)
    fig.set_size_inches(15,15)
    df1Copy = []
    df2Copy = []
    df1Copy = df1.copy()
    df2Copy = df2.copy()
    if plotSub:
        df2Copy['magnitude'] = np.sqrt(np.square(df2Copy.x)+np.square(df2Copy.y)+np.square(df2Copy.z))
        if smooth:
            df2Copy.magnitude = get_ewma(df2Copy.magnitude)
        axs[0].plot(df2Copy.timestamp, df2Copy.x, 'black')
        axs[1].plot(df2Copy.timestamp, df2Copy.y, 'black')
        axs[2].plot(df2Copy.timestamp, df2Copy.z, 'black')
        axs[3].plot(df2Copy.timestamp, df2Copy.magnitude, 'black')    
    else:
        df1Copy['magnitude'] = np.sqrt(np.square(df1Copy.x)+np.square(df1Copy.y)+np.square(df1Copy.z))
        if smooth:
            df1Copy.magnitude = get_ewma(df1Copy.magnitude)
        axs[0].plot(df1Copy.timestamp, df1Copy.x, 'black')
        axs[1].plot(df1Copy.timestamp, df1Copy.y, 'black')
        axs[2].plot(df1Copy.timestamp, df1Copy.z, 'black')
        axs[3].plot(df1Copy.timestamp, df1Copy.magnitude, 'black')    
    axs[4].plot(df3.timestamp, df3.feature, 'black')
    if not compact:
        axs[5].plot(df4.timestamp, df4.correlation, 'black')
        axs[5].set_ylim(0,1)
        axs[6].plot(df5.timestamp, df5.lagtime, 'black')
    axs[0].set_title('x')
    axs[1].set_title('y')
    axs[2].set_title('z')
    axs[3].set_title('magnitude')
    axs[4].set_title('feature')
    if not compact:
        axs[5].set_title('output')
        axs[6].set_title('xcorr')
    rawLeft = [x for x in df1Copy.left if x != 0]
    rawRight = [x for x in df1Copy.right if x != 0]    
    for i in rawLeft:
        axs[0].axvline(i, color='b')
        axs[1].axvline(i, color='b')
        axs[2].axvline(i, color='b')      
        axs[3].axvline(i, color='b')  
        axs[4].axvline(i, color='b')
        if not compact:
            axs[6].axvline(i, color='b')
    for i in rawRight:
        axs[0].axvline(i, color='r')
        axs[1].axvline(i, color='r')
        axs[2].axvline(i, color='r')    
        axs[3].axvline(i, color='r')   
        axs[4].axvline(i, color='r')  
        if not compact:
            axs[6].axvline(i, color='r')
    fig.text(0.5, 0.1, 'Timestamp', ha='center', va='center')
    fig.text(0.08, 0.5, "Sensor Reading (" + r'$\mu$T' + ")", ha='center', va='center', rotation='vertical')
    if(saveToFile):
        fig.savefig(fileName)
    else:
        plt.show()        
    plt.close()    

# Load raw data
def loadRawData(sensor="gyro"):
    inputPath = dataPath
    rawDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("raw.csv"):
#                 print(f)
                dataFile = inputPath + f            
                x = []
                y = []
                z = []
                tsystem = []
                timestamp = []
                linecounter = []
                left = []
                right = []
                with open(dataFile, 'r') as inputFile:
                    reader = csv.reader(inputFile)
                    inputData = list(reader)
                for line in inputData:
                    # new user study data
                    if(len(line)==8):
                        if line[1] == sensor:
                            tsystem.append(int(line[0]))
                            x.append(float(line[2]))
                            y.append(float(line[3]))
                            z.append(float(line[4]))
                            timestamp.append(float(line[6])/1000000)
                            linecounter.append(float(line[7]))
                            left.append(0)
                            right.append(0)
                        lastline = line
                    # old user study data
                    elif(len(line)==7):
                        if line[1] == sensor:
                            tsystem.append(int(line[0]))
                            x.append(float(line[2]))
                            y.append(float(line[3]))
                            z.append(float(line[4]))
                            timestamp.append(float(line[5])/1000000)
                            linecounter.append(float(line[6]))
                            left.append(0)
                            right.append(0)
                        lastline = line
                    elif line[1] == "left":
                        del left[-1]
                        if(len(lastline)==8):
                            left.append(float(lastline[6])/1000000)
                        elif(len(lastline)==7):
                            left.append(float(lastline[5])/1000000)
                    elif line[1] == "right":
                        del right[-1]
                        if(len(lastline)==8):
                            right.append(float(lastline[6])/1000000)
                        elif(len(lastline)==7):
                            right.append(float(lastline[5])/1000000)

                data = {'tsystem' : tsystem, 'x' : x, 'y' : y, 'z' : z, 'timestamp' : timestamp, 'linecounter' : linecounter, 'left' : left, 'right' : right}
                dataFrame = pd.DataFrame.from_dict(data)

                # add dataframe to dictionary
                rawDataSet[f] = dataFrame
    print("done loading raw data")
    return rawDataSet

# Load live correlation data
def loadLiveCorrData():
    inputPath = dataPath
    liveCorrDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                liveCorrDataSet[f] = dataFrame    
    print("done loading livecorr")
    return liveCorrDataSet

# Load feature data
def loadFeatureData():
    inputPath = outputPath
    featureDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("feature.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                dataFrame.columns = ['timestamp', 'feature']
    #             data.feature = pd.ewma(data.feature, span=19)
    #             data.timestamp = data.timestamp - data.timestamp[0]
    #             plot_feature(data, fileName=figureFile, saveToFile=True)
                featureDataSet[f] = dataFrame 
    print("done loading feature")
    return featureDataSet

# Load offline correlation data
def loadOfflineCorrData():
    inputPath = outputPath
    offlineCorrDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("offline.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)            
                dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                offlineCorrDataSet[f] = dataFrame
    print("done loading offline")
    return offlineCorrDataSet

# Load windows
def loadWindows():
    inputPath = windowPath
    windowDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith(".csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]            
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                dataFrame.columns = ['timestamp', 'feature', 'reference', 'adjusted']
                windowDataSet[f] = dataFrame
    print("done loading windows")
    return windowDataSet

 # Load lag data
def loadLagData():
    inputPath = outputPath
    lagDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("lag.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                dataFrame.columns = ['timestamp', 'lagfactor', 'lagtime']
                lagDataSet[f] = dataFrame
    print("done loading lag")
    return lagDataSet

 # Load subsampled data
def loadSubsampleData():
    inputPath = outputPath
    subsampleDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("raw.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                dataFrame.columns = ['timestamp', 'x', 'y', 'z']
                subsampleDataSet[f] = dataFrame
    print("done loading subsample data")
    return subsampleDataSet


def loadFlashData(rawDataSet):

    leftTicksSet = {}
    rightTicksSet = {}

    # Get all data
    for key in rawDataSet:

        # Get data
        df = rawDataSet[key]
        leftTicksSet[key] = [x for x in df.left if x != 0]
        rightTicksSet[key] = [x for x in df.right if x != 0]
        
    print("done loading flashes")
    return leftTicksSet, rightTicksSet

# Overview
def vizOverview(rawSet, subsampleSet, featureSet, offlineCorrSet, lagSet, plotSubsample=True, saveToFile=True, compact=True, excerpt=False, smooth=False):

    # Make folders
    paths = [figurePath + "overview/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)

    # Iterate the data set
    for key in rawSet:

        print("key: " + key)
        # Get data
        rawData = rawSet[key]
        if(plotSubsample): 
            subsampleData = subsampleSet[key]
        featureData = featureSet[key[:-7]+"feature.csv"]
        offlineCorrData = offlineCorrSet[key[:-7]+"offline.csv"]
        lagData = lagSet[key[:-7]+"lag.csv"]

        # Plot magnetometer and feature
        path = figurePath + "overview/"
        if not os.path.exists(path):
            os.makedirs(path)
        figureFile = path + key[:-4] + ".pdf"
        if(plotSubsample):
            plot_overview(rawData, subsampleData, featureData, offlineCorrData, lagData, fileName=figureFile, saveToFile=saveToFile, plotSub=plotSubsample, compact=compact, excerpt=excerpt, smooth=smooth)    
        else:
            plot_overview(rawData, rawData, featureData, offlineCorrData, lagData, fileName=figureFile, saveToFile=saveToFile, plotSub=plotSubsample, compact=compact, excerpt=excerpt, smooth=smooth)    

    print("done plotting overview")

    
# Magnetometer    
def vizMagnetometer(rawSet):

    # Make folders
    paths = [figurePath + "rawmag/", figurePath + "magnitude/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)
    
    # Get all data
    for key in rawSet:

        # Get data
        dataFrame = rawSet[key]

        # Plot magnetometer, with ticks
        figureFile = figurePath + "rawmag/" + key[:-4] + ".pdf"
        plot_3axis_withticks(dataFrame, fileName=figureFile, saveToFile=True)

        # Plot magnetometer, with magnitude, with ticks
        figureFile = figurePath + "magnitude/" + key[:-4] + ".pdf"
        plot_3axis_withmagticks(dataFrame, fileName=figureFile, saveToFile=True)    
    
    print("done plotting magnetometer")
        
# Magnetometer    
def vizSubsampler(rawSet, subsampleSet):

    # Make folders
    paths = [figurePath + "subsample/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)
    
    # Get all data
    for key in rawSet:

        # Get data       
        rawData = []
        subsampleData = []
        rawData = rawSet[key]
        subsampleData = subsampleSet[key]
    
        # Plot magnetometer with magnitude and ticks from raw data
        figureFile = figurePath + "subsample/" + key[:-4] + ".pdf"
        plot_3axis_subwithmagticks(rawData, subsampleData, fileName=figureFile, saveToFile=True)

    print("done plotting subsampler"        )
        
# Feature Map        
def vizFeature(featureSet):
    
    # Make folders
    paths = [figurePath + "feature/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)

    # Get all data
    for key in featureSet:

        # Get data
        dataFrame = featureSet[key]

        # Plot feature
        figureFile = figurePath + "feature/" + key[:-4] + ".pdf"
        plot_feature(dataFrame, fileName=figureFile, saveToFile=True)        

    print("done plotting feature"        )
        
# Magnetometer + Feature        
def vizFeaturePlus(rawSet, featureSet):

    # Make folders
    paths = [figurePath + "featureplus/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)
    
    # Get all data
    for key in rawSet:

        # Get data
        rawData = rawSet[key]
        featureData = featureSet[key[:-7]+"feature.csv"]

        # Plot magnetometer and feature
        figureFile = figurePath + "featureplus/" + key[:-4] + ".pdf"
        plot_3axis_withfeatureandmag(rawData, featureData, fileName=figureFile, saveToFile=True)    
    #     plot_3axis_withfeature(rawData, featureData, fileName=figureFile, saveToFile=True)        

    print("done plotting feature plus")
    
# Online Correlation    
def vizOnlineCorr(corrSet):
    
    # Make folders
    paths = [figurePath + "livecorr/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)
        
    # Get all data
    for key in corrSet:

        # Get data
        liveCorrData = corrSet[key]

        # Plot online correlation
        figureFile = figurePath + "livecorr/" + key[:-4] + ".pdf"
        plot_corr(liveCorrData, fileName=figureFile, saveToFile=True)    

    print("done plotting online corr"        )
        
# Offline Correlation        
def vizOfflineCorr(corrSet):

    # Make folders
    paths = [figurePath + "offlinecorr/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)

    # Get all data
    for key in corrSet:

        # Get data
        offlineCorrData = corrSet[key]

        # Plot offline correlation
        figureFile = figurePath + "offlinecorr/" + key[:-4] + ".pdf"
        plot_corr(offlineCorrData, fileName=figureFile, saveToFile=True)        
    
    print("done plotting offline corr"    )
    
# Combined Correlation        
def vizCorrelation(rawSet, liveCorrSet, offlineCorrSet):

    # Make folders
    paths = [figurePath + "correlation/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)
    
    # Get all data
    for key in rawSet:

        # Get data
        liveCorrData = liveCorrSet[key[:-7]+"corr.csv"]
        offlineCorrData = offlineCorrSet[key[:-7]+"offline.csv"]

        # Plot combined correlation
        figureFile = figurePath + "correlation/" + key[:-7] + "correlation.pdf"
        plot_corrs(liveCorrData, offlineCorrData, fileName=figureFile, saveToFile=True)        

    print("done plotting correlation")
        
# Windows        
def vizWindows(windowSet):
    
    # Make folders
    paths = [figurePath + "windows/"]
    for path in paths:
        if not os.path.exists(path):
            os.makedirs(path)

    # Get all data
    for key in windowSet:

        # Get data
        windowData = windowSet[key]

        # Plot windows
        figureFile = figurePath + "windows/" + key[:-4] + ".pdf"
        plot_window(windowData, fileName=figureFile, saveToFile=True)

    print("done plotting windows"        )




# x = []
# y = []
# z = []
# tsystem = []
# timestamp = []
# linecounter = []
# left = []
# right = []
# with open(dataFile, 'rb') as inputFile:
#     reader = csv.reader(inputFile)
#     inputData = list(reader)
# for line in inputData:
#     if line[1] == "magnet":
#         tsystem.append(int(line[0]))
#         x.append(float(line[2]))
#         y.append(float(line[3]))
#         z.append(float(line[4]))
#         timestamp.append(float(line[5]))
#         linecounter.append(float(line[7]))
#         left.append(0)
#         right.append(0)
#     elif line[1] == "left":
#         del left[-1]
#         left.append(float(lastline[5]))
#     elif line[1] == "right":
#         del right[-1]
#         right.append(float(lastline[5]))
#     lastline = line
# data = {'tsystem' : tsystem, 'x' : x, 'y' : y, 'z' : z, 'timestamp' : timestamp, 'linecounter' : linecounter, 'left' : left, 'right' : right}
# dataFrame = pd.DataFrame.from_dict(data)

# # add dataframe to dictionary
# #         rawDataSet[f] = dataFrame
# print("done loading raw data")




def data_3d_plot(csvSensorData, PLOT_SENSOR='magnet'):
    import math
    def mag(x, y, z):
        return math.sqrt((x*x) + (y*y) + (z*z))
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    plotVector = []
    vectors = []
    timevectors = []
    plt.clf()
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
    
    print((vectors.shape[0]))

    plt.ion()
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim(-50, 50)
    ax.set_ylim(-50, 50)
    ax.set_zlim(-50, 50)
    prev_time = timevectors[0][0] - .001
    total_time = timevectors[timevectors.shape[0]-1][0] - timevectors[0][0]
    print((total_time))
    print(('dividing total_time by a factor of 100000'))
    # for i in range(timevectors.shape[0]):
    i = 0
    import time as system_time
    while i < timevectors.shape[0]:
        # print((timevectors[i]))
        time = timevectors[i][0]
        x = timevectors[i][1]
        y = timevectors[i][2]
        z = timevectors[i][3]
        ax.scatter(x, y, z)
        pause_time = time - prev_time
        pause_time /= 100000
        if pause_time == 0.0:
            pause_time = .00001
        print((pause_time))
        # system_time.sleep(pause_time)
        plt.pause(pause_time)
        prev_time = timevectors[i][0]
        i += 5
    while True:
        plt.pause(0.05)

