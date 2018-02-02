import csv
import random as rand
import numpy as np
import pandas as pd
import pprint as pp

import matplotlib.pyplot as pplot
import os

import seaborn as sns
sns.set(color_codes=True)

import sys

from matplotlib import rcParams
rcParams.update({'figure.autolayout': True})

FIGURE_SIZE = (15, 15)

FILE_FORMAT = ".pdf"
CSV_FILE = sys.argv[1]
FOLDER_PREFIX = "./" + sys.argv[2]

WINDOWS = True 

TAP_ACCEPT_THRESHOLD = 3.0

def tupleStat(tupleList):
    leftTuples = tupleList[:,0]
    rightTuples = tupleList[:,1]
    #leftMean = np.mean(leftTuples)
    #rightMean = np.mean(rightTuples)
    #diffMean = leftMean - rightMean
    #return diffMean
    diffs = [leftTuples[i] - rightTuples[i] for i in range(len(leftTuples))]
    return np.std(np.array(diffs))

def flip2Tuple(t):
    return (t[1], t[0])

def itemFlipper(tupleList, flipList):
    newList = []
    for i in range(len(tupleList)):
        if flipList[i]:
            newList.append(flip2Tuple(tupleList[i]))
        else:
            newList.append(tupleList[i])
    return newList
            

def comboIterator(tupleList):
    tupleLength = len(tupleList)
    combos = []
    for i in range(2**tupleLength-1):
        numerical = i+1
        flipList = [True if d == '1' else False for d in "{0:b}".format(numerical).zfill(tupleLength)]
        combos.append(itemFlipper(tupleList, flipList))
    return combos

csvData = pd.read_csv(CSV_FILE, names=['lx','ly','lz','rx','ry','rz','dx','dy','dz', ''])
pairNum = 4
if not os.path.exists(FOLDER_PREFIX):
    os.makedirs(FOLDER_PREFIX)

def mag(x, y, z):
    return np.sqrt(x**2 + y**2 + z**2)

def plot_distribution(ax, data, bins, actual=None, color="blue", drawStds=3, vlines=None, vlinesColor="black", xLabel="x", yLabel="y", title="Title"):
    sns.distplot(data, rug=True, bins=bins, color=color, ax=ax)
    ax.set_title(title)
    ax.set_xlabel(xLabel)
    ax.set_ylabel(yLabel)
    if actual is not None:
        ax.axvline(x=actual, color=vlinesColor, linewidth=2, clip_on=False)
    if vlines is not None:
        for vline in vlines:
            ax.axvline(x=vline, color=vlinesColor, linewidth=0.5, linestyle="dashed")
    if drawStds > 0:
        for i in range(drawStds):
            stdLine = i + 1
            ax.axvline(x=np.mean(np.array(data)) - np.std(np.array(data)) * stdLine, linestyle="dashed", linewidth=0.5, color=color)
            ax.axvline(x=np.mean(np.array(data)) + np.std(np.array(data)) * stdLine, linestyle="dashed", linewidth=0.5, color=color)
    ax.axvline(x=np.mean(data), color=color, linewidth=1)

    
xResults_all = []
yResults_all = []
zResults_all = []
magResults_all = []
queryX_all = []
queryY_all = []
queryZ_all = []
queryMag_all = []
for window in range(len(csvData) - pairNum):
    resultsX = []
    resultsY = []
    resultsZ = []
    tupleListX = []
    tupleListY = []
    tupleListZ = []
    for p in range(pairNum): # creates the tuples from the current window
        tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
        tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
        tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))
    queryX = tupleStat(np.array(tupleListX))
    queryY = tupleStat(np.array(tupleListY))
    queryZ = tupleStat(np.array(tupleListZ))
    windowCombosX = comboIterator(tupleListX)
    windowCombosY = comboIterator(tupleListY)
    windowCombosZ = comboIterator(tupleListZ)
    xResults_i = []
    yResults_i = []
    zResults_i = []
    for i in windowCombosX:
        xResults_i.append(tupleStat(np.array(i)))
    for i in windowCombosY:
        yResults_i.append(tupleStat(np.array(i)))
    for i in windowCombosZ:
        zResults_i.append(tupleStat(np.array(i)))
    std_distance_x = np.std(xResults_i)
    std_distance_y = np.std(yResults_i)
    std_distance_z = np.std(zResults_i)
    std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
    query_mag = mag(queryX, queryY, queryZ)
    results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
    results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
    magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
    if WINDOWS:
        pplot.clf()
        f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
        f.set_size_inches(FIGURE_SIZE)
        plot_distribution(ax1, xResults_i, 10, queryX, "blue", xLabel="Std", yLabel="Density", title="Query X Distribution")
        plot_distribution(ax2, yResults_i, 10, queryY, "blue", xLabel="Std", yLabel="Density", title="Query Y Distribution")
        plot_distribution(ax3, zResults_i, 10, queryZ, "blue", xLabel="Std", yLabel="Density", title="Query Z Distribution")
        plot_distribution(ax4, magResults_i, 10, query_mag, "blue", xLabel="Std", yLabel="Density", title="Combined Axes Magnitude")
        pplot.savefig(FOLDER_PREFIX + "std_window" + str(window) + FILE_FORMAT)
    
    xResults_all.append(xResults_i)
    yResults_all.append(yResults_i)
    zResults_all.append(zResults_i)
    magResults_all.append(magResults_i)
    queryX_all.append(queryX)
    queryY_all.append(queryY)
    queryZ_all.append(queryZ)
    queryMag_all.append(query_mag)

pplot.clf()
f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
f.set_size_inches(FIGURE_SIZE)
xResults = [item for sublist in xResults_all for item in sublist]
yResults = [item for sublist in yResults_all for item in sublist]
zResults = [item for sublist in zResults_all for item in sublist]
magResults = [item for sublist in magResults_all for item in sublist]
plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Std", yLabel="Density", title="Query X Distribution")
plot_distribution(ax1, queryX_all, 10, None, "green", xLabel="Std", yLabel="Density", title="Query X Distribution")
plot_distribution(ax2, yResults, 10, None, "blue", xLabel="Std", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax2, queryY_all, 10, None, "green", xLabel="Std", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax3, zResults, 10, None, "blue", xLabel="Std", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax3, queryZ_all, 10, None, "green", xLabel="Std", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax4, magResults, 10, None, "blue", xLabel="Std", yLabel="Density", title="Combined Axes Magnitude")
plot_distribution(ax4, queryMag_all, 10, None, "green", xLabel="Std", yLabel="Density", title="Combined Axes Magnitude")
pplot.savefig(FOLDER_PREFIX + "std_distribution" + FILE_FORMAT)
pplot.clf()
f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
f.set_size_inches(FIGURE_SIZE)
plot_distribution(ax1, np.mean(np.array(xResults_all), axis=0), 10, np.mean(np.array(queryX_all)), "blue", xLabel="Std", yLabel="Density", title="Query X Distribution")
plot_distribution(ax2, np.mean(np.array(yResults_all), axis=0), 10, np.mean(np.array(queryY_all)), "blue", xLabel="Std", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax3, np.mean(np.array(zResults_all), axis=0), 10, np.mean(np.array(queryZ_all)), "blue", xLabel="Std", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax4, np.mean(np.array(magResults_all), axis=0), 10, np.mean(np.array(queryMag_all)), "blue", xLabel="Std", yLabel="Density", title="Combined Axes Magnitude")
pplot.savefig(FOLDER_PREFIX + "std_mean" + FILE_FORMAT)

def tupleStatMean(tupleList):
    leftTuples = tupleList[:,0]
    rightTuples = tupleList[:,1]
    leftMean = np.mean(leftTuples)
    rightMean = np.mean(rightTuples)
    diffMean = leftMean - rightMean
    return diffMean

xResults_all = []
yResults_all = []
zResults_all = []
magResults_all = []
queryX_all = []
queryY_all = []
queryZ_all = []
queryMag_all = []
for window in range(len(csvData) - pairNum):
    resultsX = []
    resultsY = []
    resultsZ = []
    tupleListX = []
    tupleListY = []
    tupleListZ = []
    for p in range(pairNum): # creates the tuples from the current window
        tupleListX.append((csvData['lx'][window + p], csvData['rx'][window + p]))
        tupleListY.append((csvData['ly'][window + p], csvData['ry'][window + p]))
        tupleListZ.append((csvData['lz'][window + p], csvData['rz'][window + p]))
    queryX = tupleStatMean(np.array(tupleListX))
    queryY = tupleStatMean(np.array(tupleListY))
    queryZ = tupleStatMean(np.array(tupleListZ))
    windowCombosX = comboIterator(tupleListX)
    windowCombosY = comboIterator(tupleListY)
    windowCombosZ = comboIterator(tupleListZ)
    xResults_i = []
    yResults_i = []
    zResults_i = []
    for i in windowCombosX:
        xResults_i.append(tupleStatMean(np.array(i)))
    for i in windowCombosY:
        yResults_i.append(tupleStatMean(np.array(i)))
    for i in windowCombosZ:
        zResults_i.append(tupleStatMean(np.array(i)))
    std_distance_x = np.std(xResults_i)
    std_distance_y = np.std(yResults_i)
    std_distance_z = np.std(zResults_i)
    std_distance_mag = mag(std_distance_x, std_distance_y, std_distance_z)
    query_mag = mag(queryX, queryY, queryZ)
    results_i_mean_mag = mag(np.mean(xResults_i), np.mean(yResults_i), np.mean(zResults_i))
    results_i_std_mag = mag(np.std(xResults_i), np.std(yResults_i), np.std(zResults_i))
    magResults_i = [mag(xResults_i[i], yResults_i[i], zResults_i[i]) for i in range(len(xResults_i))]
    if WINDOWS:
        pplot.clf()
        f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
        f.set_size_inches(FIGURE_SIZE)
        plot_distribution(ax1, xResults_i, 10, queryX, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
        plot_distribution(ax2, yResults_i, 10, queryY, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
        plot_distribution(ax3, zResults_i, 10, queryZ, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
        plot_distribution(ax4, magResults_i, 10, query_mag, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
        pplot.savefig(FOLDER_PREFIX + "mean_window" + str(window) + FILE_FORMAT)
    
    xResults_all.append(xResults_i)
    yResults_all.append(yResults_i)
    zResults_all.append(zResults_i)
    magResults_all.append(magResults_i)
    queryX_all.append(queryX)
    queryY_all.append(queryY)
    queryZ_all.append(queryZ)
    queryMag_all.append(query_mag)

pplot.clf()
f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
f.set_size_inches(FIGURE_SIZE)
xResults = [item for sublist in xResults_all for item in sublist]
yResults = [item for sublist in yResults_all for item in sublist]
zResults = [item for sublist in zResults_all for item in sublist]
magResults = [item for sublist in magResults_all for item in sublist]
plot_distribution(ax1, xResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
plot_distribution(ax1, queryX_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query X Distribution")
plot_distribution(ax2, yResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax2, queryY_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax3, zResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax3, queryZ_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax4, magResults, 10, None, "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
plot_distribution(ax4, queryMag_all, 10, None, "green", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
pplot.savefig(FOLDER_PREFIX + "mean_distribution" + FILE_FORMAT)
pplot.clf()
f, (ax1, ax2, ax3, ax4) = pplot.subplots(4)
f.set_size_inches(FIGURE_SIZE)
plot_distribution(ax1, np.mean(np.array(xResults_all), axis=0), 10, np.mean(np.array(queryX_all)), "blue", xLabel="Mean", yLabel="Density", title="Query X Distribution")
plot_distribution(ax2, np.mean(np.array(yResults_all), axis=0), 10, np.mean(np.array(queryY_all)), "blue", xLabel="Mean", yLabel="Density", title="Query Y Distribution")
plot_distribution(ax3, np.mean(np.array(zResults_all), axis=0), 10, np.mean(np.array(queryZ_all)), "blue", xLabel="Mean", yLabel="Density", title="Query Z Distribution")
plot_distribution(ax4, np.mean(np.array(magResults_all), axis=0), 10, np.mean(np.array(queryMag_all)), "blue", xLabel="Mean", yLabel="Density", title="Combined Axes Magnitude")
pplot.savefig(FOLDER_PREFIX + "mean_mean" + FILE_FORMAT)
