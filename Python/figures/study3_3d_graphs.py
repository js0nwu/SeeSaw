#!/usr/bin/env python3

import pandas as pd
import os
from os import listdir
from os.path import isfile, join
import numpy as np
import sys

baseFilePath = "/Users/jwpilly/Desktop/study3/data"
ACTIVITY = sys.argv[1]
MISS_MS = 10000.1
THRESHOLDS = np.linspace(0.2, 1, num=31)
TIMES = np.linspace(2, 10, num=51)

sit_swipe_time = 2.81
walk_swipe_time = 2.58

EPSILON = 0.00001

activity_folders = []

for root, folders, files in os.walk(baseFilePath):
    if not root.endswith("synchro") or "MACOSX" in root:
        continue
    # print(root, folders)
    for folder in folders:
        if ACTIVITY in folder or ACTIVITY == "all":
            activity_folders.append(root + "/" + folder)

corr_files = []
for afolder in activity_folders:
    folder = afolder + "/data"
    files = [folder + "/" + f for f in listdir(folder) if isfile(join(folder, f)) and f.endswith("corr.csv")]
    corr_files.extend(files)

corr_dfs = [pd.read_csv(f, header=None, names=["timestamp", "correlation", "linecounter", "direction"]) for f in corr_files]

tprs = []

for threshold in THRESHOLDS:
    sync_times = []
    for df in corr_dfs:
        start_timestamps = df[df.linecounter == 0].timestamp
        if len(start_timestamps) < 1:
            continue
        start_time = start_timestamps.values[0]
        end_frame = df[(df.timestamp > start_time) & (df.correlation >= threshold)].timestamp
        sync_time = MISS_MS if len(end_frame) < 1 else end_frame.values[0] - start_time
        sync_times.append(sync_time / 1000)
    for t in TIMES:
        tps = len([s for s in sync_times if s <= t])
        total = len(sync_times)
        tpr = float(tps) / float(total)
        if tpr == 0:
            tpr = EPSILON
        tprs.append((threshold, t, tpr))

precisions = np.array(tprs)
xd = precisions[:,0]
yd = precisions[:,1]
zd = precisions[:,2]

import matplotlib.mlab as mlab
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.collections import PolyCollection
from matplotlib.colors import colorConverter
from matplotlib import cm

import matplotlib.pyplot as plt
plt.clf()
fig = plt.figure()
ax = fig.gca()
xi = np.linspace(np.min(xd), np.max(xd))
yi = np.linspace(np.min(yd), np.max(yd))
X, Y = np.meshgrid(xi, yi)
Z = mlab.griddata(xd, yd, zd, xi, yi, interp='linear')
contour = ax.contourf(X, Y, Z, np.linspace(0, 1, num=21), cmap=cm.jet)

fig.colorbar(contour)
ax.set_xlabel("Threshold")
ax.set_ylabel("Time (s)")

if ACTIVITY == "sit":
    plt.axhline(y=sit_swipe_time, linewidth=2.5, linestyle="dashed", color="black")
if ACTIVITY == "walk":
    plt.axhline(y=walk_swipe_time, linewidth=2.5, linestyle="dashed", color="black")

plt.savefig("study3_3d_graphs_" + ACTIVITY + ".png", bbox_inches="tight")

