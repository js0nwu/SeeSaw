[raw,sensor_noise] = readExperimentData('p1_walknoise_t1476900113453_ssensorData.csv')
[raw,sensor_sync] = readExperimentData('p1_walksync_t1476900096622_ssensorData.csv')

[my1, sy1, b1] = checkBinsStats(sensor_noise)
[my2, sy2, b2] = checkBinsStats(sensor_sync)
