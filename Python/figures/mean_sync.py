#!/usr/bin/env python3

import os
import pandas as pd
import numpy as np

sit_times = []
walk_times = []

for root, dirs, files in os.walk("."):
    for file in files:
        if not file.endswith("swipe.csv"):
            continue
        swipe_file = root + "/" + file
        if "swipe" in root and "prep" not in root:
            sdf = pd.read_csv(swipe_file, names=["timestamp", "event", "x", "y"])
            events = sdf['event'].tolist()
            if "vibrate" not in events or "swipe right" not in events:
                continue
            vibrate_time = sdf[sdf['event'] == "vibrate"]["timestamp"].values[0]
            swipe_time = sdf[sdf['event'] == "swipe right"]["timestamp"].values[0]
            delta_time = swipe_time - vibrate_time
            if "sit" in root:
                sit_times.append(delta_time)
            elif "walk" in root:
                walk_times.append(delta_time)

print("sit", np.mean(np.array(sit_times)))
print("walk", np.mean(np.array(walk_times)))
