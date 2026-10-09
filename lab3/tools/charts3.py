import sys
import imgs
imgs.GAP = 56
imgs.BH = 96
from imgs import *
out = sys.argv[1]
B = Block
T = lambda: B('term', 'Начало')
E = lambda: B('term', 'Конец')
P = lambda t: B('process', t)
IO = lambda t: B('io', t)
charts = {
 1: Seq(T(), IO('Ввод n, m'), IO('Ввод матрицы A(n × m)'), P('firstNeg = −1\nlastPos = −1'),
        For('i = 0; i < n; i = i + 1',
            For('j = 0; j < m; j = j + 1',
                Seq(P('k = i · m + j'),
                    If('a[i][j] < 0 и\nfirstNeg = −1', P('firstNeg = k')),
                    If('a[i][j] > 0', P('lastPos = k'))))),
        If('firstNeg ≠ −1', IO('Вывод a[firstNeg / m]\n[firstNeg mod m], firstNeg + 1'),
           IO('Вывод «Отрицательных\nэлементов нет»')),
        If('lastPos ≠ −1', IO('Вывод a[lastPos / m]\n[lastPos mod m], lastPos + 1'),
           IO('Вывод «Положительных\nэлементов нет»')),
        E()),
 2: Seq(T(), IO('Ввод n'), For('i = 0; i < n; i = i + 1', IO('Ввод a[i]')),
        For('i = 0; i < n; i = i + 1',
            For('j = 0; j < n; j = j + 1', P('b[i][j] = a[(i + j) mod n]'))),
        IO('Вывод матрицы B(n × n)'), E()),
 3: Seq(T(), IO('Ввод n, m'), IO('Ввод матрицы A(n × m)'),
        For('j = 0; j < m; j = j + 1',
            Seq(P('ch[j] = 0'),
                For('i = 0; i < n; i = i + 1',
                    If('a[i][j] < 0 и\na[i][j] нечетное', P('ch[j] = ch[j] + |a[i][j]|'))))),
        For('p = 0; p < m − 1; p = p + 1',
            For('j = 0; j < m − 1 − p; j = j + 1',
                If('ch[j] > ch[j + 1]',
                   Seq(P('Обмен ch[j] и ch[j + 1]'),
                       For('i = 0; i < n; i = i + 1', P('Обмен a[i][j]\nи a[i][j + 1]')))))),
        IO('Вывод ch'), IO('Вывод матрицы A'), E()),
 4: Seq(T(), IO('Ввод text, k'), P('i = 0, r = 0'),
        While("text[i] ≠ '\\0'",
              If('text[i] — буква',
                 Seq(P('start = i'),
                     While('text[i] — буква', P('i = i + 1')),
                     P('len = i − start'),
                     If('len = k и\ntext[start] —\nсогласная',
                        While("text[i] = ' '", P('i = i + 1')),
                        For('j = start; j < i; j = j + 1', P('result[r] = text[j]\nr = r + 1')))),
                 P('result[r] = text[i]\nr = r + 1\ni = i + 1'))),
        P("result[r] = '\\0'"), IO('Вывод result'), E()),
 5: Seq(T(), IO('Ввод text'), P('vowels = 0\nconsonants = 0'),
        For("i = 0; text[i] ≠ '\\0'; i = i + 1",
            If('text[i] — буква',
               If('text[i] — гласная', P('vowels = vowels + 1'), P('consonants =\nconsonants + 1')))),
        IO('Вывод vowels,\nconsonants'),
        If('vowels > consonants', IO('Вывод «Гласных\nбукв больше»'),
           If('consonants > vowels', IO('Вывод «Согласных\nбукв больше»'),
              IO('Вывод «Гласных\nи согласных поровну»'))),
        E()),
}
def split(seq, at, label='A'):
    a = Seq(*seq.items[:at], Conn(label))
    b = Seq(Conn(label), *seq.items[at:])
    return [a, b]


cuts = {1: 5, 3: 4}
for k, c in charts.items():
    if k in cuts:
        print(k, flowchart_cols(split(c, cuts[k]), f'{out}/flow{k}.png'))
    else:
        print(k, flowchart(c, f'{out}/flow{k}.png'))
