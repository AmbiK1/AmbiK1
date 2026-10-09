import sys
from imgs import *
out = sys.argv[1]
B = Block
T = lambda: B('term', 'Начало')
E = lambda: B('term', 'Конец')
charts = {
 1: Seq(T(), B('io', 'Ввод x, y'), B('process', 'z = (|x| − |y|) / (1 + |x · y|)'), B('io', 'Вывод z'), E()),
 2: Seq(T(), B('io', 'Ввод a, b'), B('process', 'cubeMean = (a³ + b³) / 2'),
        B('process', 'geomMean = √(|a| · |b|)'), B('io', 'Вывод cubeMean,\ngeomMean'), E()),
 3: Seq(T(), B('io', 'Ввод x, y'),
        If('x² + y² ≤ 1\nи y ≥ −x', B('io', 'Вывод «Точка попадает\nв область»'),
           B('io', 'Вывод «Точка не попадает\nв область»')), E()),
 4: Seq(T(), B('io', 'Ввод n'),
        For('i = 0; i < n; i = i + 1', B('io', 'Ввод a[i]')),
        B('process', 'negSum = 0\nposSum = 0'),
        For('i = 0; i < n; i = i + 1',
            If('a[i] < 0', B('process', 'negSum = negSum + a[i]'),
               If('a[i] > 0', B('process', 'posSum = posSum + a[i]')))),
        B('io', 'Вывод negSum, posSum'), E()),
 5: Seq(T(), B('io', 'Ввод N'), B('process', 's = 0\nfactI = 1'),
        For('i = 1; i ≤ N; i = i + 1',
            Seq(B('process', 'factI = factI · i'), B('process', 'p = 1\nfactJ = 1'),
                For('j = 1; j ≤ i; j = j + 1',
                    Seq(B('process', 'factJ = factJ · j'), B('process', 'p = p · factJ / factI'))),
                B('process', 's = s + p'))),
        B('io', 'Вывод s'), E()),
}
for k, c in charts.items():
    print(k, flowchart(c, f'{out}/flow{k}.png'))
