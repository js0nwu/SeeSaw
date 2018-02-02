Scripts used to generate average correlation over time graphs (comparing noise/non-noise, triangular/square/sine reference wave, diff-triangular/diff-sine feature)
Scripts meant to be run from the Magnetics/Python/Magnetics folder

file: figure_gen5.py ** Used to generate figure 7 in the paper **
description: script to generate the (uses diff-triangular feature) correlation over time graph of all sensor data files in a directory (compares noise vs non-noise). Uses growing window
usage: python figure_gen5.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave] [noise file]
examples:
generate overall correlation over time graph for all participants
python figure_gen5.py "User Study/Raw" 0.0 False allnoise.csv
generate correlation over time graph for a specific user
python figure_gen5.py "User Study/Raw/P1" 0.0 False allnoise.csv
generate correlation over time graphs for each of the users
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen5.py {} 0.0 False allnoise.csv \;

file: figure_gen5_diff.py
description: script to generate the (compares diff-triangular feature against magnitude) correlation over time graph of all non-noise sensor data files in a directory. Uses growing window
usage: python figure_gen5_diff.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use saw-tooth reference wave]
examples:
generate overall correlation over time graph for all participants
python figure_gen5_diff.py "User Study/Raw" 0.0 False
generate correlation over time graph for a specific user
python figure_gen5_diff.py "User Study/Raw/P1" 0.0 False
generate correlation over time graphs for each of the users
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen5_diff.py {} 0.0 False \;

file: figure_gen5_square.py
description: script to generate the (compares diff-triangular feature against magnitude) correlation over time graph of all non-noise sensor data files in a directory. Uses growing window
usage: python figure_gen5_square.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave]
examples:
generate overall correlation over time graph for all participants
python figure_gen5_square.py "User Study/Raw" 0.0 False
generate correlation over time graph for a specific user
python figure_gen5_square.py "User Study/Raw/P1" 0.0 False
generate correlation over time graphs for each of the users
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen5_square.py {} 0.0 False \;

file: figure_gen5_t.py
description: script to generate the (uses diff-sine feature) correlation over time graph of all non-noise sensor data files in a directory. Uses growing window
usage: python figure_gen5_t.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave]
examples:
generate overall correlation over time graph for all participants
python figure_gen5_t.py "User Study/Raw" 0.0 False
generate correlation over time graph for a specific user
python figure_gen5_t.py "User Study/Raw/P1" 0.0 False
generate correlation over time graphs for each of the users
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen5_t.py {} 0.0 False \;

file: figure_gen5_tl.py
description: script to generate the (uses diff-triangular feature) correlation over time graph of all non-noise sensor data files in a directory. Uses growing window
usage: python figure_gen5_tl.py [TARGET FOLDER w/o trailing slash] [seconds to skip in beginning of data files] [use square reference wave]
examples:
generate overall correlation over time graph for all participants
python figure_gen5_tl.py "User Study/Raw" 0.0 False
generate correlation over time graph for a specific user
python figure_gen5_tl.py "User Study/Raw/P1" 0.0 False
generate correlation over time graphs for each of the users
find "User Study/Raw" -name "*P*" -type d -exec python figure_gen5_tl.py {} 0.0 False \;