import sys
import shutil
import subprocess
import os

CSV_PATH = sys.argv[1]
CSV_NOISE_PREFIX = CSV_NOISE_PREFIX = "p1_e3_t1476900674888" # still 750

FIGURE_WORKER = "figure_gen2.py"

PROCESS_WAIT = True

ds = [(False, False), (False, True), (True, False), (True, True)]

prefixes = set([f[:f.index("_s")] for f in os.listdir(CSV_PATH) if f.endswith(".csv") and not f.startswith(".")])
for p in prefixes:
    for d in ds:
        procParams = ["python", FIGURE_WORKER, CSV_PATH, p, CSV_NOISE_PREFIX, "True" if d[0] else "False", "True" if d[1] else "False"]
        print(str(procParams))
        fileOutput = p + "_" + ("wd" if d[0] else "nd") + ("ws" if d[1] else "ns") + "_output.txt"
        with open(fileOutput, "w") as outfile:
            proc = subprocess.Popen(procParams, stdout=outfile)
        if PROCESS_WAIT:
            proc.wait()
            print("finished", p, str(d))
