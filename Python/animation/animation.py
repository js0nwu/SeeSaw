# python3 animation.py "User Study/Raw2/P1/p1_t1478897598685_xbrowsing1000_2"
import sys
import numpy as np
import pandas as pd
import matplotlib.pyplot as pplot

def mag(x, y, z):
    return np.sqrt(x**2 + y**2 + z**2)
def data_3d_plot():
    csvData = csvSensorData
    plotX = []
    plotY = []
    plotZ = []
    plotMag = []
    plotVector = []
    vectors = []
    timevectors = []
    pplot.clf()
    for rowI in range(len(csvData['sensor'])):
        row = csvData['sensor'][rowI]
        if row == PLOT_SENSOR:
            x = csvData['x'][rowI]
            y = csvData['y'][rowI]
            z = csvData['z'][rowI]
            t = csvData['time'][rowI]
            plotX.append((t, x))
            plotY.append((t, y))
            plotZ.append((t, z))
            plotMag.append((t, mag(x, y, z)))
            plotVector.append([0, 0, 0, x, y, z])
            vectors.append([x, y, z])
            timevectors.append([t, x, y, z])
        elif row == 'left':
            #pplot.axvline(x=csvData['time'][rowI], color='red')
            pass
        elif row == 'right':
            #pplot.axvline(x=csvData['time'][rowI], color='black')
            pass
        elif row == 'vibrate':
            #pplot.axvline(x=csvData['time'][rowI], color='green')
            pass
    import matplotlib.pyplot as plt
    from mpl_toolkits.mplot3d import Axes3D
    import matplotlib.animation as animation

    vectors = np.array(vectors)
    timevectors = np.array(timevectors)
    timevectors[:,1] = timevectors[:,1][::-1]
    timevectors[:,2] = timevectors[:,2][::-1]
    timevectors[:,3] = timevectors[:,3][::-1]
    soa =np.array(plotVector) 

    X,Y,Z,U,V,W = zip(*soa)

    plt.ion()
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim(-50, 50)
    ax.set_ylim(-50, 50)
    ax.set_zlim(-50, 50)
    prev_time = timevectors[0][0] - .001
    total_time = timevectors[timevectors.shape[0]-1][0] - timevectors[0][0]
    print (total_time)
    print ('dividing total_time by a factor of 100000')
    i = 0
    while i < timevectors.shape[0]:
        # print (timevectors[i])
        time = timevectors[i][0]
        x = timevectors[i][1]
        y = timevectors[i][2]
        z = timevectors[i][3]
        ax.scatter(x, y, z)
        pause_time = time - prev_time
        pause_time /= 100000
        if pause_time == 0.0:
            pause_time = .00001
        # print (pause_time)
        plt.pause(pause_time)
        prev_time = timevectors[i][0]
        i += 5
    while True:
        plt.pause(0.05)

CSV_PATH = "./" + sys.argv[1] + "/"
CSV_PREFIX = sys.argv[1].split("/")[-1]
CSV_SENSOR_SUFFIX = "_ssensorData.csv"
CSV_SENSOR_FILE = CSV_PATH + CSV_PREFIX + CSV_SENSOR_SUFFIX
csvSensorData = pd.read_csv(CSV_SENSOR_FILE, names=['time','sensor','x','y','z','timestamp','linecounter',''])
PLOT_SENSOR = "magnet"

data_3d_plot()