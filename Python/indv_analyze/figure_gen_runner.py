import os
import sys
import subprocess

RUNDIR = "./"

MATCH_SUFFIX = "combinedMag.csv"

FIGURE_WORKER = "figure_gen.py"

PROCESS_WAIT = True

if len(sys.argv) > 1:
  RUNDIR = sys.argv[1]

print("RUN DIR " + str(RUNDIR))
dirFiles = [f for f in os.listdir(RUNDIR) if os.path.isfile(os.path.join(RUNDIR, f))]

for f in dirFiles:
  if f.endswith(MATCH_SUFFIX):
    name = f if "_t" not in f else f.split("_t")[0]
    print("generating figures for " + name)
    p = subprocess.Popen(["python", FIGURE_WORKER, f, RUNDIR + "./figures/figures_" + name + "/"])
    if PROCESS_WAIT:
      p.wait()
      print("finished " + name)
