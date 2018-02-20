#!/usr/bin/env python3

import os
import pandas as pd
import numpy as np

baseFilePath = "/Users/jwpilly/Desktop/seesaw_data/data1/Synchro"

times = {}

for root, dirs, files in os.walk(baseFilePath):
    for file in files:
        if not file.endswith("sync.csv"):
            continue
        if "prep" not in root:
            continue
        participant = [f for f in root.split("/") if f.startswith("P")][0]
        if participant not in times:
            times[participant] = []
        time_string = [f for f in root.split("/") if f.startswith("t")][0]
        timestamp = int(time_string[1:].split("_")[0])
        sync_file = root + "/" + file
        # print(sync_file)
        sdf = pd.read_csv(sync_file, names=["timestamp", "event", "adjusted_timestamp", "correlation", "linecounter", "direction"])
        if len(sdf) == 2:
            start_time = sdf["timestamp"].values[0]
            end_time = sdf["timestamp"].values[-1]
            sync_time = (end_time - start_time) / 1000
        else:
            sync_time = -1
        times[participant].append((sync_time, timestamp))

num_preps = 0
for p in times:
    if len(times[p]) > num_preps:
        num_preps = len(times[p])
    times[p] = [e[0] for e in sorted(times[p], key=lambda x : x[1])]

if num_preps > 20:
    num_preps = 20
learn = {}
for i in range(num_preps):
    syncs = []
    misses = 0
    for p in times:
        if i < len(times[p]):
            if times[p][i] != -1:
                syncs.append(times[p][i])
            else:
                misses += 1
    mean = np.mean(np.array(syncs))
    stdev = np.std(np.array(syncs))
    learn[i] = (mean, misses)

print(learn)

import matplotlib.pyplot as plt

xs = []
ys = []
ms = []

for i in learn:
    xs.append(i)
    ys.append(learn[i][0])
    ms.append(learn[i][1])

xs = np.array(xs)
ys = np.array(ys)

fig, ax1 = plt.subplots()
ax2 = ax1.twinx()
ax2.bar(xs, ms, color="blue")
ax1.plot(xs, ys, marker="o", linestyle="None", color="black")

ax1.set_ylim(0, 10)
ax1.set_xlim(0, 20)
ax2.set_ylim(0, 10)
ax2.set_xlim(0, 20)
ax1.set_ylabel("Sync Time (s)", color="black")
ax1.set_xlabel("Practice Session #")
ax2.set_ylabel("# of Misses", color="blue")

plt.savefig("learning_rate.png", bbox_inches="tight")
