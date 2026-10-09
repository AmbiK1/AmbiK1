import json
from imgs import code_image, console_image
from runpty import run
SRC='/home/user/AmbiK1/lab2/src'; OUT='../img'
runs = {1: [['3','2'],['-4','1.5'],['0','5']],
        2: [['2 4'],['-3 3']],
        3: [['0.3 0.4'],['-0.5 0.6'],['0.5 -0.7'],['0.9 0.9']],
        4: [['6','3 -5 7 0 -2 4.5'],['4','-1 -2.5 -3 -4']],
        5: [['1'],['3'],['5']]}
for t in range(1,6):
    print(t, 'code', code_image(open(f'{SRC}/task{t}.cpp').read(), f'{OUT}/code{t}.png'))
    for k, inp in enumerate(runs[t]):
        txt = run(f'{SRC}/task{t}', inp)
        print(t, k, console_image(txt, f'{OUT}/out{t}_{k+1}.png'))
