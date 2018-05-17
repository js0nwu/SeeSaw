#!/usr/bin/env python3

import pandas as pd
from os import listdir
from os.path import isfile, join
import os

baseFilePath = "/Users/jwpilly/Desktop/study3/data"

ACTIVITY = "walk"

NOISE_BUFFER = 2000
SESSION_LENGTH = 10000

activity_folders = []

for root, folders, files in os.walk(baseFilePath):
    if not root.endswith("synchro") or "MACOSX" in root:
        continue
    # print(root, folders)
    for folder in folders:
        if ACTIVITY in folder or ACTIVITY == "all":
            activity_folders.append(root + "/" + folder)

raw_files = []
for afolder in activity_folders:
    folder = afolder + "/data"
    files = [folder + "/" + f for f in listdir(folder) if isfile(join(folder, f)) and f.endswith("raw.csv")]
    raw_files.extend(files)

raw_df_names = ["timestamp", "sensor", "x", "y", "z", "timestamp2", "timestamp3", "linecounter"]
raw_dfs = [pd.read_csv(f, header=None, names=raw_df_names) for f in raw_files]
sync_dfs = [pd.read_csv(f.replace("raw.csv", "sync.csv"), header=None, names=["timestamp", "event", "timestamp2", "correlation", "linecounter", "direction"]) for f in raw_files]

noise_data = []

for i in range(len(raw_dfs)):
    rdf = raw_dfs[i]
    sdf = sync_dfs[i]
    if len(sdf) > 1:
        begin_time = sdf.timestamp.values[0]
        end_time = sdf.timestamp.values[1]
    elif len(sdf) == 1:
        begin_time = sdf.timestamp.values[0]
        end_time = begin_time + SESSION_LENGTH
    else:
        continue

    noise_begin = rdf[rdf.timestamp < begin_time - NOISE_BUFFER]
    noise_end = rdf[rdf.timestamp > end_time + NOISE_BUFFER]
    noise_data.append(noise_begin)
    noise_data.append(noise_end)

NOISE_DIR = "./noise"
if not os.path.exists(NOISE_DIR):
    os.makedirs(NOISE_DIR)

def add_ticks(df, period=500):
    df2 = df.copy()
    df2 = df2.drop(df2[df2.sensor != "gyro"].index)
    top = True
    tick_times = []
    last_tick = 0
    for index, row in df.iterrows():
        if row.timestamp - last_tick >= period:
            last_tick = row.timestamp
            tick_times.append(row.timestamp)

    df2 = df2.append(pd.DataFrame({"timestamp" : [df.timestamp.min() - 1], "sensor" : ["begin 1000"]}))
    df2 = df2.append(pd.DataFrame({"timestamp" : [df.timestamp.max() + 1], "sensor" : ["end"]}))
    for tick in tick_times:
        df2 = df2.append(pd.DataFrame({"timestamp" : [tick], "sensor" : ["top" if top else "bottom"]}))
        top = not top
    df2 = df2.sort_values(by="timestamp")
    return df2
        
for i in range(len(noise_data)):
    noise_file = NOISE_DIR + "/" + ACTIVITY + "_" + str(i) + ".csv"
    print(i, "/", len(noise_data))
    add_ticks(noise_data[i])[raw_df_names].to_csv(noise_file, encoding="utf-8", index=False, header=False)


    
