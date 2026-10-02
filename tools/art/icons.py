"""In-game app icons: rounded tiles with original glyphs (64 x 64 canvas)."""
from __future__ import annotations

import math
import random

from vec import G, P, Vec, circle, ellipse, linear, poly, radial, rect, rrect, shade, smooth

S = 64.0
R = 14.0
WHITE = "#FFFFFF"


def tile(c1: str, c2: str) -> P:
    return P(rrect(0, 0, S, S, R), linear(0, 0, S * 0.35, S, (0, c1), (1, c2)))


def heart(cx: float, cy: float, s: float) -> str:
    return (
        f"M{cx},{cy + 0.36 * s} C{cx - 0.12 * s},{cy + 0.26 * s} {cx - 0.5 * s},{cy + 0.02 * s} {cx - 0.5 * s},{cy - 0.24 * s} "
        f"C{cx - 0.5 * s},{cy - 0.52 * s} {cx - 0.12 * s},{cy - 0.6 * s} {cx},{cy - 0.32 * s} "
        f"C{cx + 0.12 * s},{cy - 0.6 * s} {cx + 0.5 * s},{cy - 0.52 * s} {cx + 0.5 * s},{cy - 0.24 * s} "
        f"C{cx + 0.5 * s},{cy + 0.02 * s} {cx + 0.12 * s},{cy + 0.26 * s} {cx},{cy + 0.36 * s}z"
    )


def messages() -> Vec:
    bg = "#2F7DE1"
    return Vec("icon_messages", S, S, S, S, [
        tile("#4A97F5", "#2569D2"),
        P(rrect(13, 15, 38, 27, 12) + " M20,39 L16,49 L30,41.5z", WHITE),
        P(circle(23.6, 28.6, 2.7) + " " + circle(32, 28.6, 2.7) + " " + circle(40.4, 28.6, 2.7), bg),
    ])


def phone() -> Vec:
    hand = (
        "M23.5,16.5 C25,16.2 26.4,17 27,18.4 L29.6,24.4 C30.2,25.8 29.8,27.4 28.6,28.4 L26,30.4 "
        "C27.8,34.8 31.2,38.2 35.6,40 L37.6,37.4 C38.6,36.2 40.2,35.8 41.6,36.4 L47.6,39 "
        "C49,39.6 49.8,41 49.5,42.5 L48.8,46 C48.4,48 46.6,49.4 44.6,49.3 "
        "C29.6,48.6 17.4,36.4 16.7,21.4 C16.6,19.4 18,17.6 20,17.2z"
    )
    return Vec("icon_phone", S, S, S, S, [tile("#47C77D", "#239A57"), P(hand, WHITE)])


def calculator() -> Vec:
    orange = "#F59E42"
    w = 3.2
    return Vec("icon_calculator", S, S, S, S, [
        tile("#41464F", "#272B31"),
        P("M17,23.5 h12 M23,17.5 v12", None, stroke=WHITE, sw=w),
        P("M35,23.5 h12", None, stroke=WHITE, sw=w),
        P("M18.6,36.6 l8.8,8.8 M27.4,36.6 l-8.8,8.8", None, stroke=WHITE, sw=w),
        P("M35,38 h12 M35,44 h12", None, stroke=orange, sw=w),
    ])


def browser() -> Vec:
    return Vec("icon_browser", S, S, S, S, [
        tile("#6576F2", "#3B48C4"),
        P(circle(32, 32, 16.5) + " " + circle(32, 32, 13.4), WHITE, evenodd=True),
        P("M32,32 L40.6,23.4 L35.2,35.2z M32,32 L23.4,40.6 L28.8,28.8z", WHITE),
        P("M32,32 L40.6,23.4 L28.8,28.8z M32,32 L23.4,40.6 L35.2,35.2z", WHITE, alpha=0.55),
        P(circle(32, 32, 2.0), "#4A58D8"),
        P("M32,17.5 v3 M32,43.5 v3 M17.5,32 h3 M43.5,32 h3", None, stroke=WHITE, sw=1.6),
    ])


def notes() -> Vec:
    bg = "#D9952A"
    return Vec("icon_notes", S, S, S, S, [
        tile("#F0B848", "#D18A21"),
        P("M20,14 h18 l8,8 v25 a3,3 0 0 1 -3,3 h-23 a3,3 0 0 1 -3,-3 v-30 a3,3 0 0 1 3,-3z", WHITE),
        P("M38,14 v5 a3,3 0 0 0 3,3 h5z", "#F6DFAE"),
        P("M23,28 h18 M23,33.5 h18 M23,39 h12", None, stroke=bg, sw=2.0),
    ])


def archive() -> Vec:
    bg = "#55657A"
    return Vec("icon_archive", S, S, S, S, [
        tile("#7486A0", "#4A596D"),
        P(rrect(14, 17, 36, 9, 2.4), WHITE),
        P(rrect(16.5, 28, 31, 20, 2.6), WHITE),
        P(rrect(26.5, 32.5, 11, 3.4, 1.7), bg),
    ])


def loose_ends() -> Vec:
    rnd = random.Random(9)
    base = rrect(0, 0, S, S, R)
    kids = [P(base, linear(0, 0, S, S, (0, "#D2B282"), (1, "#B8935E")))]
    blots = []
    for _ in range(22):
        x, y = rnd.uniform(-4, 68), rnd.uniform(-4, 68)
        rx, ry = rnd.uniform(1.5, 7), rnd.uniform(1, 4)
        blots.append(ellipse(x, y, rx, ry))
    fibers = []
    for _ in range(26):
        x, y = rnd.uniform(0, 64), rnd.uniform(0, 64)
        a = rnd.uniform(0, math.pi)
        l = rnd.uniform(2, 6)
        fibers.append(f"M{x:.1f},{y:.1f} l{math.cos(a) * l:.1f},{math.sin(a) * l:.1f}")
    kids.append(G([
        P(" ".join(blots[:11]), "#A88350", alpha=0.25),
        P(" ".join(blots[11:]), "#E6CC9E", alpha=0.3),
        P(" ".join(fibers), None, stroke="#8C6A3E", sw=0.5, salpha=0.4),
    ], clip=base))
    thread = "M9,48 C17,47 22,42 26,36 C30,30 33,22 39,22 C45,22 46,30 40,33 C34,36 27,32 30,26 C33,20 44,19 55,15"
    kids += [
        P(thread, None, stroke="#5E1418", sw=3.4, salpha=0.35),
        P(thread, None, stroke="#B3262E", sw=2.6),
        P("M55,15 l3.5,-2.2 M55,15 l4,-0.4 M55,15 l2.6,-3.4", None, stroke="#B3262E", sw=1.0),
        P("M9,48 l-3.5,1.6 M9,48 l-3,3 M9,48 l-4,-0.2", None, stroke="#B3262E", sw=1.0),
    ]
    return Vec("icon_loose_ends", S, S, S, S, kids)


def gear(cx, cy, r_out, r_in, teeth, hole) -> str:
    pts = []
    for i in range(teeth):
        a = 2 * math.pi * i / teeth
        half = math.pi / teeth
        for da, r in ((-half * 0.95, r_in), (-half * 0.48, r_out), (half * 0.48, r_out), (half * 0.95, r_in)):
            pts.append((cx + math.cos(a + da) * r, cy + math.sin(a + da) * r))
    d = poly(pts)
    return d + " " + circle(cx, cy, hole)


def settings(enabled_look: bool = True) -> Vec:
    return Vec("icon_settings", S, S, S, S, [
        tile("#4D535C", "#2B2F35"),
        P(gear(32, 32, 17.2, 13.2, 8, 5.4), "#E3E7EC", evenodd=True),
    ])


def mail() -> Vec:
    bg = "#E0574A"
    return Vec("icon_mail", S, S, S, S, [
        tile("#F47463", "#CF4234"),
        P(rrect(13.5, 19, 37, 26, 3.6), WHITE),
        P("M16,22.5 L32,35 L48,22.5", None, stroke=bg, sw=2.6),
    ])


def gallery() -> Vec:
    return Vec("icon_gallery", S, S, S, S, [
        tile("#FFAA5C", "#EE5D58"),
        P(rrect(13, 16, 38, 32, 5) + " " + rrect(16.4, 19.4, 31.2, 25.2, 2.4), WHITE, evenodd=True),
        P("M18.5,42.5 L27,31 L33,38.6 L37,34 L45.5,42.5z", WHITE),
        P(circle(39.5, 26.5, 3.2), WHITE),
    ])


def picnook() -> Vec:
    tint = "#C651B8"
    return Vec("icon_picnook", S, S, S, S, [
        tile("#B75BE6", "#E2508F"),
        G([P(rrect(-12, -14, 24, 28, 3), WHITE, alpha=0.5)], rotate=-12, tx=29, ty=31),
        G([
            P(rrect(-12, -14, 24, 28, 3), WHITE),
            P(rrect(-9, -11, 18, 16.5, 1.4), tint),
            P(heart(0, -2.6, 9.0), WHITE),
        ], rotate=7, tx=35, ty=33),
    ])


def diary() -> Vec:
    return Vec("icon_diary", S, S, S, S, [
        tile("#93603F", "#673B24"),
        P(rrect(18, 13, 28, 38, 3.4), "#F3E9D8"),
        P(rrect(18, 13, 5.5, 38, 3.4, 3.4), "#D7C6A8"),
        P("M37,13 v10.5 l2.6,-2.2 l2.6,2.2 v-10.5z", "#C8413B"),
        P(rrect(39, 29, 11, 7, 2.2), "#C9A157"),
        P(circle(44.5, 32.5, 1.4), "#6E5222"),
    ])


def files() -> Vec:
    return Vec("icon_files", S, S, S, S, [
        tile("#5A9ACB", "#2F6893"),
        P("M14,21 a3,3 0 0 1 3,-3 h9.5 l4,4.5 h16.5 a3,3 0 0 1 3,3 v4 h-36z", WHITE, alpha=0.7),
        P(rrect(14, 26, 36, 21, 3.2), WHITE),
    ])


def _crescent(cx1, cy1, r1, cx2, cy2, r2) -> str:
    d = math.hypot(cx2 - cx1, cy2 - cy1)
    a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(max(0.0, r1 * r1 - a * a))
    ux, uy = (cx2 - cx1) / d, (cy2 - cy1) / d
    mx, my = cx1 + a * ux, cy1 + a * uy
    p1 = (mx - h * uy, my + h * ux)
    p2 = (mx + h * uy, my - h * ux)
    return (
        f"M{p1[0]:.2f},{p1[1]:.2f} A{r1},{r1} 0 1 1 {p2[0]:.2f},{p2[1]:.2f} "
        f"A{r2},{r2} 0 1 0 {p1[0]:.2f},{p1[1]:.2f}z"
    )


def mooncrush() -> Vec:
    return Vec("icon_mooncrush", S, S, S, S, [
        tile("#43327E", "#211646"),
        P(circle(14, 14, 1.0) + " " + circle(49, 12, 0.8) + " " + circle(52, 47, 1.0), "#F2E7C9", alpha=0.7),
        P(_crescent(29, 33, 16, 37.5, 27, 13.5), "#F2E7C9"),
        P(heart(40.5, 39.5, 13), "#FF7AA8"),
    ])


def all_icons() -> list[Vec]:
    return [messages(), phone(), calculator(), browser(), notes(), archive(), loose_ends(), settings(),
            mail(), gallery(), picnook(), diary(), files(), mooncrush()]
