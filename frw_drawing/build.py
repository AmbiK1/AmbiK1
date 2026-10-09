"""Собирает из geometry.py: DXF для КОМПАС, макрос КОМПАС-3D (создаёт .frw) и PNG-превью.

    python build.py
"""
import math
import os

import ezdxf

from geometry import AXIAL, MAIN, THIN, build

HERE = os.path.dirname(os.path.abspath(__file__))
NAME = "Вариант1"


def rounded(p):
    if p[0] == "hatch":
        return ("hatch", [[rounded(e) for e in loop] for loop in p[1]])
    return tuple(round(v, 4) if isinstance(v, float) else v for v in p)


# ------------------------------------------------------------------ DXF
def ends(p):
    if p[0] == "line":
        return (p[1], p[2]), (p[3], p[4])
    _, xc, yc, r, a1, a2, _ = p
    return ((xc + r * math.cos(math.radians(a1)), yc + r * math.sin(math.radians(a1))),
            (xc + r * math.cos(math.radians(a2)), yc + r * math.sin(math.radians(a2))))


def bulge_loop(loop):
    """Замкнутый контур из отрезков/дуг -> вершины полилинии (x, y, bulge)."""
    if loop[0][0] == "circle":
        _, xc, yc, r, _ = loop[0]
        return [(xc + r, yc, 1.0), (xc - r, yc, 1.0)]
    rest = list(loop)
    first = rest.pop(0)
    s, e = ends(first)
    chain = [(first, False)]
    cur = e
    while rest:
        i, rev = min(((i, rv) for i in range(len(rest)) for rv in (False, True)),
                     key=lambda t: math.dist(cur, ends(rest[t[0]])[1 if t[1] else 0]))
        el = rest.pop(i)
        chain.append((el, rev))
        cur = ends(el)[0 if rev else 1]
    verts = []
    for el, rev in chain:
        start = ends(el)[1 if rev else 0]
        b = 0.0
        if el[0] == "arc":
            b = math.tan(math.radians((el[5] - el[4]) % 360) / 4) * (-1 if rev else 1)
        verts.append((start[0], start[1], b))
    return verts


def write_dxf(prims, path):
    doc = ezdxf.new("R2010", setup=True)
    doc.header["$INSUNITS"] = 4
    doc.header["$LTSCALE"] = 0.3
    layers = {MAIN: "Основная", THIN: "Тонкая", AXIAL: "Осевая"}
    doc.layers.add(layers[MAIN], lineweight=50)
    doc.layers.add(layers[THIN], lineweight=25)
    doc.layers.add(layers[AXIAL], lineweight=25, linetype="CENTER")
    doc.layers.add("Штриховка", lineweight=25)
    msp = doc.modelspace()
    for p in prims:
        k = p[0]
        if k == "line":
            msp.add_line((p[1], p[2]), (p[3], p[4]), dxfattribs={"layer": layers[p[5]]})
        elif k == "circle":
            msp.add_circle((p[1], p[2]), p[3], dxfattribs={"layer": layers[p[4]]})
        elif k == "arc":
            msp.add_arc((p[1], p[2]), p[3], p[4], p[5], dxfattribs={"layer": layers[p[6]]})
        elif k == "hatch":
            h = msp.add_hatch(dxfattribs={"layer": "Штриховка"})
            h.set_pattern_fill("ANSI31", scale=0.6)
            for loop in p[1]:
                h.paths.add_polyline_path([(x, y, b) for x, y, b in bulge_loop(loop)], is_closed=True)
    doc.saveas(path)


# ------------------------------------------------------- макрос КОМПАС-3D
MACRO = '''# -*- coding: utf-8 -*-
"""Макрос для КОМПАС-3D: строит чертёж «Вариант 1» (Гитара + Корпус) во фрагменте
и сохраняет его как {name}.frw.

Запуск:
  * в КОМПАС-3D: Приложения -> Python -> Запустить скрипт (или «Макросы» в старых версиях);
  * или снаружи, при установленном КОМПАС-3D:  pip install pywin32  и  python {script}

Файл сгенерирован build.py из geometry.py -- правьте геометрию там.
"""
import os

import pythoncom
from win32com.client import Dispatch, gencache

OUT_NAME = "{name}.frw"

# (тип, ...); стили: 1 - основная, 2 - тонкая, 3 - осевая
PRIMS = {prims}


def connect():
    api5 = gencache.EnsureModule("{{0422828C-F174-495E-AC5D-D31014DBBE87}}", 0, 1, 0)
    const = gencache.EnsureModule("{{75C9F5D0-B5B8-4526-8681-9903C567D2ED}}", 0, 1, 0).constants
    app = Dispatch("Kompas.Application.5")
    kompas = api5.KompasObject(app._oleobj_.QueryInterface(api5.KompasObject.CLSID, pythoncom.IID_IDispatch))
    return api5, const, kompas


def draw(doc, p):
    kind = p[0]
    if kind == "line":
        doc.ksLineSeg(p[1], p[2], p[3], p[4], p[5])
    elif kind == "circle":
        doc.ksCircle(p[1], p[2], p[3], p[4])
    elif kind == "arc":
        doc.ksArcByAngle(p[1], p[2], p[3], p[4], p[5], 1, p[6])
    elif kind == "hatch":
        # штриховка «Металл», 45 град., шаг 2 мм; контуры внутри ksHatch задают её границы
        doc.ksHatch(0, 45.0, 2.0, 0.0, 0.0, 0.0)
        for loop in p[1]:
            for e in loop:
                draw(doc, e)
        doc.ksEndObj()


def out_dir():
    try:
        return os.path.dirname(os.path.abspath(__file__))
    except NameError:
        return os.path.join(os.path.expanduser("~"), "Documents")


def main():
    api5, const, kompas = connect()
    kompas.Visible = True
    doc = kompas.Document2D()
    param = api5.ksDocumentParam(kompas.GetParamStruct(const.ko_DocumentParam))
    param.Init()
    param.type = const.lt_DocFragment
    doc.ksCreateDocument(param)
    for p in PRIMS:
        draw(doc, p)
    path = os.path.join(out_dir(), OUT_NAME)
    if doc.ksSaveDocument(path):
        kompas.ksMessage("Фрагмент сохранён: " + path)
    else:
        kompas.ksMessage("Чертёж построен, но сохранить не удалось -- сохраните вручную (Файл -> Сохранить как .frw)")


main()
'''


def write_macro(prims, path):
    body = "[\n" + "".join("    %r,\n" % (rounded(p),) for p in prims) + "]"
    with open(path, "w", encoding="utf-8") as f:
        f.write(MACRO.format(name=NAME, script=os.path.basename(path), prims=body))


if __name__ == "__main__":
    prims = build()
    write_dxf(prims, os.path.join(HERE, NAME + ".dxf"))
    write_macro(prims, os.path.join(HERE, "kompas_macro.py"))
    import preview
    preview.main(os.path.join(HERE, "preview.png"))
    print("ok")
