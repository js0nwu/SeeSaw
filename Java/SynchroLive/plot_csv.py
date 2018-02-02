import matplotlib.pyplot as pplot
import sys
import pandas as pd
import matplotlib.font_manager

input_file = sys.argv[1]
output_file = sys.argv[2]

print(input_file)

pplot.clf()
input_data = pd.read_csv(input_file, names=['x', 'y'])
for i in range(len(input_data['y'])):
    yVal = input_data['y'][i]
    xVal = input_data['x'][i]
pplot.gca().set_ylim(-1, 1)
pplot.scatter(input_data['x'], input_data['y'])
fp = matplotlib.font_manager.FontProperties()
fp.set_size('xx-small')
pplot.legend(prop=fp, loc='lower right')
pplot.savefig(output_file)
