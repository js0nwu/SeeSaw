
dd = dir('*_ssensorData.csv');



Ncycles = 6

ii_fits = [1:100]';
fits = zeros(100,length(dd));

max_fit = 0;

for i=1:length(dd)
    data_name(i) = {strtok(dd(i).name(4:end),'_')};
    [raw,sn] = readExperimentData(dd(i).name);
    fits_s = fit_for_moving_cycles(sn(1).time2, sn(1).xyz(:,1), Ncycles);
    
    max_fit = max(max_fit, length(fits_s))
    fits(1:length(fits_s),i) = fits_s(:);
end

ii_fits = ii_fits(1:max_fit);
fits = fits(1:max_fit,:);
    

figure(Ncycles);
for i=1:size(fits,2)
    if isempty(strfind(data_name{i},'sync'))
        plot(ii_fits, fits(:,i),'+k');
        
    else
        plot(ii_fits, fits(:,i),'or');
    end
    hold on;
end
        
title(['Ncycles = ' num2str(Ncycles)]);shg