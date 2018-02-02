function [a, r, fit, t_rec, y_rec] = sinefit(t, y, T_cycle, skip_cycles, ord)

if nargin<4 || isempty(skip_cycles)
    skip_cycles = 0;
end
if nargin<5 || isempty(ord)
    ord = 1;
end
 

t = t(:);
y = y(:);

% Cutting data into chunks (slow but definite) and removing the

y_bias = y*0;

tf = t(1) + (skip_cycles+1)*T_cycle;
% Cutting leftovers
idc = (t<(tf-T_cycle)); 
y(idc) = [];
t(idc) = [];
y_bias(idc) = [];

while tf<=t(end)
    idd = (t>=(tf-T_cycle) & t<tf);
    y_bias(idd) = mean(y(idd));
    y(idd) = y(idd) - mean(y(idd));
    tf = tf+T_cycle;
end
% Cutting leftovers
idc = (t>=(tf-T_cycle)); 
y(idc) = [];
t(idc) = [];
y_bias(idc) = [];

omega = 2*pi/T_cycle;

for i=ord:-1:1
    A(:,(i-1)*2+[1:2]) = [sin(i*omega*t), cos(i*omega* t)];
end

%a = lscov(A, y);
a = inv(A'*A)*A'*y;

r = A*a - y;

if norm(a)*10000000<norm(r)
    fit = 0;
else
    fit = 1/2*(norm(a))^2/cov(r);
end

y_rec = y_bias + A*a;
t_rec = t;
