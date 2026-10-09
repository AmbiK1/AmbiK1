"""Геометрия чертежа «Вариант 1»: деталь 1 «Гитара» и деталь 2 «Корпус».

Все сопряжения вычисляются точно по размерам с исходного задания.
Примитивы (координаты в мм, углы в градусах, дуги строятся против часовой стрелки):
    ("line",   x1, y1, x2, y2, style)
    ("circle", xc, yc, r, style)
    ("arc",    xc, yc, r, a1, a2, style)
    ("hatch",  [контуры])  -- контур = список примитивов line/arc/circle
Стили линий как в КОМПАС-3D: 1 - основная, 2 - тонкая, 3 - осевая.
"""
import math

MAIN, THIN, AXIAL = 1, 2, 3

# Положение деталей на листе фрагмента
GUITAR_ORIGIN = (0.0, 0.0)      # центр втулки Ф60 «Гитары»
BODY_ORIGIN = (150.0, 0.0)      # центр отверстия Ф20 «Корпуса»


def polar(c, r, a):
    a = math.radians(a)
    return (c[0] + r * math.cos(a), c[1] + r * math.sin(a))


def dist(p, q):
    return math.hypot(q[0] - p[0], q[1] - p[1])


def ang(c, p):
    return math.degrees(math.atan2(p[1] - c[1], p[0] - c[0])) % 360.0


def towards(c, p, r):
    """Точка на расстоянии r от c в направлении p."""
    d = dist(c, p)
    return (c[0] + (p[0] - c[0]) * r / d, c[1] + (p[1] - c[1]) * r / d)


def circle_intersections(c1, r1, c2, r2):
    d = dist(c1, c2)
    a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r1 * r1 - a * a)
    mx = c1[0] + a * (c2[0] - c1[0]) / d
    my = c1[1] + a * (c2[1] - c1[1]) / d
    ox = h * (c2[1] - c1[1]) / d
    oy = h * (c2[0] - c1[0]) / d
    return (mx + ox, my - oy), (mx - ox, my + oy)


def ccw_span(a1, a2):
    return (a2 - a1) % 360.0


def arc_between(c, r, p1, p2, through, style=MAIN):
    """Дуга окружности (c, r) между точками p1 и p2, проходящая через направление through."""
    a1, a2 = ang(c, p1), ang(c, p2)
    if ccw_span(a1, through % 360.0) <= ccw_span(a1, a2):
        return ("arc", c[0], c[1], r, a1, a2, style)
    return ("arc", c[0], c[1], r, a2, a1, style)


def short_arc(c, r, p1, p2, style=MAIN):
    a1, a2 = ang(c, p1), ang(c, p2)
    if ccw_span(a1, a2) <= 180.0:
        return ("arc", c[0], c[1], r, a1, a2, style)
    return ("arc", c[0], c[1], r, a2, a1, style)


def axis(p, q):
    return ("line", p[0], p[1], q[0], q[1], AXIAL)


def center_cross(c, half, rot=0.0):
    return [axis(polar(c, half, rot), polar(c, half, rot + 180)),
            axis(polar(c, half, rot + 90), polar(c, half, rot + 270))]


def outer_tangents(c1, r1, c2, r2):
    """Две внешние касательные к окружностям: [((p1, p2)), ...]."""
    th = math.atan2(c2[1] - c1[1], c2[0] - c1[0])
    al = math.acos((r1 - r2) / dist(c1, c2))
    res = []
    for s in (1, -1):
        n = math.degrees(th + s * al)
        res.append((polar(c1, r1, n), polar(c2, r2, n)))
    return res


# ----------------------------------------------------------------- Гитара
def guitar(o=GUITAR_ORIGIN):
    g = []
    # Втулка Ф60, отверстие Ф28, 6 отв. Ф8 на Ф44
    g.append(("circle", o[0], o[1], 30.0, MAIN))
    g.append(("circle", o[0], o[1], 14.0, MAIN))
    g.append(("circle", o[0], o[1], 22.0, AXIAL))
    for k in range(6):
        h = polar(o, 22.0, 60.0 * k)
        g.append(("circle", h[0], h[1], 4.0, MAIN))
        g += center_cross(h, 6.5, 60.0 * k)

    # Дуговой паз: ось R74 от 45° влево до 30° вправо от вертикали
    a_l, a_r = 270.0 - 45.0, 270.0 + 30.0
    c1, c2 = polar(o, 74.0, a_l), polar(o, 74.0, a_r)
    # Наружный контур полосы R54 / R94 со скруглениями R20
    g.append(("arc", o[0], o[1], 94.0, a_l, a_r, MAIN))
    g.append(("arc", o[0], o[1], 54.0, a_l, a_r, MAIN))
    g.append(("arc", c1[0], c1[1], 20.0, a_l - 180.0, a_l, MAIN))
    g.append(("arc", c2[0], c2[1], 20.0, a_r, a_r + 180.0, MAIN))
    # Прорезь шириной 20 (R10)
    g.append(("arc", o[0], o[1], 84.0, a_l, a_r, MAIN))
    g.append(("arc", o[0], o[1], 64.0, a_l, a_r, MAIN))
    g.append(("arc", c1[0], c1[1], 10.0, a_l - 180.0, a_l, MAIN))
    g.append(("arc", c2[0], c2[1], 10.0, a_r, a_r + 180.0, MAIN))

    # Сопряжение R25: втулка Ф60 и скругление R20 левого конца (внешнее касание)
    f = min(circle_intersections(o, 30.0 + 25.0, c1, 20.0 + 25.0), key=lambda p: ang(o, p))
    g.append(short_arc(f, 25.0, towards(f, o, 25.0), towards(f, c1, 25.0)))

    # Дуга R82: касается втулки Ф60 и скругления R20 правого конца (внутреннее касание)
    q = min(circle_intersections(o, 82.0 - 30.0, c2, 82.0 - 20.0), key=lambda p: p[0])
    g.append(arc_between(q, 82.0, towards(q, o, 82.0), towards(q, c2, 82.0), through=0.0))

    # Осевые линии
    g.append(axis((o[0] - 40, o[1]), (o[0] + 40, o[1])))
    g.append(axis((o[0], o[1] + 40), (o[0], o[1] - 102)))
    g.append(("arc", o[0], o[1], 74.0, a_l - 4.0, a_r + 4.0, AXIAL))
    for a, c in ((a_l, c1), (a_r, c2)):
        g.append(axis(polar(o, 34.0, a), polar(o, 102.0, a)))
        g.append(axis(polar(c, 14.0, a + 90), polar(c, 14.0, a - 90)))
    return g


# ----------------------------------------------------------------- Корпус
def body(a=BODY_ORIGIN):
    g = []
    b = (a[0] + 65.0, a[1])          # центр Ф54 / Ф24
    c = (a[0] + 45.0, a[1] - 80.0)   # центр Ф34 / R30

    # Рычаг R20 - R30 с касательными
    (t1a, t1c), (t2a, t2c) = outer_tangents(a, 20.0, c, 30.0)
    away_a, away_c = ang(c, a), ang(a, c)
    link = [
        ("line", t1a[0], t1a[1], t1c[0], t1c[1], MAIN),
        ("line", t2a[0], t2a[1], t2c[0], t2c[1], MAIN),
        arc_between(a, 20.0, t1a, t2a, through=away_a),
        arc_between(c, 30.0, t1c, t2c, through=away_c),
    ]
    g += link
    g.append(("circle", a[0], a[1], 10.0, MAIN))
    g.append(("circle", c[0], c[1], 17.0, MAIN))
    g.append(("hatch", [link,
                        [("circle", a[0], a[1], 10.0, MAIN)],
                        [("circle", c[0], c[1], 17.0, MAIN)]]))

    # Бобышка Ф54 с отверстием Ф24
    g.append(("circle", b[0], b[1], 27.0, MAIN))
    g.append(("circle", b[0], b[1], 12.0, MAIN))

    # Дуга R70: касается R20 и Ф54 (внутреннее касание), центр ниже оси
    p = min(circle_intersections(a, 70.0 - 20.0, b, 70.0 - 27.0), key=lambda t: t[1])
    g.append(arc_between(p, 70.0, towards(p, a, 70.0), towards(p, b, 70.0), through=90.0))

    # Сопряжение R32: Ф54 и R30 (внешнее касание), центр справа
    s = max(circle_intersections(b, 27.0 + 32.0, c, 30.0 + 32.0), key=lambda t: t[0])
    g.append(short_arc(s, 32.0, towards(s, b, 32.0), towards(s, c, 32.0)))

    # Осевые линии
    g.append(axis((a[0] - 26, a[1]), (b[0] + 33, b[1])))
    g.append(axis((a[0], a[1] + 26), (a[0], a[1] - 26)))
    g.append(axis((b[0], b[1] + 33), (b[0], b[1] - 33)))
    g += center_cross(c, 36.0)
    return g


def build():
    return guitar() + body()
