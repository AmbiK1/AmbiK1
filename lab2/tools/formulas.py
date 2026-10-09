import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
plt.rcParams['mathtext.fontset'] = 'stix'
F = {'formula1': r'$z=\dfrac{|x|-|y|}{1+|xy|}$',
     'formula5': r'$s=\sum_{i=1}^{N}\ \prod_{j=1}^{i}\ \dfrac{j!}{i!}$'}
for name, tex in F.items():
    fig = plt.figure(figsize=(0.01, 0.01))
    fig.text(0, 0, tex, fontsize=20)
    fig.savefig(f'../img/{name}.png', dpi=300, bbox_inches='tight', pad_inches=0.04, facecolor='white')
