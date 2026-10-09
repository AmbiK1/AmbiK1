import re
from PIL import Image, ImageDraw, ImageFont

MONO = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf'
SERIF = '/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf'

# ---------------------------------------------------------------- code image
C_BG = (30, 30, 30)
C_LN = (43, 145, 175)
C_SEP = (64, 64, 64)
C_TXT = (212, 212, 212)
C_KW = (86, 156, 214)
C_STR = (206, 145, 120)
C_NUM = (181, 206, 168)
C_FN = (220, 220, 170)
C_NS = (78, 201, 176)
C_PP = (155, 155, 155)
C_CTL = (197, 134, 192)

KW = {'int', 'double', 'return', 'new', 'delete', 'void', 'char', 'bool', 'float', 'long'}
CTL = set()
KW_BLUE_CTL = {'if', 'else', 'for', 'while'}


def tokenize(line):
    if line.lstrip().startswith('#include'):
        i = line.index('#include')
        rest = line[i + 8:]
        return [(line[:i], C_TXT), ('#include', C_PP), (rest, C_STR)]
    toks = []
    pat = re.compile(r'("(?:\\.|[^"\\])*")|(\d+(?:\.\d+)?)|([A-Za-z_]\w*)|(\s+)|(.)')
    words = list(pat.finditer(line))
    for k, m in enumerate(words):
        s, num, ident, ws, other = m.groups()
        if s:
            toks.append((s, C_STR))
        elif num:
            toks.append((num, C_NUM))
        elif ident:
            nxt = line[m.end():m.end() + 1]
            if ident in KW or ident in KW_BLUE_CTL:
                toks.append((ident, C_KW))
            elif ident == 'std':
                toks.append((ident, C_NS))
            elif nxt == '(':
                toks.append((ident, C_FN))
            else:
                toks.append((ident, C_TXT))
        else:
            toks.append((ws or other, C_TXT))
    return toks


def code_image(src, path, fs=26):
    font = ImageFont.truetype(MONO, fs)
    lines = src.rstrip('\n').split('\n')
    cw = font.getlength('M')
    lh = int(fs * 1.35)
    gutter = int(cw * 3.2)
    maxlen = max(len(l) for l in lines)
    W = int(gutter + 16 + cw * maxlen + 40)
    H = lh * len(lines) + 24
    img = Image.new('RGB', (W, H), C_BG)
    d = ImageDraw.Draw(img)
    d.line([(gutter + 4, 0), (gutter + 4, H)], fill=C_SEP, width=2)
    for n, l in enumerate(lines):
        y = 12 + n * lh
        num = str(n + 1)
        d.text((gutter - 10 - font.getlength(num), y), num, font=font, fill=C_LN)
        x = gutter + 18
        for t, c in tokenize(l):
            d.text((x, y), t, font=font, fill=c)
            x += font.getlength(t)
    img.save(path)
    return img.size


# ------------------------------------------------------------- console image
def console_image(text, path, fs=22, width=752):
    font = ImageFont.truetype(MONO, fs)
    lines = text.split('\n')
    lh = int(fs * 1.55)
    W = max(width, int(max(font.getlength(l) for l in lines)) + 40)
    H = lh * len(lines) + 28
    img = Image.new('RGB', (W, H), (12, 12, 12))
    d = ImageDraw.Draw(img)
    for n, l in enumerate(lines):
        d.text((16, 14 + n * lh), l, font=font, fill=(204, 204, 204))
    img.save(path)
    return img.size


# --------------------------------------------------------------- flowcharts
LW = 3
BW, BH = 380, 110      # block size
GAP = 72               # vertical gap between blocks
MARG = 45              # side margin for bypass lines
FS = 34
font = ImageFont.truetype(SERIF, FS)
small = ImageFont.truetype(SERIF, 32)
INK = (0, 0, 0)


def text_w(t, f=font):
    return max(f.getlength(s) for s in t.split('\n'))


class Node:
    pass


class Block(Node):
    def __init__(self, kind, text, w=None, h=None):
        self.kind, self.text = kind, text
        nl = text.count('\n') + 1
        tw = text_w(text)
        extra = {'io': 120, 'decision': 0, 'loop': 110, 'term': 80, 'process': 60}[kind]
        self.w = w or max(BW, int(tw + extra))
        if kind == 'decision':
            self.w = w or max(BW + 20, int(tw * 1.75 + 40))
        self.h = h or max(BH, int(nl * FS * 1.2 + 50))
        if kind == 'decision':
            self.h = h or max(150, int(nl * FS * 1.2 + 90))
        if kind == 'term':
            self.w = w or 280
            self.h = 92

    def size(self):
        return self.w / 2, self.w / 2, self.h

    def draw(self, d, cx, y):
        w, h = self.w, self.h
        x0, x1, y1 = cx - w / 2, cx + w / 2, y + h
        k = self.kind
        if k == 'process':
            d.rectangle([x0, y, x1, y1], outline=INK, width=LW)
        elif k == 'term':
            d.rounded_rectangle([x0, y, x1, y1], radius=h / 2, outline=INK, width=LW)
        elif k == 'io':
            s = 50
            d.polygon([(x0 + s, y), (x1, y), (x1 - s, y1), (x0, y1)], outline=INK, width=LW)
        elif k == 'decision':
            d.polygon([(cx, y), (x1, y + h / 2), (cx, y1), (x0, y + h / 2)], outline=INK, width=LW)
        elif k == 'loop':
            s = 55
            d.polygon([(x0 + s, y), (x1 - s, y), (x1, y + h / 2), (x1 - s, y1), (x0 + s, y1), (x0, y + h / 2)],
                      outline=INK, width=LW)
        d.multiline_text((cx, y + h / 2), self.text, font=font, fill=INK, anchor='mm', align='center',
                         spacing=8)


def arrow_down(d, x, y0, y1):
    d.line([(x, y0), (x, y1 - 2)], fill=INK, width=LW)
    d.polygon([(x - 8, y1 - 18), (x + 8, y1 - 18), (x, y1)], fill=INK)


def arrow_right(d, x0, x1, y):
    d.line([(x0, y), (x1 - 2, y)], fill=INK, width=LW)
    d.polygon([(x1 - 18, y - 8), (x1 - 18, y + 8), (x1, y)], fill=INK)


def line(d, pts):
    d.line(pts, fill=INK, width=LW, joint='curve')


class Seq(Node):
    def __init__(self, *items):
        self.items = list(items)

    def size(self):
        l = r = 0
        h = 0
        for i, it in enumerate(self.items):
            a, b, c = it.size()
            l, r = max(l, a), max(r, b)
            h += c + (GAP if i else 0)
        return l, r, h

    def draw(self, d, cx, y):
        for i, it in enumerate(self.items):
            if i:
                arrow_down(d, cx, y, y + GAP)
                y += GAP
            it.draw(d, cx, y)
            y += it.size()[2]


class If(Node):
    """Decision; 'да' goes down (then), 'нет' goes right (bypass or else)."""

    def __init__(self, cond, then, other=None):
        self.dec = Block('decision', cond)
        self.then = then if isinstance(then, Seq) else Seq(then)
        self.other = other if (other is None or isinstance(other, Seq)) else Seq(other)

    def geom(self):
        tl, tr, th = self.then.size()
        dl = self.dec.w / 2
        if self.other is None:
            rx = max(dl, tr) + MARG
            return dict(tl=tl, tr=tr, th=th, rx=rx)
        ol, orr, oh = self.other.size()
        ox = max(dl + 40, tr + MARG + ol)   # center of 'нет' branch
        return dict(tl=tl, tr=tr, th=th, ol=ol, orr=orr, oh=oh, ox=ox)

    def size(self):
        g = self.geom()
        dh = self.dec.h
        if self.other is None:
            return max(self.dec.w / 2, g['tl']), g['rx'] + 60, dh + GAP + g['th'] + GAP * 0.75
        bh = max(g['th'], g['oh'])
        return max(self.dec.w / 2, g['tl']), g['ox'] + g['orr'], dh + GAP + bh + GAP * 0.75

    def draw(self, d, cx, y):
        g = self.geom()
        dec = self.dec
        dec.draw(d, cx, y)
        my = y + dec.h / 2
        ty = y + dec.h + GAP
        arrow_down(d, cx, y + dec.h, ty)
        d.text((cx + 18, y + dec.h + 8), 'да', font=small, fill=INK, anchor='lt')
        self.then.draw(d, cx, ty)
        if self.other is None:
            jy = ty + g['th'] + GAP * 0.75
            rx = cx + g['rx']
            d.text((cx + dec.w / 2 + 40, my - 12), 'нет', font=small, fill=INK, anchor='lb')
            line(d, [(cx + dec.w / 2, my), (rx, my), (rx, jy), (cx, jy)])
            line(d, [(cx, ty + g['th']), (cx, jy)])
        else:
            bh = max(g['th'], g['oh'])
            jy = ty + bh + GAP * 0.75
            ox = cx + g['ox']
            d.text((cx + dec.w / 2 + 14, my - 12), 'нет', font=small, fill=INK, anchor='lb')
            line(d, [(cx + dec.w / 2, my), (ox, my)])
            arrow_down(d, ox, my, ty)
            self.other.draw(d, ox, ty)
            line(d, [(ox, ty + g['oh']), (ox, jy), (cx, jy)])
            line(d, [(cx, ty + g['th']), (cx, jy)])


class For(Node):
    def __init__(self, header, body):
        self.hdr = Block('loop', header)
        self.body = body if isinstance(body, Seq) else Seq(body)

    def geom(self):
        bl, br, bh = self.body.size()
        lx = max(self.hdr.w / 2, bl) + MARG
        rx = max(self.hdr.w / 2, br) + MARG
        return bl, br, bh, lx, rx

    def size(self):
        bl, br, bh, lx, rx = self.geom()
        return lx + 4, rx + 4, self.hdr.h + GAP + bh + GAP * 0.6 + GAP * 0.9

    def draw(self, d, cx, y):
        bl, br, bh, lx, rx = self.geom()
        h = self.hdr
        h.draw(d, cx, y)
        my = y + h.h / 2
        by = y + h.h + GAP
        arrow_down(d, cx, y + h.h, by)
        self.body.draw(d, cx, by)
        ry = by + bh + GAP * 0.6
        line(d, [(cx, by + bh), (cx, ry), (cx - lx, ry), (cx - lx, my)])
        arrow_right(d, cx - lx, cx - h.w / 2, my)
        ey = ry + GAP * 0.9
        line(d, [(cx + h.w / 2, my), (cx + rx, my), (cx + rx, ey), (cx, ey)])


def flowchart(root, path, pad=24):
    l, r, h = root.size()
    W, H = int(l + r + 2 * pad), int(h + 2 * pad)
    img = Image.new('RGB', (W, H), 'white')
    d = ImageDraw.Draw(img)
    root.draw(d, pad + l, pad)
    img.save(path)
    return img.size
