[raw,sn] = readExperimentData('p1_walknoise_t1476900113453_ssensorData.csv');
[raw,ss] = readExperimentData('p1_walksync_t1476900096622_ssensorData.csv');
close all
T_scale = 1e-9;
T_cycle = 0.75;
t_start = 6;
Ncycles = 5;
t_finish = t_start+ (0.01+Ncycles)*T_cycle;
order = 2;

ys = ss(1).xyz(:,1);
yn = sn(1).xyz(:,1);

ts = ss(1).time2*T_scale;
tn = sn(1).time2*T_scale;

ts = ts-ts(1);
tn = tn-tn(1);

idx_s = find(ts>=t_start & ts<=t_finish);
idx_n = find(tn>=t_start & tn<=t_finish);

[a, r, fit_s, t_rec_s, y_rec_s] = sinefit(ts(idx_s), ys(idx_s), T_cycle, 0,order);
[a, r, fit_n, t_rec_n, y_rec_n] = sinefit(tn(idx_n), yn(idx_n), T_cycle, 0,order);

figure;
plot(ts, ys, t_rec_s, y_rec_s);
xlabel('Time [s]')
ylabel('X axis mag. signal')
title(['Sync motion while walking: SNR = ' num2str(fit_s)])
legend('Measurements','Fitted harmonic signal')
grid on;

figure;
plot(tn, yn, t_rec_n, y_rec_n);
xlabel('Time [s]')
ylabel('X axis mag. signal')
title(['Random motion while walking: SNR = ' num2str(fit_n)])
legend('Measurements','Fitted harmonic signal')
grid on;
