function fits = fit_for_moving_cycles(sn_time, sn_data, Ncycles)

T_scale = 1e-9;
T_cycle = 0.75;
order = 2;
skip_cycles = 0;

sn_time = (sn_time - sn_time(1))*T_scale;

fits = [];
passed_time = Ncycles*T_cycle;
while passed_time<=sn_time(end)
    idx = find(sn_time>=(passed_time-Ncycles*T_cycle) & (sn_time<passed_time));
    
    [a, r, fit, t_rec, y_rec] = sinefit(sn_time(idx), sn_data(idx), T_cycle, skip_cycles,order);
    
    fits(end+1) = fit;
    
    passed_time = passed_time + T_cycle;  
end