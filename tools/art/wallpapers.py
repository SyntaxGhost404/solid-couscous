"""Home-screen wallpapers for the two in-game phones (360 x 800 canvas, cover-scaled in-app)."""
from __future__ import annotations

import math
import random

from vec import G, P, Vec, circle, ellipse, linear, poly, radial, rect, smooth

W, H = 360.0, 800.0


def _ridge(rnd: random.Random, y0: float, amp: float, step: float, bottom: float) -> str:
    pts = [(-10, bottom)]
    x = -10.0
    while x < W + 10:
        pts.append((x, y0 - rnd.uniform(0, amp)))
        x += rnd.uniform(step * 0.6, step * 1.4)
    pts.append((W + 10, y0 - rnd.uniform(0, amp)))
    pts.append((W + 10, bottom))
    return smooth(pts, closed=True, tension=0.7)


def theo() -> Vec:
    """A moonlit lake beneath layered ridges: the view from the boathouse road."""
    rnd = random.Random(3)
    horizon = 548.0
    kids = [
        P(rect(0, 0, W, horizon), linear(0, 0, 0, horizon, (0, "#02050A"), (0.55, "#071423"), (1, "#123049"))),
        P(rect(0, horizon, W, H - horizon), linear(0, horizon, 0, H, (0, "#0F283C"), (0.3, "#081725"), (1, "#020509"))),
        P(circle(252, 392, 120), radial(252, 392, 120, (0, "#9FC3D1", 0.22), (1, "#9FC3D1", 0.0))),
        P(circle(252, 392, 17), "#E3ECEA"),
        P(circle(246, 387, 4.2), "#C9D6D5", alpha=0.6),
        P(circle(257, 398, 2.6), "#C9D6D5", alpha=0.5),
    ]
    stars = []
    for _ in range(70):
        x, y = rnd.uniform(0, W), rnd.uniform(0, 430)
        if (x - 252) ** 2 + (y - 392) ** 2 < 60 ** 2:
            continue
        stars.append((x, y, rnd.uniform(0.35, 1.05), rnd.uniform(0.25, 0.85)))
    for x, y, r, a in stars:
        kids.append(P(circle(x, y, r), "#DCE6F0", alpha=round(a, 2)))
    kids.append(P(_ridge(rnd, 470, 70, 46, horizon + 2), linear(0, 400, 0, horizon, (0, "#173247"), (1, "#0F2234"))))
    kids.append(P(_ridge(rnd, 520, 46, 30, horizon + 2), linear(0, 470, 0, horizon, (0, "#0D1C2A"), (1, "#0A1622"))))
    # treeline on the near shore
    trees = []
    x = -6.0
    while x < W + 6:
        h = rnd.uniform(10, 26)
        w = rnd.uniform(6, 10)
        trees.append(poly([(x, horizon + 1), (x + w / 2, horizon - h), (x + w, horizon + 1)]))
        x += rnd.uniform(4, 9)
    kids.append(P(" ".join(trees), "#060E16"))
    kids.append(P(rect(-4, horizon - 1, W + 8, 3), "#060E16"))
    # moon path on the water
    streaks = []
    for i in range(26):
        y = horizon + 8 + i * 9.5 + rnd.uniform(-2, 2)
        w = 8 + i * 2.2 + rnd.uniform(-4, 6)
        cx = 252 + rnd.uniform(-5, 5)
        streaks.append(rect(cx - w / 2, y, w, 1.2 + i * 0.05))
    kids.append(P(" ".join(streaks), "#BFD3DA", alpha=0.32))
    return Vec("wallpaper_theo", W, H, W, H, kids)


def mira() -> Vec:
    """The pier fair at dusk: a ferris wheel against a rose sky, strung with lights."""
    rnd = random.Random(5)
    horizon = 612.0
    kids = [
        P(rect(0, 0, W, horizon), linear(0, 0, 0, horizon, (0, "#140C24"), (0.38, "#3D1E4A"), (0.72, "#9C4659"), (1, "#E89067"))),
        P(rect(0, horizon, W, H - horizon), linear(0, horizon, 0, H, (0, "#B65E5E"), (0.25, "#4A2338"), (1, "#120A16"))),
        P(circle(104, horizon, 140), radial(104, horizon, 140, (0, "#FFD39A", 0.55), (0.4, "#FFB27A", 0.18), (1, "#FFB27A", 0.0))),
        G([P(circle(104, horizon - 4, 30), "#FFDDB0", alpha=0.88)], clip=rect(0, 0, W, horizon - 6)),
    ]
    # long, thin dusk clouds
    for (cx, cy, rx, ry, c, a) in [(60, 300, 120, 9, "#5A2A55", 0.55), (250, 352, 150, 8, "#6E3158", 0.5),
                                   (120, 420, 160, 7, "#C46A6B", 0.35), (300, 470, 120, 6, "#E39070", 0.3),
                                   (40, 505, 90, 5, "#F2A27A", 0.35)]:
        kids.append(P(smooth([(cx - rx, cy), (cx - rx * 0.4, cy - ry), (cx + rx * 0.3, cy - ry * 0.7), (cx + rx, cy),
                              (cx + rx * 0.2, cy + ry * 0.6), (cx - rx * 0.5, cy + ry * 0.5)]), c, alpha=a))
    stars = []
    for _ in range(26):
        x, y = rnd.uniform(0, W), rnd.uniform(0, 250)
        stars.append(circle(x, y, rnd.uniform(0.4, 0.9)))
    kids.append(P(" ".join(stars), "#F6E7F0", alpha=0.6))
    # far shore
    kids.append(P(smooth([(-10, horizon + 1), (-10, 598), (60, 590), (140, 600), (220, 594), (300, 602), (370, 596),
                          (370, horizon + 1)]), "#2A1530"))
    # ferris wheel
    wx, wy, wr = 268.0, 452.0, 104.0
    ink = "#1C0F22"
    spokes = []
    for i in range(16):
        a = 2 * math.pi * i / 16
        spokes.append(f"M{wx:.2f},{wy:.2f} L{wx + math.cos(a) * wr:.2f},{wy + math.sin(a) * wr:.2f}")
    kids += [
        P(poly([(wx - 3, wy), (wx + 3, wy), (wx + 58, horizon + 30), (wx + 48, horizon + 30)]), ink),
        P(poly([(wx - 3, wy), (wx + 3, wy), (wx - 48, horizon + 30), (wx - 58, horizon + 30)]), ink),
        P(circle(wx, wy, wr) + " " + circle(wx, wy, wr - 4.5), ink, evenodd=True),
        P(circle(wx, wy, wr * 0.58), None, stroke=ink, sw=1.6),
        P(" ".join(spokes), None, stroke=ink, sw=1.3),
        P(circle(wx, wy, 8), ink),
    ]
    cars, bulbs = [], []
    for i in range(16):
        a = 2 * math.pi * (i + 0.5) / 16
        px, py = wx + math.cos(a) * wr, wy + math.sin(a) * wr
        cars.append(f"M{px:.2f},{py:.2f} L{px:.2f},{py + 6:.2f}")
        cars.append(f"M{px - 6:.2f},{py + 6:.2f} h12 l-1.4,8 C{px + 3:.2f},{py + 16:.2f} {px - 3:.2f},{py + 16:.2f} {px - 4.6:.2f},{py + 14:.2f}z")
        for k in range(2):
            b = 2 * math.pi * (i + k * 0.5) / 16
            bulbs.append(circle(wx + math.cos(b) * (wr - 2.2), wy + math.sin(b) * (wr - 2.2), 1.5))
    kids.append(P(" ".join(c for c in cars if c.endswith("z")), ink))
    kids.append(P(" ".join(c for c in cars if not c.endswith("z")), None, stroke=ink, sw=1.1))
    kids.append(P(" ".join(bulbs), "#FFD58A", alpha=0.95))
    kids.append(P(circle(wx, wy, wr + 10), radial(wx, wy, wr + 26, (0.8, "#FFC27A", 0.0), (0.9, "#FFC27A", 0.14), (1, "#FFC27A", 0.0))))
    # strings of festoon lights from the left edge to the wheel's leg
    for (x0, y0, x1, y1, sag) in [(-10, 520, 222, 560, 46), (-10, 572, 214, 594, 30)]:
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2 + sag
        kids.append(P(f"M{x0},{y0} Q{mx:.1f},{my:.1f} {x1},{y1}", None, stroke="#1C0F22", sw=0.9))
        pts = []
        for i in range(1, 15):
            t = i / 15
            bx = (1 - t) ** 2 * x0 + 2 * (1 - t) * t * mx + t * t * x1
            by = (1 - t) ** 2 * y0 + 2 * (1 - t) * t * my + t * t * y1
            pts.append(circle(bx, by + 2.2, 1.7))
        kids.append(P(" ".join(pts), "#FFE2A4", alpha=0.95))
    # boardwalk railing in front
    rail_top = 676.0
    kids.append(P(rect(-4, rail_top + 46, W + 8, H - rail_top - 46), "#120A16"))
    posts = " ".join(rect(x, rail_top, 5, 52) for x in range(-2, 370, 46))
    kids.append(P(posts, "#160C1B"))
    kids.append(P(rect(-4, rail_top, W + 8, 5), "#160C1B"))
    kids.append(P(rect(-4, rail_top + 22, W + 8, 3), "#160C1B"))
    # sunset path on the water
    streaks = []
    for i in range(10):
        y = horizon + 6 + i * 6.2
        w = 26 + i * 7 + rnd.uniform(-6, 6)
        streaks.append(rect(104 - w / 2 + rnd.uniform(-4, 4), y, w, 1.4))
    kids.append(P(" ".join(streaks), "#FFD3A0", alpha=0.45))
    return Vec("wallpaper_mira", W, H, W, H, kids)


def all_wallpapers() -> list[Vec]:
    return [theo(), mira()]
