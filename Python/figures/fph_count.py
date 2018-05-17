#!/usr/bin/env python3

import os
import subprocess
import numpy as np
from collections import Counter
import sys

baseFilePath = sys.argv[1]

testFiles = []
THRESHOLDS = np.linspace(0, 1, num=31)
SYNC_LOCK = 5000

for root, folders, files in os.walk(baseFilePath):
    testFiles.extend([root + "/" + f for f in files])

def test_file(filepath, thresholds):
    with open(os.devnull, "w") as devnull:
        result = subprocess.run(["java", "-jar", "javaapp-offlinetester.jar", filepath, "-1", "0"], stdout=subprocess.PIPE, stderr=devnull)
        result_string = result.stdout.decode("utf-8")
        lines = [line for line in result_string.split("\n") if "," in line]
        if len(lines) < 1:
            return Counter(), 0
        total_time = float(lines[-1].split(",")[0]) - float(lines[0].split(",")[0])
        fph_counter = Counter()
        if "\n" in result_string:
            for threshold in thresholds:
                last_sync = 0
                for line in lines:
                    row = line.split(",")
                    timestamp = float(row[0])
                    correlation = float(row[1])
                    linecounter = int(row[2])
                    direction = row[3]
                    if correlation >= threshold and direction == "top" and timestamp - last_sync > SYNC_LOCK:
                        fph_counter[threshold] += 1
                        last_sync = timestamp

        return fph_counter, total_time


fphs = Counter()
all_time = 0

i = 0
for file in testFiles:
    i += 1
    print(i, "/", len(testFiles))
    file_fphs, file_time = test_file(file, THRESHOLDS)
    fphs = fphs + file_fphs
    all_time += file_time

fph_dict = dict(fphs)
for threshold in THRESHOLDS:
    if threshold not in fph_dict:
        continue
    fph = fph_dict[threshold] * 3600000 / all_time
    print(threshold, ",", fph)




