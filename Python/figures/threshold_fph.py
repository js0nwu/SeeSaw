#!/usr/bin/env python3

from imports import *
from lib_util import *
from lib_summary import *
from lib_misc import *

# baseFilePath = "/Users/jwpilly/Research/Synchro/Study_v2/"
# baseFilePath = "/Users/jwpilly/Research/Synchro/Study_v1_750/"
# baseFilePath = "/media/jwpilly/PillowDisk/Research/Synchro/figuredata/figure12/Study_v2"
# baseFilePath = "/home/jwpilly/Downloads/seesaw_data/data1/Synchro/"
# baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/data1/Synchro/"
baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/in-the-wild/Synchro/"

# this block is used to specify which activity to use when generating the 3d plots
ACTIVITY = 'activateauto'
xmax = None
swipe_time = None
# swipe_time = 2.45 # sit
# swipe_time = 2.20 # walk
thresholds = np.linspace(0.2, 1, 17)
times = np.linspace(0, 10, 51)

# this block is used to calculate the true positives and false positives of the 3d plots the data is stored in tps and fps

# tp = syncRateFile("/Users/jwpilly/Research/Synchro/Study_v2/P1/synchro/t20170401112834_p1_sit/")
# fp = syncRateFile("/Users/jwpilly/Research/Synchro/Study_v2/P1/synchro/t20170401112834_p1_sit/", False)
fps = {}
for root, folders, files in os.walk(baseFilePath):
    if not root.endswith("synchro") or "MACOSX" in root:
        continue
    sfolders = sorted(folders, key=lambda x : int(x.split("_")[0].replace("t", "")))
    efolders = [f for f in sfolders[:] if "prep" not in f in f]
    if ACTIVITY != "all":
        efolderpaths = [root + "/" + f + "/" for f in efolders if ACTIVITY in f]
    else:
        efolderpaths = [root + "/" + f + "/" for f in efolders]
    efps = [fpCountFile(efolderpaths[i]) for i in range(len(efolderpaths))]
    for th in thresholds:
        if th not in fps:
            fps[th] = 0
        #fps[th] += efps[th]
        for efp in efps:
            fps[th] += efp[th]
        
# for th in fps: # 3 hours of noise for each condition
#     fps[th] = fps[th] / 3

fps1 = fps
ACTIVITY = 'activatenotif'
fps = {}
for root, folders, files in os.walk(baseFilePath):
    if not root.endswith("synchro") or "MACOSX" in root:
        continue
    sfolders = sorted(folders, key=lambda x : int(x.split("_")[0].replace("t", "")))
    efolders = [f for f in sfolders[:] if "prep" not in f in f]
    if ACTIVITY != "all":
        efolderpaths = [root + "/" + f + "/" for f in efolders if ACTIVITY in f]
    else:
        efolderpaths = [root + "/" + f + "/" for f in efolders]
    efps = [fpCountFile(efolderpaths[i]) for i in range(len(efolderpaths))]
    for th in thresholds:
        if th not in fps:
            fps[th] = 0
        #fps[th] += efps[th]
        for efp in efps:
            fps[th] += efp[th]
        
# for th in fps: # 3 hours of noise for each condition
#     fps[th] = fps[th] / 3

fps2 = fps

plt.clf()
ind = np.arange(len(thresholds))
labels = ["{0:.2f}".format(thresholds[i]) for i in range(len(thresholds))]
y1 = [fps1[thresholds[i]] for i in range(len(thresholds))]
y2 = [fps2[thresholds[i]] for i in range(len(thresholds))]
notif = plt.plot(thresholds, y1, marker="o", linestyle="None", label="With Stimulus")
awake = plt.plot(thresholds, y2, marker="^", linestyle="None", label="Without Stimulus")
#plt.xticks( 0.1 + ind + width / 1.5, labels)
plt.xlabel("Threshold")
plt.ylabel("False Positives")
plt.legend(loc='best')
plt.savefig("inthewildfph.png", bbox_inches="tight")
