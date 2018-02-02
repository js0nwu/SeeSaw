Scripts used to generate summary figures of all of the participants and trials 
Scripts meant to be run from the Magnetics/Python/Magnetics folder

file: figure_gen6.py ** Used to generate figure 8 in the paper **
description: script to generate the (uses diff-triangular feature) threshold vs window time vs accuracy graph. Uses growing window
usage: python figure_gen6.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave] [frequency] [noise file]
examples:
generate threshold vs window time vs accuracy graph for all participants for 750 ms
python figure_gen6.py "User Study/Raw" 0.0 False 750 allnoise.csv
generate threshold vs window time vs accuracy graph for a specific user for 750 ms
python figure_gen6.py "User Study/Raw/P1" 0.0 False 750 allnoise.csv
generate threshold vs window time vs accuracy graphs for each of the users for 750 ms
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen6.py {} 0.0 False 750 allnoise.csv \;

file: figure_gen7.py ** Used to generate figure 6 in the paper **
description: script to generate the (uses diff-triangular feature) average response lag graph (compared by frequency). Uses growing window
usage: python figure_gen7.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave]
examples:
generate average response time comparison graph for all participant data
python figure_gen7.py "User Study/Raw" 0.0

file: figure_gen8.py ** Used to generate figure 9 in the paper **
description: script to generate the (uses diff-triangular feature) threshold vs window time vs false positives per hour graph. Uses growing window
usage: python figure_gen8.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave] [frequency] [noise file]
examples:
generate threshold vs window time vs false positives per hour graph for all participants for 750 ms
python figure_gen8.py "User Study/Raw" 0.0 False 750 allnoise.csv
generate threshold vs window time vs false positives per hour graph for a specific user for 750 ms
python figure_gen8.py "User Study/Raw/P1" 0.0 False 750 allnoise.csv
generate threshold vs window time vs false positives per hour graphs for each of the users for 750 ms
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen8.py {} 0.0 False 750 allnoise.csv \;