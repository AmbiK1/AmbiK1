from imgs import code_image, console_image
from runpty import run
SRC = '/home/user/AmbiK1/lab3/src'; OUT = '../img3'
runs = {1: [['3 4', '2 5 -1 0', '-3 7 4 -6', '1 -2 8 0'], ['2 3', '1 2 3', '4 5 6'], ['2 2', '-1 -2', '0 -4']],
        2: [['5', '1 2 3 4 5'], ['3', '7 -2 9']],
        3: [['3 4', '2 -9 -3 -1', '-5 4 -7 6', '1 -3 8 -2']],
        4: [['I like big red apples and green trees', '3'], ['The cat and the dog play in the old park', '3']],
        5: [['Programming is fun'], ['I see a queue'], ['Data base']]}
for t in range(1, 6):
    print(t, 'code', code_image(open(f'{SRC}/task{t}.cpp').read(), f'{OUT}/code{t}.png'))
    for k, inp in enumerate(runs[t]):
        txt = run(f'{SRC}/task{t}', inp)
        print(txt); print(t, k, console_image(txt, f'{OUT}/out{t}_{k+1}.png'))
