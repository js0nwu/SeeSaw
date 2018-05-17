#!/usr/bin/env python3

from imports import *
from lib_util import *
from lib_summary import *
from lib_misc import *

# this block is used to specify which activity to use when generating the 3d plots
xmax = None
swipe_time = None
# swipe_time = 2.45 # sit
# swipe_time = 2.20 # walk
thresholds = np.linspace(0.2, 1, 17)
times = np.linspace(0, 10, 51)

ACTIVITY = 'all'
# baseFilePath = "/Users/jwpilly/Research/Synchro/Study_v2/"
# baseFilePath = "/Users/jwpilly/Research/Synchro/Study_v1_750/"
# baseFilePath = "/media/jwpilly/PillowDisk/Research/Synchro/figuredata/figure12/Study_v2"
# baseFilePath = "/home/jwpilly/Downloads/seesaw_data/data1/Synchro/"
# baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/data1/Synchro/"
# baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/in-the-wild/Synchro/"
# baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/expert/Synchro/"
baseFilePath = "/Users/jwpilly/Desktop/study3/data/"

# this block is used to calculate the true positives and false positives of the 3d plots the data is stored in tps and fps

tps = {}
fps = {}
ttot = {}
ftot = {}
for root, folders, files in os.walk(baseFilePath):
    if not root.endswith("synchro") or "MACOSX" in root:
        continue
    sfolders = sorted(folders, key=lambda x : int(x.split("_")[0].replace("t", "")))
    efolders = [f for f in sfolders[:] if "prep" not in f in f]
    if ACTIVITY != "all":
        efolderpaths = [root + "/" + f + "/" for f in efolders if ACTIVITY in f]
    else:
        efolderpaths = [root + "/" + f + "/" for f in efolders]
    etps = [syncRateFile(efolderpaths[i], xm=xmax) for i in range(len(efolderpaths))]
    efps = [syncRateFile(efolderpaths[i], True, xm=xmax) for i in range(len(efolderpaths))]
    for th in thresholds:
        if th not in tps:
            tps[th] = np.array([0 for i in times])
        if th not in fps:
            fps[th] = np.array([0 for i in times])
        if th not in ttot:
            ttot[th] = np.array([0 for i in times])
        if th not in ftot:
            ftot[th] = np.array([0 for i in times])
        for stps in etps:
            tps[th] += np.array(stps[th][0])
            ttot[th] += np.array(stps[th][1])
        for sfps in efps:
            fps[th] += np.array(sfps[th][0])
            ftot[th] += np.array(sfps[th][1])

# this block is used to generate the precision graph with 0.5 * tp + 0.5 tn

precisions = []
for th in thresholds:
    for ti in range(len(times)):
        ptime = times[ti]
        if ptime < IGNORE_TIME / 1000:
            continue
        tp_rate = tps[th][ti] / ttot[th][ti]
        fp_rate = fps[th][ti] / ftot[th][ti]
        prec = 0.5 * tp_rate + 0.5 * (1 - fp_rate)
        point = [th, ptime, prec]
        precisions.append(point)
precisions = np.array(precisions)
xd = precisions[:,0]
yd = precisions[:,1]
zd = precisions[:,2]

import matplotlib.mlab as mlab
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.collections import PolyCollection
from matplotlib.colors import colorConverter
from matplotlib import cm


plt.clf()
fig = plt.figure(figsize=(8, 6))
# ax = fig.gca(projection='3d')
ax = fig.gca()
xi = np.linspace(np.min(xd), np.max(xd))
yi = np.linspace(np.min(yd), np.max(yd))
X, Y = np.meshgrid(xi, yi)
Z = mlab.griddata(xd, yd, zd, xi, yi, interp='linear')
contour = ax.contourf(X, Y, Z, np.linspace(0.0, 1.0, 21, endpoint=True), cmap=cm.jet)

if swipe_time is not None:
    ax.axhline(y=swipe_time, linestyle="dashed", color="black", lw=2.5)

fig.colorbar(contour)


ax.set_xlabel("Threshold")
ax.set_ylabel("Time (s)")
plt.savefig("3d_all_" + ACTIVITY + ".png", bbox_inches="tight")

# this graph is used to generate the true positive graph

precisions = []
for th in thresholds:
    for ti in range(len(times)):
        ptime = times[ti]
        if ptime < IGNORE_TIME / 1000:
            continue
        tp_rate = tps[th][ti] / ttot[th][ti]
        prec = tp_rate
        point = [th, ptime, prec]
        precisions.append(point)
precisions = np.array(precisions)
xd = precisions[:,0]
yd = precisions[:,1]
zd = precisions[:,2]

import matplotlib.mlab as mlab
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.collections import PolyCollection
from matplotlib.colors import colorConverter
from matplotlib import cm


plt.clf()
fig = plt.figure(figsize=(8, 6))
ax = fig.gca()
xi = np.linspace(np.min(xd), np.max(xd))
yi = np.linspace(np.min(yd), np.max(yd))
X, Y = np.meshgrid(xi, yi)
Z = mlab.griddata(xd, yd, zd, xi, yi, interp='linear')
contour = ax.contourf(X, Y, Z, np.linspace(0.0, 1.0, 21, endpoint=True), cmap=cm.jet)

if swipe_time is not None:
    ax.axhline(y=swipe_time, linestyle="dashed", color="black", lw=2.5)

fig.colorbar(contour)


ax.set_xlabel("Threshold")
ax.set_ylabel("Time (s)")
plt.savefig("3d_tp_" + ACTIVITY + ".png", bbox_inches="tight")

# this graph is used to generate the false positive rate graph

precisions = []
for th in thresholds:
    for ti in range(len(times)):
        ptime = times[ti]
        if ptime < IGNORE_TIME / 1000:
            continue
        fp_rate = fps[th][ti] / ftot[th][ti]
        prec = fp_rate
        point = [th, ptime, prec]
        precisions.append(point)
precisions = np.array(precisions)
xd = precisions[:,0]
yd = precisions[:,1]
zd = precisions[:,2]

import matplotlib.mlab as mlab
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.collections import PolyCollection
from matplotlib.colors import colorConverter
from matplotlib import cm


plt.clf()
fig = plt.figure(figsize=(8, 6))
ax = fig.gca()
xi = np.linspace(np.min(xd), np.max(xd))
yi = np.linspace(np.min(yd), np.max(yd))
X, Y = np.meshgrid(xi, yi)
Z = mlab.griddata(xd, yd, zd, xi, yi, interp='linear')
contour = ax.contourf(X, Y, Z, np.linspace(0.0, 1.0, 21, endpoint=True), cmap=cm.jet)

if swipe_time is not None:
    ax.axhline(y=swipe_time, linestyle="dashed", color="black", lw=2.5)

fig.colorbar(contour)


ax.set_xlabel("Threshold")
ax.set_ylabel("Time (s)")
plt.savefig("3d_fpr_" + ACTIVITY + ".png", bbox_inches="tight")

# this block is used to generate the false positives per hour 3d graph

precisions = []
for th in thresholds:
    for ti in range(len(times)):
        ptime = times[ti]
        if ptime < IGNORE_TIME / 1000:
            continue
        prec = (fps[th][ti] / ftot[th][ti]) / 6 * 3600
        point = [th, ptime, prec]
        precisions.append(point)
precisions = np.array(precisions)
xd = precisions[:,0]
yd = precisions[:,1]
zd = precisions[:,2]

import matplotlib.mlab as mlab
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.collections import PolyCollection
from matplotlib.colors import colorConverter
from matplotlib import cm


plt.clf()
fig = plt.figure(figsize=(8, 6))
ax = fig.gca()
xi = np.linspace(np.min(xd), np.max(xd))
yi = np.linspace(np.min(yd), np.max(yd))
X, Y = np.meshgrid(xi, yi)
Z = mlab.griddata(xd, yd, zd, xi, yi, interp='linear')
from matplotlib.colors import LogNorm
lvls = np.logspace(0, np.log10(np.max(zd)), 26, endpoint=True)
contour = ax.contourf(X, Y, Z, levels=lvls, cmap=cm.jet, norm=LogNorm())

if swipe_time is not None:
    ax.axhline(y=swipe_time, linestyle="dashed", color="black", lw=2.5)

fig.colorbar(contour, ticks=lvls, format="%.1f")


ax.set_xlabel("Threshold")
ax.set_ylabel("Time (s)")
plt.savefig("3d_fph_" + ACTIVITY + ".png", bbox_inches="tight")

