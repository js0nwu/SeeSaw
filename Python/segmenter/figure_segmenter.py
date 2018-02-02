import sys
import os
import random

CSV_DATA_FILE = sys.argv[1]
CSV_DATA_PREFIX = CSV_DATA_FILE.replace(".csv", "")
CSV_DATA_SUFFIX = "_ssensorData.csv"

SENSOR_FILE_MAP = {"magnet":"combinedMag", "accel":"combinedAccel", "gyro":"combinedGyro"}
USE_SAME_RANDOM = False
RANDOM_CONTINUOUS = True 
OUTPUT_ENTIRE_NOISE = True 
DATA_ROW_LENGTH = 8
DATA_LINE_COUNTER_INDEX = 6
DATA_TIMESTAMP_INDEX = 0
DATA_SENSOR_INDEX = 1
DATA_SENSOR_DATA_START = 2
DATA_SENSOR_DATA_END = 4

def get_sensor_suffix(sensorName):
    return "_s" + SENSOR_FILE_MAP[sensorName] + ".csv"

def sample_data_file(sensorData, sensors):
    sampledData = {}
    processStack = []
    sensorValues = {}
    for rowI in range(len(sensorData)):
        currentRow = sensorData[rowI]
        currentRowColumns = currentRow.split(",")
        if len(currentRowColumns) == DATA_ROW_LENGTH:
            rowSensor = currentRowColumns[DATA_SENSOR_INDEX]
            if rowSensor in sensors:
                rowSensorData = currentRowColumns[DATA_SENSOR_DATA_START:DATA_SENSOR_DATA_END + 1]
                rowSensorData = [float(v) for v in rowSensorData]
                sensorValues[rowSensor] = rowSensorData
        else:
            eventColumn = currentRowColumns[DATA_SENSOR_INDEX]
            if eventColumn == "left" or eventColumn == "right":
                processStack.append(sensorValues.copy())
                if len(processStack) == 2:
                    rightValues = processStack.pop()
                    leftValues = processStack.pop()
                    for sensor in sensors:
                        sampledRow = ""
                        sensorLeftValues = leftValues[sensor]
                        sensorRightValues = rightValues[sensor]
                        for v in sensorLeftValues:
                            sampledRow += str(v) + ","
                        for v in sensorRightValues:
                            sampledRow += str(v) + ","
                        if len(sensorLeftValues) == len(sensorRightValues):
                            diffValues = [sensorLeftValues[i] - sensorRightValues[i] for i in range(len(sensorLeftValues))]
                            for v in diffValues:
                                sampledRow += str(v) + ","
                        if sensor not in sampledData:
                            sampledData[sensor] = []
                        sampledData[sensor].append(sampledRow + "\n")
    return sampledData


def insert_timer_ticks(noiseData, interval):
    noiseDataCopy = noiseData[:]
    currentTick = "left"
    insertOffset = 0
    lastTick = 0
    for rowI in range(len(noiseData)):
        currentRow = noiseData[rowI]
        currentRowColumns = currentRow.split(",")
        if len(currentRowColumns) != DATA_ROW_LENGTH:
            continue
        currentRowTime = int(currentRowColumns[DATA_TIMESTAMP_INDEX])
        if lastTick == 0 or (currentRowTime - lastTick) >= interval:
            if lastTick != 0:
                tickRow = str(currentRowTime) + "," + currentTick + ",\n"
                noiseDataCopy.insert(rowI + insertOffset, tickRow)
                insertOffset += 1
                if currentTick == "left":
                    currentTick = "right"
                else:
                    currentTick = "left"
            lastTick = currentRowTime
    return noiseDataCopy

def get_random_data(noiseData, length, randomContinuous=RANDOM_CONTINUOUS):
    randomData = []
    if randomContinuous:
        startIndex = random.randint(0, len(noiseData) - length - 1)
        while True:
            startRow = noiseData[startIndex]
            startRowColumns = startRow.split(",")
            if len(startRowColumns) != DATA_ROW_LENGTH:
                startIndex = random.randint(0, len(noiseData) - length - 1)
                continue
            endIndex = startIndex + length - 1
            endRow = noiseData[endIndex]
            endRowColumns = endRow.split(",")
            if len(endRowColumns) != DATA_ROW_LENGTH or int(endRowColumns[DATA_LINE_COUNTER_INDEX]) - int(startRowColumns[DATA_LINE_COUNTER_INDEX]) != length - 1:
                startIndex = random.randint(0, len(noiseData) - length - 1)
            else:
                break
        for i in range(length):
            rowIndex = startIndex + i
            randomData.append(noiseData[rowIndex])
    else:
        startIndex = random.randint(0, len(noiseData) - length - 1)
        indexOffset = 0
        while len(randomData) < length:
            currentRow = noiseData[startIndex + indexOffset]
            currentRowColumns = currentRow.split(",")
            if len(currentRowColumns) != DATA_ROW_LENGTH:
                startIndex = random.randint(0, len(noiseData) - length - 1)
                indexOffset = 0
            else:
                startLineCounter = int(noiseData[startIndex].split(",")[DATA_LINE_COUNTER_INDEX])
                currentLineCounter = int(noiseData[startIndex + indexOffset].split(",")[DATA_LINE_COUNTER_INDEX])
                if currentLineCounter - startLineCounter == indexOffset:
                    randomData.append(currentRow)
                    indexOffset += 1
                else:
                    startIndex = random.randint(0, len(noiseData) - length - 1)
                    indexOffset = 0
        for i in range(length):
            rowIndex = startIndex + i
            noiseRow = noiseData[rowIndex]
            randomData.append(noiseRow)
    return randomData

csvDataLines = open(CSV_DATA_FILE).readlines()
print("loaded csv file with open:", CSV_DATA_FILE, len(csvDataLines), "lines")

fileData = {}
fileCounter = {}
fileStack = ['noise']

startKeyword = "begin"
stopKeyword = "end"

for rowI in range(len(csvDataLines)):
    if rowI % int(len(csvDataLines) / 10) == 0:
        print("progress:", rowI, len(csvDataLines))
    currentRow = csvDataLines[rowI]
    currentRowColumns = currentRow.split(",")
    row = currentRowColumns[DATA_SENSOR_INDEX]
    if row.startswith(startKeyword):
        startFile = row[len(startKeyword):]
        fileStack.append(startFile)
        if startFile not in fileCounter:
            fileCounter[startFile] = 0
        fileCounter[startFile] += 1
        continue
    elif row == stopKeyword:
        fileStack.pop()
        continue
    currentFile = fileStack[-1]
    
    if currentFile == "noise":
        experimentName = currentFile
    else:
        experimentName = currentFile + "_" + str(fileCounter[currentFile])
    if experimentName not in fileData:
        fileData[experimentName] = [] 
    fileData[experimentName].append(csvDataLines[rowI])

fileLength = 0
for currentFile in fileData:
    if currentFile != "noise" and len(fileData[currentFile]) > fileLength:
        fileLength = len(fileData[currentFile])

print("longest file length:", fileLength)
sameRandom = None

if USE_SAME_RANDOM:
    sameRandom = get_random_data(fileData['noise'], fileLength)
    print("generated random noise data for all files")

if OUTPUT_ENTIRE_NOISE:
    noiseFileName = CSV_DATA_PREFIX + "total_noise" + CSV_DATA_SUFFIX
    noiseFile = open(noiseFileName, 'w')
    noiseFile.writelines(fileData['noise'])
    noiseFile.close()

for currentFile in fileData:
    if currentFile == 'noise':
        continue
    outputDir = "./" + CSV_DATA_PREFIX + currentFile + "/"
    if not os.path.exists(outputDir):
        os.makedirs(outputDir)
    csvDataFile = outputDir + CSV_DATA_PREFIX + currentFile + CSV_DATA_SUFFIX
    outputSensorFile = open(csvDataFile, 'w')
    outputSensorFile.writelines(fileData[currentFile])
    outputSensorFile.close()
    fileNoiseName = outputDir + CSV_DATA_PREFIX + currentFile + "_noise" + CSV_DATA_SUFFIX
    fileNoiseFile = open(fileNoiseName, 'w')
    fileInterval = int(currentFile.split("_")[0]) // 2
    if sameRandom is not None:
        randomNoiseData = insert_timer_ticks(sameRandom[:len(fileData[currentFile])], fileInterval)
    else:
        randomNoiseData = insert_timer_ticks(get_random_data(fileData['noise'], len(fileData[currentFile])), fileInterval)
        print("generated random noise data for", currentFile)
    fileNoiseFile.writelines(randomNoiseData)
    fileNoiseFile.close()
    print("wrote noise file for", currentFile)
    sensors = ["magnet", "accel", "gyro"]
    sampledSensorFile = sample_data_file(fileData[currentFile], sensors)
    sampledNoiseFile = sample_data_file(randomNoiseData, sensors)

    for s in sampledSensorFile:
        outputSampleName = outputDir + CSV_DATA_PREFIX + currentFile + get_sensor_suffix(s)
        outputSampleFile = open(outputSampleName, 'w')
        outputSampleFile.writelines(sampledSensorFile[s])
        outputSampleFile.close()
        print("sampled file:", currentFile, "sensor:", s)
    for s in sampledNoiseFile:
        outputSampleName = outputDir + CSV_DATA_PREFIX + currentFile + "_noise" + get_sensor_suffix(s)
        outputSampleFile = open(outputSampleName, 'w')
        outputSampleFile.writelines(sampledNoiseFile[s])
        outputSampleFile.close()
        print("sampled noise file:", currentFile, "sensor:", s)

    
print("processed file:", CSV_DATA_FILE)