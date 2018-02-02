function [mean_y, std_y, bb] = checkBinsStats(sensor1)

dd = sensor1(1).xyz(:,1);

N_bin_cycles = 10;
N_bins = 4;

tt = 1e-9*sensor1(1).time2/0.75 * N_bin_cycles; 
tt = tt-tt(1);

d1 = dd( (tt(end)-tt) < N_bin_cycles*N_bins );
t1 = tt( (tt(end)-tt) < N_bin_cycles*N_bins );

y = zeros(N_bin_cycles, 1);

for i=N_bin_cycles:-1:1
    mean_y(i) = mean(d1( mod(round(t1), N_bin_cycles) == (i-1)));
    std_y(i) = std(d1( mod(round(t1), N_bin_cycles) == (i-1)));
    bb(i) = i;
end
