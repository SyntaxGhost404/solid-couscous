"""Layered key art for the title screen: a still night lake, a pier running into the mist and an
empty rowboat. Sky, moon, mist, water shimmer and the lantern glow are drawn live in Compose; these
are the static silhouettes, each on the same 360 x 470 canvas so they stack exactly.
"""
from __future__ import annotations

import math
import random

from vec import G, P, Vec, circle, ellipse, linear, poly, rect, smooth

W, H = 360.0, 470.0
HORIZON = 252.0


def _pine(x: float, base: float, h: float, w: float, rnd: random.Random) -> list:
    """A jagged conifer silhouette as a list of outline points (left to right)."""
    tiers = max(3, int(h / 7))
    left, right = [], []
    for i in range(tiers + 1):
        t = i / tiers
        y = base - h + t * h
        half = w * (0.12 + 0.88 * t) / 2
        jag = half * (0.55 + 0.25 * rnd.random())
        left.append((x - half, y))
        if i < tiers:
            left.append((x - jag, y + h / tiers * 0.55))
        right.append((x + half, y))
        if i < tiers:
            right.append((x + jag, y + h / tiers * 0.55))
    return [(x, base - h - 2.5)] + right + list(reversed(left))


def far_shore() -> Vec:
    rnd = random.Random(7)
    hills = smooth([(-10, HORIZON), (-10, 214), (40, 206), (95, 214), (150, 200), (205, 209), (262, 196), (318, 204),
                    (370, 198), (370, HORIZON)], closed=True, tension=0.8)
    kids = [P(hills, linear(0, 196, 0, HORIZON, (0, "#152836"), (1, "#0F1E29")))]
    back, front = [], []
    x = -6.0
    while x < W + 8:
        dense = 0.55 if 160 < x < 236 else 1.0
        h = rnd.uniform(10, 26) * dense
        if rnd.random() < 0.18:
            h *= 1.55
        back.append(poly(_pine(x, HORIZON - 2, h, rnd.uniform(6, 10), rnd)))
        x += rnd.uniform(4.5, 9.5)
    x = -4.0
    while x < W + 8:
        gap = 150 < x < 246
        if gap and rnd.random() < 0.7:
            x += rnd.uniform(6, 12)
            continue
        h = rnd.uniform(12, 30) * (0.7 if gap else 1.0)
        if rnd.random() < 0.15:
            h *= 1.45
        front.append(poly(_pine(x, HORIZON + 1, h, rnd.uniform(7, 12), rnd)))
        x += rnd.uniform(7, 15)
    kids.append(P(" ".join(back), "#0E1A23"))
    kids.append(P(" ".join(front), "#080F15"))
    kids.append(P(rect(-4, HORIZON - 1, W + 8, 3.5), "#080F15"))
    return Vec("title_far", W, H, W, H, kids)


def pier() -> Vec:
    vx, vy = 198.0, 262.0  # far end of the pier
    near_l, near_r, near_y = 34.0, 150.0, 472.0
    far_l, far_r = vx - 7.0, vx + 6.0
    deck = poly([(far_l, vy), (far_r, vy), (near_r, near_y), (near_l, near_y)])
    side = poly([(far_r, vy), (far_r, vy + 2.4), (near_r + 4, near_y + 6), (near_r, near_y)])
    kids = [
        P(side, "#070B0F"),
        P(deck, linear(0, vy, 0, near_y, (0, "#2C3943"), (0.3, "#1A242D"), (0.7, "#0E151B"), (1, "#070A0D"))),
    ]
    planks = []
    n = 26
    for i in range(1, n):
        t = (i / n) ** 1.9  # perspective spacing
        y = vy + (near_y - vy) * t
        xl = far_l + (near_l - far_l) * t
        xr = far_r + (near_r - far_r) * t
        planks.append(f"M{xl:.2f},{y:.2f} L{xr:.2f},{y:.2f}")
    kids.append(P(" ".join(planks), None, stroke="#05080B", sw=0.9, salpha=0.6))
    # pilings and their broken reflections
    posts, refl = [], []
    for i in range(0, 9):
        t = (i / 8) ** 1.9
        y = vy + (near_y - vy) * t
        xr = far_r + (near_r - far_r) * t
        pw = 1.4 + 6.5 * t
        ph = 3 + 30 * t
        posts.append(rect(xr - pw * 0.2, y - 1, pw, ph))
        for k in range(3):
            refl.append(rect(xr - pw * 0.1 + (k % 2) * 0.8, y + ph + 2 + k * (2.4 + 3 * t), pw * (0.9 - 0.2 * k), 0.9 + 1.4 * t))
    kids.append(P(" ".join(posts), "#05080B"))
    kids.append(P(" ".join(refl), "#05080B", alpha=0.55))
    # lantern post at the far end (glow is drawn live)
    kids += [
        P(rect(vx + 1.6, 236.5, 1.3, 26), "#06090C"),
        P(rect(vx + 2.9, 238, 4.6, 0.9), "#06090C"),
        P(poly([(vx + 5.1, 238.9), (vx + 7.9, 238.9), (vx + 8.4, 245.8), (vx + 4.6, 245.8)]), "#0B0F13"),
        P(rect(vx + 5.3, 240.0, 2.4, 4.9), "#FFD58F"),
        P(poly([(vx + 4.6, 238.9), (vx + 8.4, 238.9), (vx + 6.5, 236.6)]), "#06090C"),
    ]
    return Vec("title_pier", W, H, W, H, kids)


def boat() -> Vec:
    hull = "M218,298 C222,304 232,307.6 248,307.6 C262,307.6 272,305 277,299.4 C268,301.6 258,302.4 247,302.2 C234,302 224,300.6 218,298z"
    hull_side = "M218,298 C224,300.6 234,302 247,302.2 C258,302.4 268,301.6 277,299.4 L276.4,302 C271,306.4 262,309.6 248,309.6 C232,309.6 222,306 218,298z"
    kids = [
        P(hull_side, "#05080B"),
        P(hull, "#0E171E"),
        P("M220,298.6 C228,301 238,302.2 248,302.2 C258,302.4 268,301.6 276.4,299.6", None, stroke="#5B7480", sw=0.8, salpha=0.65),
        P(rect(236, 300.4, 1.4, 4.2), "#05080B"),
        P(rect(256, 300.6, 1.4, 4.2), "#05080B"),
        # an oar left resting across the gunwale
        P("M226,299.2 L262,294.6", None, stroke="#0A1116", sw=1.3),
        P(ellipse(264.6, 294.2, 3.6, 1.2), "#0A1116"),
        # reflection
        P("M220,311.6 C230,315.6 240,316.6 248,316.6 C258,316.6 268,315 275,311.6", None, stroke="#05080B", sw=3.0, salpha=0.4),
    ]
    return Vec("title_boat", W, H, W, H, kids)


def _blades(rnd: random.Random, x0: float, x1: float, base: float, hmin: float, hmax: float, lean: float, count: int):
    out = []
    for _ in range(count):
        x = rnd.uniform(x0, x1)
        h = rnd.uniform(hmin, hmax)
        bend = rnd.uniform(-1, 1) * lean
        w = rnd.uniform(1.6, 3.6)
        tip = (x + bend, base - h)
        ctrl = (x + bend * 0.25, base - h * 0.55)
        out.append(
            f"M{x - w / 2:.2f},{base} Q{ctrl[0] - w * 0.3:.2f},{ctrl[1]:.2f} {tip[0]:.2f},{tip[1]:.2f} "
            f"Q{ctrl[0] + w * 0.3:.2f},{ctrl[1]:.2f} {x + w / 2:.2f},{base}z"
        )
    return out


def _cattails(rnd: random.Random, xs, base: float, hmin: float, hmax: float):
    out = []
    for x in xs:
        h = rnd.uniform(hmin, hmax)
        bend = rnd.uniform(-6, 6)
        top = (x + bend, base - h)
        out.append(f"M{x - 0.7:.2f},{base} Q{x + bend * 0.3:.2f},{base - h * 0.5:.2f} {top[0] - 0.5:.2f},{top[1]:.2f} "
                   f"L{top[0] + 0.5:.2f},{top[1]:.2f} Q{x + bend * 0.3 + 1:.2f},{base - h * 0.5:.2f} {x + 0.7:.2f},{base}z")
        ang = math.degrees(math.atan2(bend, h * 0.5))
        cx, cy = top[0] + math.sin(math.radians(ang)) * 9, top[1] + 9
        pts = [(cx + math.cos(t) * 2.4, cy + math.sin(t) * 8.5) for t in [i * math.pi / 7 for i in range(14)]]
        out.append(smooth(pts))
        out.append(f"M{top[0]:.2f},{top[1] - 0.5:.2f} L{top[0] + bend * 0.05:.2f},{top[1] - 7:.2f}")
    return out


def reeds(side: str) -> Vec:
    rnd = random.Random(11 if side == "left" else 23)
    base = H + 2
    if side == "left":
        blades = _blades(rnd, -8, 78, base, 40, 150, 26, 46)
        tails = _cattails(rnd, [14, 31, 52], base, 120, 170)
    else:
        blades = _blades(rnd, 280, 372, base, 40, 170, 26, 50)
        tails = _cattails(rnd, [298, 322, 343], base, 140, 195)
    paths = [d for d in blades + tails if not d.startswith("M") or "z" in d]
    stems = [d for d in tails if "z" not in d]
    kids = [P(" ".join(paths), "#03060A")]
    if stems:
        kids.append(P(" ".join(stems), None, stroke="#03060A", sw=0.8))
    return Vec(f"title_reeds_{side}", W, H, W, H, kids)


# Each layer is exported cropped to its own bounds (x0, y0, x1, y1 on the 360 x 470 canvas) so the
# app only rasterizes what it draws. TitleScreen.kt places the layers with the same numbers.
BOUNDS = {
    "title_far": (0, 188, 360, 256),
    "title_pier": (30, 234, 216, 470),
    "title_boat": (216, 292, 280, 318),
    "title_reeds_left": (-40, 290, 110, 470),
    "title_reeds_right": (250, 268, 390, 470),
}


def cropped(vec: Vec) -> Vec:
    x0, y0, x1, y1 = BOUNDS[vec.name]
    w, h = x1 - x0, y1 - y0
    return Vec(vec.name, w, h, w, h, [G(vec.children, tx=-x0, ty=-y0, clip=None)])


def all_title() -> list[Vec]:
    return [cropped(v) for v in (far_shore(), pier(), boat(), reeds("left"), reeds("right"))]


def composite_preview() -> Vec:
    """Rough stand-in for the live layers, to judge the composition in previews."""
    sky = P(rect(0, 0, W, HORIZON), linear(0, 0, 0, HORIZON, (0, "#04070B"), (0.6, "#0B1823"), (1, "#1B3140")))
    lake = P(rect(0, HORIZON, W, H - HORIZON), linear(0, HORIZON, 0, H, (0, "#16303D"), (0.25, "#0C1922"), (1, "#05090C")))
    moon = [P(circle(246, 108, 70), "#CFE3E0", alpha=0.06), P(circle(246, 108, 36), "#CFE3E0", alpha=0.08), P(circle(246, 108, 22), "#DDE9E6")]
    glow = [P(circle(205.5, 242.5, 26), "#FFC46B", alpha=0.10), P(circle(205.5, 242.5, 10), "#FFC46B", alpha=0.22)]
    layers = [sky, lake] + moon + far_shore().children + glow + pier().children + boat().children + reeds("left").children + reeds("right").children
    return Vec("title_composite", W, H, W, H, layers)
