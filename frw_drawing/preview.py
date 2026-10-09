"""Рендер PNG-превью чертежа (для проверки без КОМПАС)."""
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Arc, Circle, PathPatch
from matplotlib.path import Path

from geometry import AXIAL, MAIN, build

LW = {MAIN: 1.6, 2: 0.5, AXIAL: 0.5}
LS = {MAIN: "-", 2: "-", AXIAL: (0, (12, 3, 2, 3))}


def contour_path(loop):
    import math
    verts = []
    for p in loop:
        if p[0] == "line":
            verts.append([(p[1], p[2]), (p[3], p[4])])
        elif p[0] == "circle":
            verts.append([(p[1] + p[3] * math.cos(t / 60 * math.pi), p[2] + p[3] * math.sin(t / 60 * math.pi))
                          for t in range(121)])
        else:
            _, xc, yc, r, a1, a2, _ = p
            span = (a2 - a1) % 360
            n = 60
            verts.append([(xc + r * math.cos(math.radians(a1 + span * i / n)),
                           yc + r * math.sin(math.radians(a1 + span * i / n))) for i in range(n + 1)])
    # склеиваем сегменты в замкнутый контур по ближайшим концам
    chain = verts.pop(0)
    while verts:
        end = chain[-1]
        best = min(range(len(verts)), key=lambda i: min(
            (verts[i][0][0] - end[0]) ** 2 + (verts[i][0][1] - end[1]) ** 2,
            (verts[i][-1][0] - end[0]) ** 2 + (verts[i][-1][1] - end[1]) ** 2))
        seg = verts.pop(best)
        if (seg[-1][0] - end[0]) ** 2 + (seg[-1][1] - end[1]) ** 2 < (seg[0][0] - end[0]) ** 2 + (seg[0][1] - end[1]) ** 2:
            seg = seg[::-1]
        chain += seg[1:]
    return chain


def main(prims, out):
    fig, ax = plt.subplots(figsize=(16, 9))
    for p in prims:
        k = p[0]
        if k == "line":
            ax.plot([p[1], p[3]], [p[2], p[4]], color="k", lw=LW[p[5]], ls=LS[p[5]])
        elif k == "circle":
            ax.add_patch(Circle((p[1], p[2]), p[3], fill=False, lw=LW[p[4]], ls=LS[p[4]]))
        elif k == "arc":
            ax.add_patch(Arc((p[1], p[2]), 2 * p[3], 2 * p[3], theta1=p[4], theta2=p[5],
                             lw=LW[p[6]], ls=LS[p[6]]))
        elif k == "hatch":
            verts, codes = [], []
            for loop in p[1]:
                pts = contour_path(loop)
                verts += pts
                codes += [Path.MOVETO] + [Path.LINETO] * (len(pts) - 1)
            ax.add_patch(PathPatch(Path(verts, codes), fill=False, hatch="////", lw=0))
    ax.set_aspect("equal")
    ax.autoscale_view()
    ax.margins(0.05)
    ax.axis("off")
    fig.savefig(out, dpi=110, bbox_inches="tight")


if __name__ == "__main__":
    main(build(), sys.argv[1] if len(sys.argv) > 1 else "preview.png")
