from subprocess import call

reference_periods = [750, 1000, 1250]
generator_periods = [750, 1000, 1250, 0]
window_size = [50, 100, 200, 300, 400, 500]
infile = "p3_t1478789646579_xsitting1000_1/p3_t1478789646579_xsitting1000_1_ssensorData.csv"

for rp in reference_periods:
    for gp in generator_periods:
        for ws in window_size:
            out_file = "jarout_" + str(ws) + "_" + str(rp) + "_" + str(gp) + ".csv"
            graph_args = [infile, str(ws), str(rp), str(gp), out_file]
            call_args = ["java", "-jar", "SynchroLive.jar"] + graph_args
            call(call_args)
