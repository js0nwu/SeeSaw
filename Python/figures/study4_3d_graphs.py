#!/usr/bin/env python3

import pandas as pd
import os
from os import listdir
from os.path import isfile, join
import numpy as np
import sys

MISS_MS = 10000.1
THRESHOLDS = np.linspace(0.2, 1, num=31)
TIMES = np.linspace(1, 10, num=51)

EPSILON = 0.00001
WATCH_DATA = "/Users/jwpilly/Desktop/study4/watch2.csv"
GLASS_DATA = "/Users/jwpilly/Desktop/study4/glass2.csv"

wdf = pd.read_csv(WATCH_DATA, header=None, names=["timestamp", "timestamp2", "correlation", "linecounter", "direction"])
gdf = pd.read_csv(GLASS_DATA, header=None, names=["timestamp", "event", "seqnum", "time"])

starts = gdf[gdf.event == "show"].timestamp.values

tprs = []

for threshold in THRESHOLDS:
    sync_times = []
    for start in starts:
        df = wdf[(wdf.timestamp >= start) & (wdf.timestamp < (start + MISS_MS))]
        if len(df) < 1:
            continue
        start_time = df.timestamp.values[0]
        end_frame = df[df.correlation > threshold].timestamp
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

plt.savefig("study4_3d_graphs.png", bbox_inches="tight")

