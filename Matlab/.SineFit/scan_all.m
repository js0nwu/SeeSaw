function scan_all
dd = dir('*_ssensorData.csv');


T_scale = 1e-9;
T_cycle = 0.75;
skip_cycles = 0;
order = 2;

xls_array = {'File','X magnet','Y magnet','Z magnet','NORM magnet','X accel','Y accel','Z accel','NORM acc','X gyro','Y gyro','Z gyro', 'NORM gyro'};


for i=1:length(dd)
    xls_array{1+i,1} = dd(i).name(1:end-4);
    [~,sn] = readExperimentData(dd(i).name);
    
    for j = 1:length(sn)
        for k=1:3
            [a, r, fit(j,k), t_rec, y_rec] = sinefit(sn(j).time2*T_scale, sn(j).xyz(:,k), T_cycle, skip_cycles,order);
        end
        [a, r, fit(j,4), t_rec, y_rec] = sinefit(sn(j).time2*T_scale, sn(j).norm, T_cycle, skip_cycles,order);
    end
    
    xls_array(1+i,1 + (1:4) + 4*0) = num2cell(fit(1,:));
    xls_array(1+i,1 + (1:4) + 4*1) = num2cell(fit(2,:));
    xls_array(1+i,1 + (1:4) + 4*2) = num2cell(fit(3,:));
   
end
xlswrite('Results.xls',xls_array)