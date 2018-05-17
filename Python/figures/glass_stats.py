#!/usr/bin/env python3

import pandas as pd
import os
from os import listdir
from os.path import isfile, join
import numpy as np
import sys

baseFilePath = "/Users/jwpilly/Desktop/study4/glass"

glass_files = []
participant = ""

if len(sys.argv) == 2:
    participant = sys.argv[1]

for root, folders, files in os.walk(baseFilePath):
    for f in files:
        if f == "data.csv" and (participant == "" or participant in root):
            glass_files.append(root + "/" + f)

glass_dfs = [pd.read_csv(gf, header=None, names=["timestamp", "event", "number", "time"]) for gf in glass_files]

dismiss_count = 0
miss_count = 0

dismiss_times = []

for gdf in glass_dfs:
    dismisses = gdf[gdf.event == "dismissed"]
    dismiss_count += len(dismisses)
    for dt in dismisses.time.values:
        dismiss_times.append(dt)
    misses = gdf[gdf.event == "missed"]
    miss_count += len(misses)

print("success rate", float(dismiss_count) / float(dismiss_count + miss_count))

print("mean dismiss time", np.mean(np.array(dismiss_times)))
