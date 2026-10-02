"""Parametric flat-illustration portraits for the case-board polaroids.

Each portrait is drawn in a 100 x 100 viewport (the square photo window of a polaroid). Light
comes from the upper right, so the left side of each face carries the shadow.
"""
from __future__ import annotations

from dataclasses import dataclass, field

import math

from vec import G, P, Vec, circle, ellipse, linear, poly, radial, rect, rot_pts, shade, smooth

CX = 50.0


@dataclass
class Spec:
    name: str
    bg: str
    skin: str
    hair: str
    hair_style: str
    iris: str = "#3B2A20"
    lips: str = "#B5675E"
    clothes: str = "#555555"
    clothes2: str = "#333333"
    collar: str = "crew"
    jaw: float = 1.0  # lower-face width multiplier
    face_len: float = 1.0  # chin drop multiplier
    tilt: float = 0.0  # head rotation in degrees
    brow_weight: float = 1.0
    extras: list = field(default_factory=list)  # "glasses", "hoops", "stud", "freckles", "stubble", "blush", "lines"
    mouth_w: float = 1.0


# ------------------------------------------------------------------ face geometry

def face_path(s: Spec) -> str:
    j, L = s.jaw, s.face_len
    right = [
        (CX, 20.5),
        (58.6, 21.9),
        (64.8, 27.0),
        (67.3, 35.2),
        (67.2, 44.5),
        (65.6 - (1 - j) * 5, 53.0 + (L - 1) * 2),
        (61.8 - (1 - j) * 7, 61.2 + (L - 1) * 5),
        (56.2 - (1 - j) * 3, 67.4 + (L - 1) * 7),
    ]
    chin = (CX, 70.0 + (L - 1) * 8)
    left = [(2 * CX - x, y) for x, y in reversed(right[1:])]
    return smooth(right + [chin] + left, closed=True, tension=1.0)


def chin_y(s: Spec) -> float:
    return 70.0 + (s.face_len - 1) * 8


def ear(s: Spec, side: int):
    x = CX + side * 16.8
    shade_c = shade(s.skin, -0.18)
    return [
        P(ellipse(x, 46.5, 3.0, 5.4), s.skin),
        P(ellipse(x + side * 0.6, 46.8, 1.5, 3.2), shade_c, alpha=0.8),
    ]


def eye(s: Spec, side: int):
    ex, ey = CX + side * 8.2, 44.6
    w, top, bot = 4.4, 2.75, 1.65
    inner, outer = ex - side * w, ex + side * w
    white = (
        f"M{inner},{ey} C{inner + side * 1.2},{ey - top} {outer - side * 1.6},{ey - top - 0.3} {outer},{ey - 0.4} "
        f"C{outer - side * 1.4},{ey + bot} {inner + side * 1.2},{ey + bot + 0.2} {inner},{ey}z"
    )
    lid = (
        f"M{inner - side * 0.3},{ey + 0.1} C{inner + side * 1.2},{ey - top - 0.2} "
        f"{outer - side * 1.6},{ey - top - 0.5} {outer + side * 0.6},{ey - 0.9}"
    )
    lid_c = shade(s.skin, -0.62)
    return [
        P(white, "#F3EEE8"),
        G([
            P(circle(ex + side * 0.15, ey - 0.2, 2.25), s.iris),
            P(circle(ex + side * 0.15, ey - 0.2, 2.25), None, stroke=shade(s.iris, -0.45), sw=0.45),
            P(circle(ex + side * 0.15, ey - 0.2, 0.95), "#141011"),
            P(circle(ex + side * 0.15 + 0.7, ey - 0.95, 0.5), "#FFFFFF", alpha=0.9),
        ], clip=white),
        P(lid, None, stroke=lid_c, sw=0.95),
        # soft crease above the lid
        P(
            f"M{inner + side * 0.8},{ey - top - 1.2} C{ex},{ey - top - 2.4} {outer - side * 0.6},{ey - top - 1.9} {outer + side * 0.2},{ey - top - 0.6}",
            None, stroke=shade(s.skin, -0.25), sw=0.5, salpha=0.7,
        ),
    ]


def brow(s: Spec, side: int):
    bx, by = CX + side * 8.4, 38.9
    c = shade(s.hair, -0.25) if s.hair_style not in ("gray_bun",) else shade(s.hair, -0.35)
    if s.hair_style == "blond_messy":
        c = shade(s.hair, -0.35)
    w = 1.55 * s.brow_weight
    d = (
        f"M{bx - side * 4.6},{by + 1.0} C{bx - side * 2.2},{by - 0.9} {bx + side * 1.4},{by - 1.2} {bx + side * 4.4},{by + 0.4}"
    )
    return [P(d, None, stroke=c, sw=w, cap="round")]


def nose(s: Spec):
    sh = shade(s.skin, -0.28)
    return [
        P("M47.6,53.6 C48.6,55.0 51.4,55.0 52.4,53.6", None, stroke=sh, sw=0.9),
        P("M50.6,44.8 C51.6,48.6 52.2,51.2 51.8,53.0", None, stroke=sh, sw=0.6, salpha=0.55),
        P(ellipse(50.0, 52.8, 2.2, 1.2), shade(s.skin, 0.12), alpha=0.35),
    ]


def mouth(s: Spec):
    w = 4.6 * s.mouth_w
    y = 60.4 + (s.face_len - 1) * 3
    upper = (
        f"M{CX - w},{y} C{CX - w * 0.55},{y - 1.6} {CX - 1.0},{y - 2.2} {CX},{y - 1.2} "
        f"C{CX + 1.0},{y - 2.2} {CX + w * 0.55},{y - 1.6} {CX + w},{y} C{CX + w * 0.4},{y + 0.5} {CX - w * 0.4},{y + 0.5} {CX - w},{y}z"
    )
    lower = (
        f"M{CX - w * 0.92},{y + 0.2} C{CX - w * 0.5},{y + 3.0} {CX + w * 0.5},{y + 3.0} {CX + w * 0.92},{y + 0.2} "
        f"C{CX + w * 0.4},{y + 0.7} {CX - w * 0.4},{y + 0.7} {CX - w * 0.92},{y + 0.2}z"
    )
    line = f"M{CX - w * 0.95},{y + 0.15} C{CX - w * 0.4},{y + 0.75} {CX + w * 0.4},{y + 0.75} {CX + w * 0.95},{y + 0.15}"
    return [
        P(upper, shade(s.lips, -0.08)),
        P(lower, s.lips),
        P(ellipse(CX + 0.6, y + 1.6, w * 0.35, 0.55), "#FFFFFF", alpha=0.18),
        P(line, None, stroke=shade(s.lips, -0.45), sw=0.55),
    ]


def face_shading(s: Spec):
    fp = face_path(s)
    dark = shade(s.skin, -0.32)
    light = shade(s.skin, 0.35)
    cy = chin_y(s)
    return [
        G([
            P(rect(26, 14, 30, 64), linear(31, 0, 48, 0, (0, dark, 0.62), (0.55, dark, 0.18), (1, dark, 0.0))),
            P(rect(26, cy - 10, 48, 14), linear(0, cy - 9, 0, cy + 1, (0, dark, 0.0), (1, dark, 0.38))),
            P(ellipse(60, 33, 9, 7), radial(60, 33, 9, (0, light, 0.34), (1, light, 0.0))),
            P(ellipse(61.5, 51, 5.5, 3.6), radial(61.5, 51, 5.5, (0, light, 0.26), (1, light, 0.0))),
        ], clip=fp)
    ]


# ------------------------------------------------------------------ body

def body(s: Spec):
    neck_c = s.skin
    neck_sh = shade(s.skin, -0.3)
    cy = chin_y(s)
    out = [
        P(poly([(43.4, cy - 8), (56.6, cy - 8), (57.4, 88), (42.6, 88)]), neck_c),
        P(smooth([(43.2, cy - 6), (CX, cy + 3.5), (56.8, cy - 6), (57.2, cy + 4), (CX, cy + 9), (42.8, cy + 4)]), neck_sh, alpha=0.55),
    ]
    torso_top = 79.0
    if s.collar == "vneck":
        neckline = f"L41,{torso_top} L{CX},90 L59,{torso_top}"
    elif s.collar in ("turtle",):
        neckline = f"L41,{torso_top} L59,{torso_top}"
    else:
        neckline = f"L40.5,{torso_top} Q{CX},86.5 59.5,{torso_top}"
    torso = (
        f"M2,100 C3,88 13,82.5 30,80.6 L36,{torso_top - 0.5} {neckline} L64,{torso_top - 0.5} "
        f"C87,82.5 97,88 98,100z"
    )
    out.append(P(torso, s.clothes))
    # light falls on the right shoulder
    out.append(G([P(ellipse(78, 86, 18, 9), shade(s.clothes, 0.12), alpha=0.6)], clip=torso))
    out.append(G([P(ellipse(16, 94, 20, 12), shade(s.clothes, -0.25), alpha=0.6)], clip=torso))
    c = s.collar
    if c == "crew":
        out.append(P(f"M40.5,{torso_top} Q{CX},86.5 59.5,{torso_top}", None, stroke=shade(s.clothes, -0.28), sw=1.6))
    elif c == "vneck":
        out.append(P(f"M41,{torso_top} L{CX},90 L59,{torso_top}", None, stroke=shade(s.clothes, -0.3), sw=1.4))
    elif c == "turtle":
        out.append(P(f"M42.2,{cy - 2} C44,{cy + 1} 56,{cy + 1} 57.8,{cy - 2} L60,{torso_top + 3} C55,{torso_top + 6} 45,{torso_top + 6} 40,{torso_top + 3}z", s.clothes))
        out.append(P(f"M41,{torso_top - 1} C45,{torso_top + 2.5} 55,{torso_top + 2.5} 59,{torso_top - 1}", None, stroke=shade(s.clothes, -0.18), sw=0.8))
        out.append(P(f"M41.6,{cy + 4} C45,{cy + 6.5} 55,{cy + 6.5} 58.4,{cy + 4}", None, stroke=shade(s.clothes, -0.18), sw=0.8))
    elif c == "jacket":
        out.append(P(f"M40,{torso_top} L{CX},93 L60,{torso_top} Q{CX},86 40,{torso_top}z", s.clothes2))
        out.append(P(f"M36,{torso_top - 0.5} L40.5,{torso_top} L47.5,97 L41,100 L32,100 L33,86z", shade(s.clothes, 0.1)))
        out.append(P(f"M64,{torso_top - 0.5} L59.5,{torso_top} L52.5,97 L59,100 L68,100 L67,86z", shade(s.clothes, 0.04)))
        out.append(P("M47.5,97 L43,82", None, stroke=shade(s.clothes, -0.35), sw=0.6, salpha=0.6))
    elif c == "raincoat":
        out.append(P(f"M37,{torso_top - 4} C39,{cy - 1} 44,{cy + 2} {CX},{cy + 3} C56,{cy + 2} 61,{cy - 1} 63,{torso_top - 4} L65,{torso_top + 1} C58,{torso_top + 7} 42,{torso_top + 7} 35,{torso_top + 1}z", shade(s.clothes, -0.06)))
        out.append(P(f"M{CX},{torso_top + 5} L{CX},100", None, stroke=shade(s.clothes, -0.3), sw=1.0))
        out.append(P(circle(CX + 3.2, 90, 0.9), shade(s.clothes, -0.35)))
        out.append(P(circle(CX + 3.2, 96.5, 0.9), shade(s.clothes, -0.35)))
    elif c == "hoodie":
        out.append(P(f"M33,{torso_top + 1} C36,{torso_top - 6} 43,{torso_top - 3} {CX},{torso_top + 2} C57,{torso_top - 3} 64,{torso_top - 6} 67,{torso_top + 1} C62,{torso_top + 7} 38,{torso_top + 7} 33,{torso_top + 1}z", shade(s.clothes, -0.12)))
        out.append(P(f"M46,{torso_top + 4} L45.4,95", None, stroke="#E9E6E0", sw=0.9))
        out.append(P(f"M54,{torso_top + 4} L54.8,93", None, stroke="#E9E6E0", sw=0.9))
    elif c in ("shirt", "flannel"):
        out.append(P(f"M40.5,{torso_top - 1} L{CX},88 L59.5,{torso_top - 1} Q{CX},86 40.5,{torso_top - 1}z", shade(s.skin, -0.12)))
        out.append(P(f"M38.5,{torso_top - 2} L{CX - 0.5},88.5 L43,92 L35,86z", shade(s.clothes, 0.14)))
        out.append(P(f"M61.5,{torso_top - 2} L{CX + 0.5},88.5 L57,92 L65,86z", shade(s.clothes, 0.08)))
        if c == "flannel":
            for x in (14, 24, 74, 84):
                out.append(G([P(rect(x, 80, 2.2, 22), shade(s.clothes, -0.25), alpha=0.5)], clip=torso))
            for y in (91, 97):
                out.append(G([P(rect(0, y, 100, 1.6), shade(s.clothes, -0.25), alpha=0.45)], clip=torso))
        else:
            out.append(P(circle(CX, 93, 0.8), shade(s.clothes, -0.3)))
            out.append(P(circle(CX, 98.5, 0.8), shade(s.clothes, -0.3)))
    elif c == "windbreaker":
        out.append(P(f"M37,{torso_top - 3} C40,{cy + 1} 45,{cy + 3} {CX},{cy + 3.5} C55,{cy + 3} 60,{cy + 1} 63,{torso_top - 3} L64,{torso_top + 2} C58,{torso_top + 6} 42,{torso_top + 6} 36,{torso_top + 2}z", shade(s.clothes, 0.08)))
        out.append(P(f"M{CX + 1.5},{torso_top + 4} L{CX + 3},100", None, stroke="#C9CDD4", sw=0.8, salpha=0.8))
        out.append(P(f"M70,86 L84,84", None, stroke="#C9CDD4", sw=1.1, salpha=0.5))
    return out


# ------------------------------------------------------------------ hair styles

def hair_paint(h: str) -> object:
    return linear(66, 12, 36, 70, (0, shade(h, 0.14), 1.0), (1, shade(h, -0.14), 1.0))


def tilted_ellipse(cx, cy, rx, ry, deg, n=14):
    pts = [(cx + math.cos(2 * math.pi * i / n) * rx, cy + math.sin(2 * math.pi * i / n) * ry) for i in range(n)]
    return smooth(rot_pts(pts, deg, cx, cy))


def hair_back(s: Spec):
    h, st = s.hair, s.hair_style
    dark = shade(h, -0.2)
    if st == "long_straight":
        return [P(smooth([(CX, 13), (66, 17), (73, 32), (74, 56), (77, 80), (80, 92), (66, 94), (60, 78), (40, 78), (34, 94), (20, 92), (23, 80), (26, 56), (27, 32), (34, 17)]), dark)]
    if st == "curly_shoulder":
        pts = []
        for i in range(26):
            a = math.pi * 2 * i / 26
            r = 30 + (2.6 if i % 2 else -0.6)
            pts.append((CX + math.cos(a) * r * 1.05, 46 + math.sin(a) * r * 1.08))
        return [P(smooth(pts), dark)]
    if st == "braid":
        return [P(smooth([(CX, 15), (64, 18), (70, 30), (71, 48), (69, 60), (63, 62), (37, 62), (31, 60), (29, 48), (30, 30), (36, 18)]), dark)]
    if st == "gray_bun":
        return [
            P(circle(CX + 2, 14.5, 8.6), dark),
            P(circle(CX + 2, 14.5, 8.6), None, stroke=shade(h, -0.35), sw=0.6, salpha=0.6),
            P("M44,13 C47,10 55,10 58,15", None, stroke=shade(h, 0.2), sw=0.7, salpha=0.8),
            P("M45,17 C49,14 55,15 58.5,18", None, stroke=shade(h, -0.3), sw=0.6, salpha=0.7),
        ]
    if st == "pixie":
        return [P(smooth([(CX, 15), (63, 17), (69, 28), (70, 44), (67, 52), (33, 52), (30, 44), (31, 28), (37, 17)]), dark)]
    return []


def hair_front(s: Spec):
    h, st = s.hair, s.hair_style
    hi = shade(h, 0.22)
    lo = shade(h, -0.2)
    out = []
    if st == "long_straight":
        out.append(P(smooth([(CX, 15.5), (61, 17.5), (68, 26), (70.5, 40), (70, 60), (72, 78), (65, 80), (64.4, 58), (65.4, 40), (62, 28), (CX + 1.2, 21.5), (CX, 19.5), (CX - 1.2, 21.5), (38, 28), (34.6, 40), (35.6, 58), (35, 80), (28, 78), (30, 60), (29.5, 40), (32, 26), (39, 17.5)]), h))
        out.append(P("M50,17 C55.5,18 61,22 64,30", None, stroke=hi, sw=1.2, salpha=0.6))
        out.append(P("M66.5,40 C67,52 67,64 68.5,76", None, stroke=lo, sw=0.8, salpha=0.6))
    elif st == "braid":
        out.append(P(smooth([(CX - 4, 15.2), (58, 15.8), (65.5, 21), (68.6, 31), (68, 40), (66.4, 33.5), (61, 27), (52, 25.5), (44, 28.5), (37, 34.5), (33.6, 40), (31.8, 33), (33.5, 24), (40.5, 17.5)]), h))
        out.append(P("M40,22 C46,18.5 55,18 61,21.5", None, stroke=hi, sw=1.1, salpha=0.55))
        # woven braid falling over the right shoulder
        lo2 = shade(h, -0.32)
        for i in range(8):
            t = i / 7
            bx = 66.8 + t * 9.5 - math.sin(t * 2.6) * 1.2
            by = 56.5 + t * 34
            side = 1 if i % 2 else -1
            seg = tilted_ellipse(bx + side * 1.2, by, 3.9 - t * 0.6, 2.5 - t * 0.2, side * 32 + 8)
            out.append(P(seg, hair_paint(h)))
            out.append(P(seg, None, stroke=lo2, sw=0.55, salpha=0.8))
        out.append(P(f"M{76.3 - 2.3},{91.8} h4.6 v2.2 h-4.6z", "#D9A93F"))
        out.append(P(smooth([(74.4, 94), (78.4, 94), (79.4, 98.5), (76.8, 101), (73.6, 98.8)]), hair_paint(h)))
    elif st == "blond_messy":
        tufts = [(31.6, 41.5), (31, 31), (34, 22), (40, 16.4), (47, 13.6), (54, 13.2), (61, 15.2), (66.5, 20.2), (69, 28),
                 (69.2, 36), (68.4, 41.6), (66.6, 35), (64.2, 30.6), (60.6, 30.8), (57.6, 27.2), (53.2, 30), (48.6, 26.8),
                 (44.2, 30.2), (39.6, 28.8), (35.8, 32.8), (33.6, 37.4)]
        out.append(P(smooth(tufts, tension=0.95), hair_paint(h)))
        out.append(P("M41,19.5 C46,16.4 53,15.6 59,17.4", None, stroke=hi, sw=1.1, salpha=0.7))
        out.append(P("M60.5,19.8 C63.5,21 65.6,23.6 66.8,27", None, stroke=hi, sw=0.9, salpha=0.6))
        out.append(P("M49,27.2 C50,24 52.4,21.8 55.6,20.8", None, stroke=lo, sw=0.7, salpha=0.7))
        out.append(P("M40,29 C41,25.8 43,23.8 46,22.6", None, stroke=lo, sw=0.7, salpha=0.6))
    elif st == "curly_fade":
        pts = []
        n = 30
        for i in range(n + 1):
            a = math.pi + math.pi * i / n
            r = 18.2 + (0.7 if i % 2 else 0)
            pts.append((CX + math.cos(a) * r, 33.4 + math.sin(a) * r * 0.84))
        pts += [(67.4, 38), (65.6, 31.8), (59.6, 29.6), (CX, 29.0), (40.4, 29.6), (34.4, 31.8), (32.6, 38)]
        out.append(P(smooth(pts, tension=0.9), hair_paint(h)))
        for (x, y, k) in [(39, 23.5, 1), (44.5, 19.8, -1), (50.5, 18.6, 1), (56.5, 19.8, -1), (61.6, 23.5, 1),
                          (42, 26.8, -1), (48, 23.6, 1), (54, 23.4, -1), (59.4, 26.6, 1), (36.4, 29.2, -1), (64, 29.4, 1)]:
            out.append(P(f"M{x - 1.4},{y + 0.5} C{x - 0.8},{y - 1.1 * k} {x + 0.9},{y - 1.1 * k} {x + 1.4},{y + 0.4}",
                         None, stroke=hi, sw=0.6, salpha=0.45))
        out.append(P("M32.6,38 C32.8,42 33.4,45.4 34.4,48.4", None, stroke=shade(h, 0.08), sw=1.8, salpha=0.5))
        out.append(P("M67.4,38 C67.2,42 66.6,45.4 65.6,48.4", None, stroke=shade(h, 0.08), sw=1.8, salpha=0.5))
    elif st == "pixie":
        out.append(P(smooth([(CX - 6, 15.3), (57, 15.4), (64.5, 19.5), (68.8, 28), (69.4, 39), (67.6, 44), (66.6, 35), (63, 29.5), (56, 30.5), (48, 33.6), (41, 35.4), (35.4, 37.6), (32.6, 43), (30.8, 36), (31.6, 26), (36, 19.5)], tension=0.95), h))
        out.append(P("M37,26 C44,21 55,19.5 63,23", None, stroke=hi, sw=1.1, salpha=0.6))
        out.append(P("M58,30.5 C52,31.5 46,33.5 40,35.6", None, stroke=lo, sw=0.7, salpha=0.6))
    elif st == "slick":
        out.append(P(smooth([(CX, 13.8), (60, 15), (66.6, 21), (69, 30), (68.6, 39), (66.8, 32), (63.4, 26.6), (56, 24), (48, 23.4), (40, 25), (35, 29), (33, 36), (31.2, 39), (31, 30), (33.6, 21), (40, 15)]), h))
        for k in range(4):
            out.append(P(f"M{37 + k * 6},{26 - k * 0.4} C{42 + k * 5},{19 - k * 0.6} {50 + k * 4},{17 - k * 0.3} {58 + k * 3},{18 + k * 0.4}", None, stroke=hi, sw=0.7, salpha=0.45))
    elif st == "curly_shoulder":
        pts = []
        n = 18
        for i in range(n + 1):
            a = math.pi * 1.05 + math.pi * 0.9 * i / n
            r = 21.5 + (1.8 if i % 2 else 0)
            pts.append((CX + math.cos(a) * r * 1.02, 37 + math.sin(a) * r * 0.95))
        pts += [(69, 45), (66.6, 36), (60, 30), (CX, 29), (40, 30), (33.4, 36), (31, 45)]
        out.append(P(smooth(pts, tension=0.9), h))
        for (x, y) in [(38, 24), (44, 19.6), (51, 18), (58, 19.8), (63.5, 25), (35, 31), (66, 32)]:
            out.append(P(f"M{x - 2},{y + 1} C{x - 1},{y - 1.6} {x + 1.4},{y - 1.6} {x + 2},{y + 0.8}", None, stroke=hi, sw=0.8, salpha=0.6))
    elif st == "beanie":
        beanie = "#2E3440" if not s.extras or "beanie_red" not in s.extras else "#8B2E2E"
        out.append(P("M33,40 C35,41.5 37,40 39,39 C39.5,42 41,44 42,45 C40,41 39.5,40 39.6,38.5z", h))
        out.append(P("M67,40 C65,41.5 63,40 61,39 C60.5,42 59,44 58,45 C60,41 60.5,40 60.4,38.5z", h))
        out.append(P(smooth([(CX, 8.5), (61, 10.5), (68, 18), (70.6, 30), (70, 34), (30, 34), (29.4, 30), (32, 18), (39, 10.5)]), beanie))
        out.append(P("M28.6,29.6 C40,26.6 60,26.6 71.4,29.6 L71.6,37.8 C60,35.4 40,35.4 28.4,37.8z", shade(beanie, 0.12)))
        for k in range(9):
            x = 31 + k * 4.75
            out.append(P(f"M{x},{29.2 if 2 < k < 7 else 29.8} L{x},{36.6 if 2 < k < 7 else 37.4}", None, stroke=shade(beanie, -0.25), sw=0.6, salpha=0.7))
        out.append(P("M38,13 C46,9.5 56,9.5 63,13.5", None, stroke=shade(beanie, 0.25), sw=0.9, salpha=0.6))
    elif st == "gray_bun":
        out.append(P(smooth([(CX + 1, 16.2), (60, 17.5), (66.6, 24), (68.6, 34), (68, 42), (66, 34.5), (61, 27.6), (52, 24.6), (CX + 1, 22.5), (47, 25), (39, 27.6), (34, 34.5), (32, 42), (31.4, 34), (33.4, 24), (40, 17.5)]), h))
        out.append(P("M51,22.6 C56,22 62,24.5 65.5,30", None, stroke=shade(h, -0.25), sw=0.6, salpha=0.7))
        out.append(P("M49,22.6 C44,22.2 38,24.8 34.6,30", None, stroke=shade(h, -0.25), sw=0.6, salpha=0.7))
        out.append(P("M52,19 C57,18.6 62,20.6 65,24", None, stroke=shade(h, 0.3), sw=0.8, salpha=0.7))
    return out


def rrect_tie(x, y):
    return f"M{x - 2.2},{y} h4.4 v2.4 h-4.4z"


# ------------------------------------------------------------------ extras

def extras(s: Spec):
    out = []
    ex = s.extras
    if "glasses" in ex:
        frame = "#2B2422"
        out.append(P(circle(41.6, 44.6, 5.6), None, stroke=frame, sw=1.0))
        out.append(P(circle(58.4, 44.6, 5.6), None, stroke=frame, sw=1.0))
        out.append(P("M47.2,44 C48.8,42.8 51.2,42.8 52.8,44", None, stroke=frame, sw=0.9))
        out.append(P("M36,43.6 L33.2,42.6", None, stroke=frame, sw=0.9))
        out.append(P("M64,43.6 L66.8,42.6", None, stroke=frame, sw=0.9))
        out.append(P("M55.4,41.4 C56.8,40.6 58.6,40.6 60,41.2", None, stroke="#FFFFFF", sw=0.6, salpha=0.5))
    if "hoops" in ex:
        out.append(P(circle(66.6, 54.6, 1.9), None, stroke="#D9B45A", sw=0.7))
    if "stud" in ex:
        out.append(P(circle(53.2, 52.4, 0.55), "#E8E2D0"))
    if "freckles" in ex:
        for (x, y) in [(41, 50), (43.6, 51.2), (39.6, 52.4), (57, 50.6), (59.6, 51.4), (61.4, 49.8), (47.4, 49.4), (52.8, 49.2)]:
            out.append(P(circle(x, y, 0.42), shade(s.skin, -0.32), alpha=0.75))
    if "stubble" in ex:
        st = shade(s.skin, -0.42)
        cy = chin_y(s)
        out.append(G([
            P(rect(30, 50, 40, 26), radial(CX, cy - 2, 17, (0, st, 0.34), (0.6, st, 0.2), (1, st, 0.0))),
            P(ellipse(CX, 57.6, 6.5, 2.2), radial(CX, 57.6, 6.5, (0, st, 0.3), (1, st, 0.0))),
        ], clip=face_path(s)))
    if "blush" in ex:
        out.append(P(ellipse(39.4, 53.4, 4.2, 2.2), radial(39.4, 53.4, 4.2, (0, "#E0796B", 0.24), (1, "#E0796B", 0.0))))
        out.append(P(ellipse(60.6, 53.4, 4.2, 2.2), radial(60.6, 53.4, 4.2, (0, "#E0796B", 0.24), (1, "#E0796B", 0.0))))
    if "lines" in ex:
        out.append(P("M34.8,46.6 C33.8,47.6 33.2,48.6 33.2,49.8", None, stroke=shade(s.skin, -0.3), sw=0.45))
        out.append(P("M65.2,46.6 C66.2,47.6 66.8,48.6 66.8,49.8", None, stroke=shade(s.skin, -0.3), sw=0.45))
        out.append(P("M44.6,57.8 C43.6,59.2 43.4,60.6 43.8,62", None, stroke=shade(s.skin, -0.25), sw=0.45, salpha=0.8))
        out.append(P("M55.4,57.8 C56.4,59.2 56.6,60.6 56.2,62", None, stroke=shade(s.skin, -0.25), sw=0.45, salpha=0.8))
    return out


# ------------------------------------------------------------------ assemble

def background(bg: str):
    return [
        P(rect(0, 0, 100, 100), bg),
        P(rect(0, 0, 100, 100), radial(58, 34, 74, (0, shade(bg, 0.16), 1.0), (1, shade(bg, -0.18), 1.0))),
    ]


def portrait(s: Spec) -> Vec:
    head = []
    head += hair_back(s)
    head += ear(s, -1) + ear(s, 1)
    head.append(P(face_path(s), s.skin))
    head += face_shading(s)
    head += brow(s, -1) + brow(s, 1)
    head += eye(s, -1) + eye(s, 1)
    head += nose(s)
    head += mouth(s)
    head += extras(s)
    head += hair_front(s)
    children = background(s.bg) + body(s) + [G(head, rotate=s.tilt, px=CX, py=72)]
    return Vec(f"portrait_{s.name}", 70, 70, 100, 100, [G(children, clip=rect(0, 0, 100, 100))])


def default_avatar() -> Vec:
    """An account with no profile photo: the familiar placeholder silhouette."""
    bg = "#20242B"
    sil = "#4A515B"
    return Vec("portrait_heron", 70, 70, 100, 100, [
        P(rect(0, 0, 100, 100), bg),
        P(rect(0, 0, 100, 100), radial(50, 40, 70, (0, "#2A2F37"), (1, "#16191E"))),
        P(circle(50, 41, 15.5), sil),
        P("M18,100 C19,80 32,68 50,68 C68,68 81,80 82,100z", sil),
    ])


def hooded_figure() -> Vec:
    """Grainy, unidentified figure in a raised hood: only the outline reads."""
    bg = "#15171B"
    hood = "#0A0B0D"
    rim = "#3A4048"
    return Vec("portrait_unknown", 70, 70, 100, 100, [
        P(rect(0, 0, 100, 100), bg),
        P(rect(0, 0, 100, 100), radial(70, 26, 72, (0, "#2B3036"), (1, "#0F1114"))),
        P("M6,100 C8,84 22,74 36,72 C30,62 28,50 30,38 C33,22 42,14 52,14 C64,14 72,24 73,38 C74,52 70,64 64,72 C80,74 92,84 94,100z", hood),
        P("M52,14 C64,14 72,24 73,38 C74,52 70,64 64,72", None, stroke=rim, sw=1.4, salpha=0.7),
        P(smooth([(52, 30), (61, 34), (63, 48), (58, 62), (50, 66), (42, 60), (39, 46), (43, 33)]), "#050506"),
        P("M64,72 C80,74 92,84 94,100", None, stroke=rim, sw=1.2, salpha=0.5),
    ])


CAST = [
    Spec("mira", bg="#5F7F7D", skin="#E0B391", hair="#3A2620", hair_style="braid", iris="#4A3324", lips="#B9665E",
         clothes="#E2B13A", clothes2="#C8962A", collar="raincoat", jaw=0.96, tilt=-2.5, extras=["hoops", "blush"]),
    Spec("jonah", bg="#8A97A3", skin="#F1CCB4", hair="#D3AE68", hair_style="blond_messy", iris="#5B7C93", lips="#C27F72",
         clothes="#4E6B8C", clothes2="#E7E3DA", collar="jacket", jaw=1.06, face_len=1.04, tilt=3, extras=["stubble"]),
    Spec("priya", bg="#B79A57", skin="#A86E48", hair="#1C1718", hair_style="long_straight", iris="#2B1C14", lips="#8E4A40",
         clothes="#7A2E3A", collar="crew", jaw=0.94, tilt=-1.5, extras=["stud"]),
    Spec("owen", bg="#A5684B", skin="#6E4632", hair="#1D1716", hair_style="curly_fade", iris="#24170F", lips="#7A4A3C",
         clothes="#3E5E45", collar="flannel", jaw=1.08, face_len=1.03, tilt=2, brow_weight=1.15),
    Spec("celia", bg="#8FA287", skin="#F3D2BE", hair="#9A4527", hair_style="pixie", iris="#4E6A3E", lips="#C4766A",
         clothes="#E6DAC4", collar="turtle", jaw=0.95, tilt=-3, extras=["glasses", "freckles"]),
    Spec("marcus", bg="#58626C", skin="#C38D66", hair="#151314", hair_style="slick", iris="#2A1B12", lips="#A06656",
         clothes="#2A2523", clothes2="#C9CCD1", collar="jacket", jaw=1.1, face_len=1.05, tilt=1.5, brow_weight=1.2, extras=["stubble"]),
    Spec("ines", bg="#988CB0", skin="#D2A07B", hair="#3B2416", hair_style="curly_shoulder", iris="#3A2618", lips="#A85A55",
         clothes="#8B9097", collar="hoodie", jaw=0.95, tilt=2.5, extras=["blush"]),
    Spec("felix", bg="#B48890", skin="#F4DDD0", hair="#2A2120", hair_style="beanie", iris="#59636E", lips="#C08277",
         clothes="#25304A", collar="windbreaker", jaw=1.0, face_len=1.06, tilt=-2),
    Spec("rosa", bg="#AE8E66", skin="#C69070", hair="#BDB8B0", hair_style="gray_bun", iris="#3A2A1E", lips="#A2655A",
         clothes="#557499", collar="shirt", jaw=1.02, face_len=1.02, tilt=1, extras=["lines"]),
]


def all_portraits() -> list[Vec]:
    return [portrait(s) for s in CAST] + [default_avatar(), hooded_figure()]
