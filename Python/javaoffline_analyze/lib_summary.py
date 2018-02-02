import numpy as np
import os
import pandas as pd

IGNORE_TIME = 2000


def accel_check(x, y, z):
    return x > -5 and y > -1.5 and z > -0.5
#     return True

def mag_check(m):
    return m < 40
#    return True

# this block is used to generate the sync rate over time graphs

def processSyncFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                liveCorrDataSet[f] = dataFrame
    onlineCorrDataSet = liveCorrDataSet
    thresholds = [i / 10 for i in range(10)]
    outputSet = {}
    for i in thresholds:
        averageTimes = []
        for key in onlineCorrDataSet.keys():
            df = onlineCorrDataSet[key]
            if "timestamp" not in df.columns or "correlation" not in df.columns:
                continue
            st = df.timestamp[0]
            dfsync = df[(df.correlation >= i) & (df.timestamp - st > IGNORE_TIME)]
            sync_time = dfsync.timestamp.values[0] - st if len(dfsync > 0) else -1
#             print(sync_time)
            if sync_time > 0:
                averageTimes.append(sync_time)
        outputSet[i] = np.mean(averageTimes) / 1000
    return outputSet

# this block is used in the correlation over time graphs
corr_times = np.linspace(0, 10000, 51)
def correlateSyncFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                liveCorrDataSet[f] = dataFrame
    onlineCorrDataSet = liveCorrDataSet
    sync_threshold = 0.9
    corrs = []
    for key in onlineCorrDataSet.keys():
        df = onlineCorrDataSet[key]
        if "timestamp" not in df.columns or "correlation" not in df.columns:
            continue
        st = df.timestamp[0]
        dfsync = df[(df.correlation >= sync_threshold) & (df.timestamp - st > IGNORE_TIME)]
        synctime = dfsync.timestamp.values[0] if len(dfsync) > 0 else -1
#         print(synctime)
        if synctime > -1:
            mask = (df.timestamp - st > IGNORE_TIME) & (df.timestamp >= synctime)
            df.loc[mask, 'correlation'] = sync_threshold
        corrs.append(np.interp(corr_times + st, df.timestamp, df.correlation))
    corrs = np.array(corrs)
#     print(corrs)
    avg_corrs = np.mean(corrs, axis=0)
    return avg_corrs

# this block is used in the magnitude over time graphs
def magSyncFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("mag.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                liveCorrDataSet[f] = dataFrame
    onlineCorrDataSet = liveCorrDataSet
    sync_threshold = 999
    corrs = []
    for key in onlineCorrDataSet.keys():
        df = onlineCorrDataSet[key]
        if "timestamp" not in df.columns or "correlation" not in df.columns:
            continue
        st = df.timestamp[0]
        dfsync = df[(df.correlation >= sync_threshold) & (df.timestamp - st > IGNORE_TIME)]
        synctime = dfsync.timestamp.values[0] if len(dfsync) > 0 else -1
#         print(synctime)
        if synctime > -1:
            mask = (df.timestamp - st > IGNORE_TIME) & (df.timestamp >= synctime)
            df.loc[mask, 'correlation'] = sync_threshold
        corrs.append(np.interp(corr_times + st, df.timestamp, df.correlation))
    corrs = np.array(corrs)
#     print(corrs)
    avg_corrs = np.mean(corrs, axis=0)
    return avg_corrs

# this block is used to generate the correlation over time graphs with the accel data
def correlateSyncAccelFile(filePath, noise=False, output=False):
    import sys
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    liveRawDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if not noise:
                if "noise" in f:
                    continue
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    rawDataFrame = pd.read_csv(dataFile.replace("corr", "raw") + ".csv", header=None)
                except:
                    if output:
                        print("error in " + f)
                        print(sys.exc_info()[0])
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                liveCorrDataSet[f] = dataFrame
                liveRawDataSet[f] = rawDataFrame
    onlineCorrDataSet = liveCorrDataSet
    onlineRawDataSet = liveRawDataSet
    sync_threshold = 0.9
    corrs = []
    accels = []
    for key in onlineCorrDataSet.keys():
        df = onlineCorrDataSet[key]
        rdf = onlineRawDataSet[key]
        accel_mask = (rdf.sensor == "accel")
#         plt.clf()
#         plt.plot(rdf[accel_mask].system_time, rdf[accel_mask].x)
#         plt.plot(rdf[accel_mask].system_time, rdf[accel_mask].y)
#         plt.plot(rdf[accel_mask].system_time, rdf[accel_mask].z)
#         plt.show()
#         return
        if "timestamp" not in df.columns or "correlation" not in df.columns:
            continue
        st = df.timestamp[0]
        df.correlation = np.clip(df.correlation, -1, 1)
        dfsync = df[(df.correlation >= sync_threshold) & (df.timestamp - st > IGNORE_TIME)]
        synctime = dfsync.timestamp.values[0] if len(dfsync) > 0 else -1
#         print(synctime)
        if synctime > -1:
            mask = (df.timestamp - st > IGNORE_TIME) & (df.timestamp >= synctime)
            df.loc[mask, 'correlation'] = sync_threshold
        corrs.append(np.interp(corr_times + st, df.timestamp, df.correlation))
        axs = np.interp(corr_times + st, rdf[accel_mask].adjusted_time, rdf[accel_mask].x)
        ays = np.interp(corr_times + st, rdf[accel_mask].adjusted_time, rdf[accel_mask].y)
        azs = np.interp(corr_times + st, rdf[accel_mask].adjusted_time, rdf[accel_mask].z)
#         plt.clf()
#         plt.plot(corr_times, axs)
#         plt.plot(corr_times, ays)
#         plt.plot(corr_times, azs)
#         plt.show()
#         return
        accels.append((axs, ays, azs))
    corrs = np.array(corrs)
    accels = np.array(accels)
#     print(corrs)
    avg_corrs = np.mean(corrs, axis=0)
    avg_accels = np.mean(accels, axis=0)
    return avg_corrs, avg_accels

# this block is used to generate the correlation over time graphs with the delta data
def correlateSyncDeltaFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    liveDeltaDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    deltaDataFrame = pd.read_csv(dataFile.replace("corr", "delta") + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                deltaDataFrame.columns = ['timestamp', 'delta']
                liveCorrDataSet[f] = dataFrame
                liveDeltaDataSet[f] = deltaDataFrame
    onlineCorrDataSet = liveCorrDataSet
    onlineDeltaDataSet = liveDeltaDataSet
    sync_threshold = 0.9
    corrs = []
    deltas = []
    for key in onlineCorrDataSet.keys():
        df = onlineCorrDataSet[key]
        rdf = onlineDeltaDataSet[key]
        if "timestamp" not in df.columns or "correlation" not in df.columns:
            continue
        st = df.timestamp[0]
        dfsync = df[(df.correlation >= sync_threshold) & (df.timestamp - st > IGNORE_TIME)]
        synctime = dfsync.timestamp.values[0] if len(dfsync) > 0 else -1
#         print(synctime)
        if synctime > -1:
            mask = (df.timestamp - st > IGNORE_TIME) & (df.timestamp >= synctime)
            df.loc[mask, 'correlation'] = sync_threshold
        corrs.append(np.interp(corr_times + st, df.timestamp, df.correlation))
        ads = np.interp(corr_times + st, rdf.timestamp, rdf.delta)
#         plt.clf()
#         plt.plot(corr_times, axs)
#         plt.plot(corr_times, ays)
#         plt.plot(corr_times, azs)
#         plt.show()
#         return
        deltas.append(ads)
    corrs = np.array(corrs)
    deltas = np.array(deltas)
#     print(corrs)
    avg_corrs = np.mean(corrs, axis=0)
    avg_deltas = np.mean(deltas, axis=0)
    return avg_corrs, avg_deltas

# this block is used to generate the correlation over time graphs with the mag data
def correlateSyncMagFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    liveMagDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    magDataFrame = pd.read_csv(dataFile.replace("corr", "mag") + ".csv", header=None)
                except:
                    print("error in " + f)
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                magDataFrame.columns = ['timestamp', 'mag', 'linecounter']
                liveCorrDataSet[f] = dataFrame
                liveMagDataSet[f] = magDataFrame
    onlineCorrDataSet = liveCorrDataSet
    onlineMagDataSet = liveMagDataSet
    sync_threshold = 0.9
    corrs = []
    deltas = []
    for key in onlineCorrDataSet.keys():
        df = onlineCorrDataSet[key]
        rdf = onlineMagDataSet[key]
        if "timestamp" not in df.columns or "correlation" not in df.columns:
            continue
        st = df.timestamp[0]
        dfsync = df[(df.correlation >= sync_threshold) & (df.timestamp - st > IGNORE_TIME)]
        synctime = dfsync.timestamp.values[0] if len(dfsync) > 0 else -1
#         print(synctime)
        if synctime > -1:
            mask = (df.timestamp - st > IGNORE_TIME) & (df.timestamp >= synctime)
            df.loc[mask, 'correlation'] = sync_threshold
        corrs.append(np.interp(corr_times + st, df.timestamp, df.correlation))
        ads = np.interp(corr_times + st, rdf.timestamp, rdf.mag)
#         plt.clf()
#         plt.plot(corr_times, axs)
#         plt.plot(corr_times, ays)
#         plt.plot(corr_times, azs)
#         plt.show()
#         return
        deltas.append(ads)
    corrs = np.array(corrs)
    deltas = np.array(deltas)
#     print(corrs)
    avg_corrs = np.mean(corrs, axis=0)
    avg_deltas = np.mean(deltas, axis=0)
    return avg_corrs, avg_deltas

# this block is used in the 3d plot generation
# thresholds = [0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.60, 0.65, 0.70, 0.75, 0.8, 0.85, 0.9, 0.95, 1, 1.05, 1.1, 1.15, 1.2]
thresholds = np.linspace(0, 1, 21)
times = np.linspace(0, 10, 51)
def syncRateFile(filePath, noise=False):
    dataPath = filePath + ("/noise/" if noise else "/data/")
    inputPath = dataPath
    liveCorrDataSet = {}
    liveRawDataSet = {}
    liveMagDataSet = {}
    liveSyncDataSet = {}
    for root, folders, files in os.walk(inputPath):
        for f in files:    
            if f.endswith("corr.csv"):
                if not noise and "noise" in f:
                    continue
                if noise and "noise" not in f:
                    continue
#                 print(f)
                dataFile = inputPath + f[:-4]
                try:
                    dataFrame = pd.read_csv(dataFile + ".csv", header=None)
                    rawDataFrame = pd.read_csv(dataFile.replace("corr", "raw") + ".csv", names=['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter'])
                    magDataFrame = pd.read_csv(dataFile.replace("corr", "mag") + ".csv", names=['timestamp', 'magnitude', 'linecounter'])
                    try:
                        syncDataFrame = pd.read_csv(dataFile.replace("corr", "sync") + ".csv", names=['system_time', 'sync_type', 'adjusted_time', 'correlation', 'linecounter', 'direction']) if not noise else None
                    except Exception as e:
                        print("no sync file in " + dataFile)
                        print(str(e))
                        syncDataFrame = None
                except Exception as e2:
                    print("error in " + dataFile)
                    print(str(e2))
                    continue
                if len(dataFrame) < 1:
                    continue
                if(dataFrame.shape[1]==3):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter']
                elif(dataFrame.shape[1]==4):
                    dataFrame.columns = ['timestamp', 'correlation', 'linecounter', 'direction']
                rawDataFrame.columns = ['system_time', 'sensor', 'x', 'y', 'z', 'adjusted_time', 'nano_time', 'linecounter']
                magDataFrame.columns = ['timestamp', 'magnitude', 'linecounter']
                liveCorrDataSet[f] = dataFrame
                liveRawDataSet[f] = rawDataFrame
                liveMagDataSet[f] = magDataFrame
                liveSyncDataSet[f] = syncDataFrame
    onlineCorrDataSet = liveCorrDataSet
    onlineRawDataSet = liveRawDataSet
    onlineMagDataSet = liveMagDataSet
    onlineSyncDataSet = liveSyncDataSet
    outputSet = {}
    for i in thresholds:
#         averageTimes = []
        sync_counter = [0 for i in range(len(times))]
        total_counter = [0 for i in range(len(times))]
        for key in onlineCorrDataSet.keys():
            df = onlineCorrDataSet[key]
            rdf = onlineRawDataSet[key]
            mdf = onlineMagDataSet[key]
            sdf = onlineSyncDataSet[key]
            if "timestamp" not in df.columns or "correlation" not in df.columns:
                continue
            st = df.timestamp[0]
            if sdf is not None and len(sdf) == 2:
                sdf_sync = sdf.adjusted_time.values[-1]
#                 print("sdf_sync", sdf_sync)
                df_sync_rows = df[df.timestamp == sdf_sync]
#                 print("key", key)
#                 print("df_sync_len", len(df_sync_rows))
#                 print(df_sync_rows)
#                 print("sync_type", sdf.sync_type.values[-1])
                if len(df_sync_rows) >= 1:
                    df_sync_value = df_sync_rows.correlation.values[0]
    #                 print("df_sync_value", df_sync_value, "original_sync_value", sdf.correlation.values[-1])
                    after_sync = df.timestamp >= sdf_sync
#                     print("old last", df.correlation.values[-1])
                    df.loc[after_sync, 'correlation'] = df_sync_value
#                     print("df_sync_value", df_sync_value)
#                     print("new last", df.correlation.values[-1])
            dfsync = df[(df.correlation >= i) & (df.timestamp - st > IGNORE_TIME)]
            sync_time = dfsync.timestamp.values[0] if len(dfsync > 0) else -1
#             sync_time -= st
#             print(sync_time)
            rdfsync = rdf[(rdf.sensor == 'accel') & (rdf.adjusted_time >= sync_time)]
            mdfsync = mdf[(mdf.timestamp >= sync_time)]
            accel_pass = len(rdfsync) > 0 and accel_check(rdfsync.x.values[0], rdfsync.y.values[0], rdfsync.z.values[0])
            mag_pass = len(mdfsync) > 0 and mag_check(mdfsync.magnitude.values[0])
            sync_time = rdfsync.adjusted_time.values[0] if sync_time != -1 and accel_pass and mag_pass else -1
#             sync_time = sync_time - st if sync_time != -1 else -1
#             print(sync_time / 1000)
            for ti in range(len(times)):
                if sync_time != -1 and (sync_time / 1000) <= times[ti]:
                    sync_counter[ti] += 1
                total_counter[ti] += 1
            outputSet[i] = (sync_counter, total_counter)
#             if sync_time > 0:
#                 averageTimes.append(sync_time)
#         outputSet[i] = np.mean(averageTimes) / 1000
    return outputSet
