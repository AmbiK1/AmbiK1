"""Геометрия чертежа «Вариант 10»: деталь 1 «Серьга» и деталь 2 «Корпус».

Формат примитивов и стили линий -- см. geometry.py.
"""
from geometry import (AXIAL, MAIN, arc_between, ang, axis, center_cross, circle_intersections,
                      outer_tangents, polar, short_arc, towards)

EARRING_ORIGIN = (0.0, 0.0)     # центр диска Ф90 «Серьги»
BODY_ORIGIN = (140.0, -40.0)    # центр дугового паза «Корпуса»


# ----------------------------------------------------------------- Серьга
def earring(o=EARRING_ORIGIN):
    g = []
    # Диск Ф90, отверстие Ф32, 12 отв. Ф8 на Ф66
    g.append(("circle", o[0], o[1], 45.0, MAIN))
    g.append(("circle", o[0], o[1], 16.0, MAIN))
    g.append(("circle", o[0], o[1], 33.0, AXIAL))
    for k in range(12):
        h = polar(o, 33.0, 30.0 * k)
        g.append(("circle", h[0], h[1], 4.0, MAIN))
        g += center_cross(h, 6.5, 30.0 * k)

    # Ушко R13 с отверстием Ф10 на 55 выше центра, касательные к диску Ф90
    t = (o[0], o[1] + 55.0)
    (l1t, l1o), (l2t, l2o) = outer_tangents(t, 13.0, o, 45.0)
    g.append(("line", l1t[0], l1t[1], l1o[0], l1o[1], MAIN))
    g.append(("line", l2t[0], l2t[1], l2o[0], l2o[1], MAIN))
    g.append(arc_between(t, 13.0, l1t, l2t, through=90.0))
    g.append(("circle", t[0], t[1], 5.0, MAIN))

    # Дуговая планка: ось отверстий R76, отверстия через 30°, края R65 / R87, концы R11
    a_l, a_r = 240.0, 300.0
    hl, hr = polar(o, 76.0, a_l), polar(o, 76.0, a_r)
    g.append(("arc", o[0], o[1], 87.0, a_l, a_r, MAIN))
    g.append(("arc", o[0], o[1], 65.0, a_l, a_r, MAIN))
    g.append(("arc", hl[0], hl[1], 11.0, a_l - 180.0, a_l, MAIN))
    g.append(("arc", hr[0], hr[1], 11.0, a_r, a_r + 180.0, MAIN))
    for a in (a_l, 270.0, a_r):
        h = polar(o, 76.0, a)
        g.append(("circle", h[0], h[1], 6.0, MAIN))
        g += center_cross(h, 9.0, a)

    # Сопряжения R32: диск Ф90 и концы планки R11 (внешнее касание)
    for h, pick in ((hr, max), (hl, min)):
        f = pick(circle_intersections(o, 45.0 + 32.0, h, 11.0 + 32.0), key=lambda p: p[0])
        g.append(short_arc(f, 32.0, towards(f, o, 32.0), towards(f, h, 32.0)))

    # Осевые линии
    g.append(axis((o[0] - 52, o[1]), (o[0] + 52, o[1])))
    g.append(axis((o[0], t[1] + 18), (o[0], o[1] - 95)))
    g.append(axis((t[0] - 18, t[1]), (t[0] + 18, t[1])))
    g.append(("arc", o[0], o[1], 76.0, a_l - 6.0, a_r + 6.0, AXIAL))
    for a in (a_l, a_r):
        g.append(axis(polar(o, 20.0, a), polar(o, 95.0, a)))
    return g


# ----------------------------------------------------------------- Корпус
def body(d=BODY_ORIGIN):
    g = []
    e = (d[0], d[1] + 60.0)          # центр Ф20 / R18
    t = (d[0], d[1] + 92.0)          # центр Ф10 / R10
    sl, sr = (d[0] - 30.0, d[1]), (d[0] + 30.0, d[1])   # концы паза

    # Наружный контур: касательные от R10 к R48
    (l1t, l1d), (l2t, l2d) = outer_tangents(t, 10.0, d, 48.0)
    g.append(("line", l1t[0], l1t[1], l1d[0], l1d[1], MAIN))
    g.append(("line", l2t[0], l2t[1], l2d[0], l2d[1], MAIN))
    g.append(arc_between(d, 48.0, l1d, l2d, through=270.0))

    # Перемычка R8 между ушком R10 и бобышкой R18
    fl, fr = sorted(circle_intersections(t, 10.0 + 8.0, e, 18.0 + 8.0), key=lambda p: p[0])
    g.append(short_arc(fl, 8.0, towards(fl, t, 8.0), towards(fl, e, 8.0)))
    g.append(short_arc(fr, 8.0, towards(fr, t, 8.0), towards(fr, e, 8.0)))
    g.append(arc_between(t, 10.0, towards(t, fl, 10.0), towards(t, fr, 10.0), through=90.0))
    g.append(("circle", t[0], t[1], 5.0, MAIN))

    # Заштрихованная часть: бобышка R18, сопряжения R32, концы R18, дуга R48
    cl = min(circle_intersections(e, 18.0 + 32.0, sl, 18.0 + 32.0), key=lambda p: p[0])
    cr = max(circle_intersections(e, 18.0 + 32.0, sr, 18.0 + 32.0), key=lambda p: p[0])
    contour = [
        ("arc", d[0], d[1], 48.0, 180.0, 360.0, MAIN),
        ("arc", sr[0], sr[1], 18.0, 0.0, ang(sr, cr), MAIN),
        short_arc(cr, 32.0, towards(cr, sr, 32.0), towards(cr, e, 32.0)),
        arc_between(e, 18.0, towards(e, cr, 18.0), towards(e, cl, 18.0), through=90.0),
        short_arc(cl, 32.0, towards(cl, e, 32.0), towards(cl, sl, 32.0)),
        ("arc", sl[0], sl[1], 18.0, ang(sl, cl), 180.0, MAIN),
    ]
    g += contour[1:]   # нижняя дуга R48 уже входит в наружный контур
    # Дуговой паз шириной 18 (R9) по оси R30
    slot = [
        ("arc", d[0], d[1], 39.0, 180.0, 360.0, MAIN),
        ("arc", d[0], d[1], 21.0, 180.0, 360.0, MAIN),
        ("arc", sl[0], sl[1], 9.0, 0.0, 180.0, MAIN),
        ("arc", sr[0], sr[1], 9.0, 0.0, 180.0, MAIN),
    ]
    g += slot
    g.append(("circle", e[0], e[1], 10.0, MAIN))
    g.append(("hatch", [contour, slot, [("circle", e[0], e[1], 10.0, MAIN)]]))

    # Осевые линии
    g.append(axis((d[0], t[1] + 15), (d[0], d[1] - 55)))
    g.append(axis((d[0] - 55, d[1]), (d[0] + 55, d[1])))
    g.append(axis((e[0] - 24, e[1]), (e[0] + 24, e[1])))
    g.append(axis((t[0] - 15, t[1]), (t[0] + 15, t[1])))
    g.append(("arc", d[0], d[1], 30.0, 180.0, 360.0, AXIAL))
    for s in (sl, sr):
        g.append(axis((s[0], s[1] + 13), (s[0], s[1] - 13)))
    return g


def build():
    return earring() + body()
