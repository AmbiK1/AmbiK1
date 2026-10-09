# -*- coding: utf-8 -*-
"""Макрос для КОМПАС-3D: строит чертёж «Вариант 1» (Гитара + Корпус) во фрагменте
и сохраняет его как Вариант1.frw.

Запуск:
  * в КОМПАС-3D: Приложения -> Python -> Запустить скрипт (или «Макросы» в старых версиях);
  * или снаружи, при установленном КОМПАС-3D:  pip install pywin32  и  python kompas_macro.py

Файл сгенерирован build.py из geometry.py -- правьте геометрию там.
"""
import os

import pythoncom
from win32com.client import Dispatch, gencache

OUT_NAME = "Вариант1.frw"

# (тип, ...); стили: 1 - основная, 2 - тонкая, 3 - осевая
PRIMS = [
    ('circle', 0.0, 0.0, 30.0, 1),
    ('circle', 0.0, 0.0, 14.0, 1),
    ('circle', 0.0, 0.0, 22.0, 3),
    ('circle', 22.0, 0.0, 4.0, 1),
    ('line', 28.5, 0.0, 15.5, 0.0, 3),
    ('line', 22.0, 6.5, 22.0, -6.5, 3),
    ('circle', 11.0, 19.0526, 4.0, 1),
    ('line', 14.25, 24.6817, 7.75, 13.4234, 3),
    ('line', 5.3708, 22.3026, 16.6292, 15.8026, 3),
    ('circle', -11.0, 19.0526, 4.0, 1),
    ('line', -14.25, 24.6817, -7.75, 13.4234, 3),
    ('line', -16.6292, 15.8026, -5.3708, 22.3026, 3),
    ('circle', -22.0, 0.0, 4.0, 1),
    ('line', -28.5, 0.0, -15.5, 0.0, 3),
    ('line', -22.0, -6.5, -22.0, 6.5, 3),
    ('circle', -11.0, -19.0526, 4.0, 1),
    ('line', -14.25, -24.6817, -7.75, -13.4234, 3),
    ('line', -5.3708, -22.3026, -16.6292, -15.8026, 3),
    ('circle', 11.0, -19.0526, 4.0, 1),
    ('line', 14.25, -24.6817, 7.75, -13.4234, 3),
    ('line', 16.6292, -15.8026, 5.3708, -22.3026, 3),
    ('arc', 0.0, 0.0, 94.0, 225.0, 300.0, 1),
    ('arc', 0.0, 0.0, 54.0, 225.0, 300.0, 1),
    ('arc', -52.3259, -52.3259, 20.0, 45.0, 225.0, 1),
    ('arc', 37.0, -64.0859, 20.0, 300.0, 480.0, 1),
    ('arc', 0.0, 0.0, 84.0, 225.0, 300.0, 1),
    ('arc', 0.0, 0.0, 64.0, 225.0, 300.0, 1),
    ('arc', -52.3259, -52.3259, 10.0, 45.0, 225.0, 1),
    ('arc', 37.0, -64.0859, 10.0, 300.0, 480.0, 1),
    ('arc', -54.5028, -7.3786, 25.0, 272.7728, 7.7098, 1),
    ('arc', -22.5569, -46.8528, 82.0, 343.862, 64.2919, 1),
    ('line', -40.0, 0.0, 40.0, 0.0, 3),
    ('line', 0.0, 40.0, 0.0, -102.0, 3),
    ('arc', 0.0, 0.0, 74.0, 221.0, 304.0, 3),
    ('line', -24.0416, -24.0416, -72.1249, -72.1249, 3),
    ('line', -42.4264, -62.2254, -62.2254, -42.4264, 3),
    ('line', 17.0, -29.4449, 51.0, -88.3346, 3),
    ('line', 49.1244, -57.0859, 24.8756, -71.0859, 3),
    ('line', 166.2595, 11.646, 219.3893, -62.531, 1),
    ('line', 131.604, -7.8478, 167.406, -91.7716, 1),
    ('arc', 150.0, 0.0, 20.0, 35.6124, 203.1031, 1),
    ('arc', 195.0, -80.0, 30.0, 203.1031, 35.6124, 1),
    ('circle', 150.0, 0.0, 10.0, 1),
    ('circle', 195.0, -80.0, 17.0, 1),
    ('hatch', [[('line', 166.2595, 11.646, 219.3893, -62.531, 1), ('line', 131.604, -7.8478, 167.406, -91.7716, 1), ('arc', 150.0, 0.0, 20.0, 35.6124, 203.1031, 1), ('arc', 195.0, -80.0, 30.0, 203.1031, 35.6124, 1)], [('circle', 150.0, 0.0, 10.0, 1)], [('circle', 195.0, -80.0, 17.0, 1)]]),
    ('circle', 215.0, 0.0, 27.0, 1),
    ('circle', 215.0, 0.0, 12.0, 1),
    ('arc', 187.5077, -33.0632, 70.0, 50.2562, 138.6037, 1),
    ('arc', 248.4582, -48.5958, 32.0, 124.5474, 210.4323, 1),
    ('line', 124.0, 0.0, 248.0, 0.0, 3),
    ('line', 150.0, 26.0, 150.0, -26.0, 3),
    ('line', 215.0, 33.0, 215.0, -33.0, 3),
    ('line', 231.0, -80.0, 159.0, -80.0, 3),
    ('line', 195.0, -44.0, 195.0, -116.0, 3),
]


def connect():
    api5 = gencache.EnsureModule("{0422828C-F174-495E-AC5D-D31014DBBE87}", 0, 1, 0)
    const = gencache.EnsureModule("{75C9F5D0-B5B8-4526-8681-9903C567D2ED}", 0, 1, 0).constants
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
