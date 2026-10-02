"""Original object art: advisory headphones and the case-board desk props."""
from __future__ import annotations

import math

from glyphs import text_path
from vec import G, P, Vec, circle, ellipse, linear, poly, radial, rect, rrect, shade, smooth


def _pt(cx, cy, r, deg):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


def arc_band(cx, cy, r_out, r_in, a0, a1) -> str:
    """Annular sector from a0 to a1 degrees (SVG angles: clockwise, y down)."""
    x0, y0 = _pt(cx, cy, r_out, a0)
    x1, y1 = _pt(cx, cy, r_out, a1)
    x2, y2 = _pt(cx, cy, r_in, a1)
    x3, y3 = _pt(cx, cy, r_in, a0)
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return (
        f"M{x0:.2f},{y0:.2f} A{r_out},{r_out} 0 {large} 1 {x1:.2f},{y1:.2f} L{x2:.2f},{y2:.2f} "
        f"A{r_in},{r_in} 0 {large} 0 {x3:.2f},{y3:.2f}z"
    )


def arc_line(cx, cy, r, a0, a1) -> str:
    x0, y0 = _pt(cx, cy, r, a0)
    x1, y1 = _pt(cx, cy, r, a1)
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return f"M{x0:.2f},{y0:.2f} A{r},{r} 0 {large} 1 {x1:.2f},{y1:.2f}"


# ---------------------------------------------------------------- advisory

def headphones() -> Vec:
    cx, cy = 67.0, 72.0
    shell, shell_hi, shell_lo = "#3B4047", "#5A616B", "#24282D"
    metal = "#8C939C"
    kids = [
        # headband: outer shell plus the padded underside
        P(arc_band(cx, cy, 52, 43.5, 196, 344), linear(30, 20, 104, 60, (0, shell_hi), (0.5, shell), (1, shell_lo))),
        P(arc_band(cx, cy, 44.5, 40.0, 204, 336), "#202328"),
        P(arc_line(cx, cy, 50.6, 214, 300), None, stroke="#7C848E", sw=1.1, salpha=0.55),
    ]

    def yoke(side: int):
        x = cx + side * 48.6
        d = poly([(x - 3.4, 54), (x + 3.4, 54), (x + 2.8 + side * 1.2, 74), (x - 2.8 + side * 1.2, 74)])
        return [
            P(d, linear(x - 4, 0, x + 4, 0, (0, shade(metal, -0.25)), (0.5, shade(metal, 0.2)), (1, shade(metal, -0.1)))),
            P(rect(x - 3.6, 57.5, 7.2, 1.2), shade(metal, -0.35), alpha=0.8),
        ]

    kids += yoke(-1) + yoke(1)
    # left cup: outer shell facing the viewer
    lx, rx, cyc = cx - 47.0, cx + 47.0, 95.0
    kids += [
        P(ellipse(lx, cyc, 17.5, 22.5), "#202328"),
        P(ellipse(lx - 1.6, cyc, 15.5, 20.5), linear(lx - 16, cyc - 20, lx + 14, cyc + 20, (0, shell_hi), (0.55, shell), (1, shell_lo))),
        P(ellipse(lx - 2.0, cyc, 9.0, 12.0), linear(lx - 10, cyc - 12, lx + 8, cyc + 12, (0, "#4E555E"), (1, "#30353B"))),
        P(ellipse(lx - 2.0, cyc, 9.0, 12.0), None, stroke="#6C747E", sw=0.8, salpha=0.6),
        P(f"M{lx + 3},{cyc - 19} C{lx + 10},{cyc - 15} {lx + 13},{cyc - 7} {lx + 13},{cyc + 1}", None, stroke="#8A929C", sw=1.0, salpha=0.5),
    ]
    # right cup: turned slightly, the cushion and grille visible
    kids += [
        P(ellipse(rx, cyc, 17.5, 22.5), linear(rx - 16, cyc - 20, rx + 16, cyc + 22, (0, shell), (1, shell_lo))),
        P(ellipse(rx + 1.4, cyc, 14.0, 19.0), "#181A1E"),
        P(ellipse(rx + 1.8, cyc, 9.6, 13.6), linear(rx - 8, cyc - 13, rx + 10, cyc + 13, (0, "#3D434A"), (1, "#2A2E33"))),
        P(ellipse(rx + 1.8, cyc, 2.2, 2.8), "#7A828C"),
        P(f"M{rx - 9},{cyc - 17} C{rx - 13},{cyc - 10} {rx - 14.5},{cyc} {rx - 13},{cyc + 9}", None, stroke="#646B75", sw=0.9, salpha=0.6),
    ]
    return Vec("art_headphones", 134, 134, 134, 134, kids)


# ---------------------------------------------------------------- desk props

def clipboard() -> Vec:
    board, board_hi = "#6E4B34", "#8C6449"
    paper = "#ECE8E0"
    line = "#9A9EA4"
    kids = [
        P(rrect(2, 6, 60, 77, 4.5), linear(2, 6, 62, 83, (0, board_hi), (1, shade(board, -0.15)))),
        P(rect(8.4, 15.4, 49, 64), "#000000", alpha=0.18),
        P(rect(7, 14, 49, 64), paper),
        # mini photo of the missing girl, clipped to the file
        P(rect(11, 19, 13.5, 15.5), "#5F7F7D"),
        P("M12.2,34.5 C12.8,30.2 15.2,29 17.8,29 C20.4,29 22.8,30.2 23.4,34.5z", "#E2B13A"),
        P(ellipse(17.8, 25.6, 3.4, 4.0), "#E0B391"),
        P("M14.2,25.4 C14,21.4 16,20.2 17.8,20.2 C20,20.2 21.8,21.6 21.4,25.4 C20.6,23.2 19,22.6 17.6,22.8 C16.2,23 15,23.8 14.2,25.4z", "#3A2620"),
        P(rect(11, 19, 13.5, 15.5), None, stroke="#FFFFFF", sw=0.8),
    ]
    for i, w in enumerate((22, 17, 20, 12)):
        kids.append(P(f"M28,{21 + i * 3.6} h{w}", None, stroke=shade(line, -0.25) if i == 0 else line, sw=1.0 if i == 0 else 0.8))
    for i, w in enumerate((40, 36, 41, 30, 38, 41, 25, 35, 39, 18)):
        kids.append(P(f"M11,{40 + i * 3.6} h{w}", None, stroke=line, sw=0.8, salpha=0.9))
    # red "MISSING" stamp
    stamp = "#B8343F"
    kids.append(G([
        P(rrect(-14, -5.2, 28, 10.4, 1.6), None, stroke=stamp, sw=1.0, salpha=0.85),
        P(text_path("im_fell_english", "MISSING", 0, 2.6, 7.2, anchor="middle", spacing=0.4), stamp, alpha=0.85),
    ], rotate=-12, sx=0.84, sy=0.84, tx=34.5, ty=67))
    kids += [
        P(rrect(21, 2.6, 22, 10, 2.4), linear(21, 2, 43, 12, (0, "#5A616B"), (1, "#353A41"))),
        P(rrect(25.5, 0.4, 13, 5.2, 1.8), "#A3AAB3"),
        P(circle(32, 3.0, 1.15), "#2B2F35"),
    ]
    return Vec("art_clipboard", 64, 84, 64, 84, kids)


def newspaper() -> Vec:
    ink = "#2B2724"
    col = "#8D877D"
    paper = poly([(1.5, 2.2), (99, 0.6), (98.4, 64.6), (0.4, 65.6)])
    kids = [
        P(paper, linear(0, 0, 100, 66, (0, "#E2DCCD"), (1, "#CFC8B8"))),
        P(rect(0, 31.6, 100, 3.6), linear(0, 31.6, 0, 35.2, (0, "#000000", 0.0), (0.5, "#000000", 0.12), (1, "#000000", 0.0))),
        P(text_path("im_fell_english", "The Gullwater Post", 50, 10.4, 8.6, anchor="middle"), ink),
        P("M6,13.2 h88 M6,14.8 h88", None, stroke=ink, sw=0.45),
        P(text_path("im_fell_english", "Student missing after", 50, 21.6, 6.0, anchor="middle"), ink),
        P(text_path("im_fell_english", "lakeside bonfire", 50, 28.0, 6.0, anchor="middle"), ink),
        # photo: the pier at dusk, in newsprint greys
        P(rect(6, 37, 34, 23), "#8E897F"),
        P(rect(6, 37, 34, 9), "#A39E93"),
        P(poly([(6, 52), (40, 49), (40, 60), (6, 60)]), "#5F5B54"),
        P(poly([(9, 60), (24, 47.6), (26, 47.6), (17, 60)]), "#3F3C37"),
        P(rect(24.2, 43.6, 0.8, 4.4), "#3F3C37"),
        P(circle(24.6, 43.2, 1.1), "#E8E2D2"),
    ]
    for c in (0, 1):
        x = 44 + c * 26
        for i in range(8):
            w = 23 if i not in (3, 7) else 14 + c * 4
            kids.append(P(f"M{x},{38 + i * 3.0} h{w}", None, stroke=col, sw=0.7))
    return Vec("art_newspaper", 100, 66, 100, 66, kids)


def notepad_cup() -> Vec:
    ink = "#2A2B3A"
    kids = [
        P(ellipse(40, 40, 14.6, 4.2), "#14171B"),
    ]

    def stick(x0, y0, x1, y1, w, body, tip=None, cap=None):
        ang = math.atan2(y1 - y0, x1 - x0)
        nx, ny = -math.sin(ang) * w / 2, math.cos(ang) * w / 2
        out = [P(poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)]), body)]
        if tip:
            tx, ty = x1 + math.cos(ang) * 5, y1 + math.sin(ang) * 5
            out.append(P(poly([(x1 + nx, y1 + ny), (tx, ty), (x1 - nx, y1 - ny)]), tip))
            out.append(P(circle(tx - math.cos(ang) * 1.2, ty - math.sin(ang) * 1.2, 0.9), "#2B2B2B"))
        if cap:
            out.append(P(poly([(x1 + nx, y1 + ny), (x1 + nx - math.cos(ang) * 6, y1 + ny - math.sin(ang) * 6),
                               (x1 - nx - math.cos(ang) * 6, y1 - ny - math.sin(ang) * 6), (x1 - nx, y1 - ny)]), cap))
        return out

    kids += stick(36, 42, 30, 9, 3.6, "#E2B13A", tip="#E8C9A0")
    kids += stick(41.5, 42, 43.5, 7, 3.0, "#2F6FD0", cap="#1E4C96")
    kids += stick(46.5, 42, 53.5, 13, 4.2, "#2A2A2E", cap="#8A9099")
    kids += stick(38.5, 42, 36.5, 16, 2.8, "#C8413B", cap="#8E2A26")
    cup = "M25.4,40 L27.4,73 C27.8,76.4 33,78.2 40,78.2 C47,78.2 52.2,76.4 52.6,73 L54.6,40 C54.6,42.4 48,44.2 40,44.2 C32,44.2 25.4,42.4 25.4,40z"
    kids += [
        P(cup, linear(25, 0, 55, 0, (0, "#272C32"), (0.62, "#4A5059"), (1, "#30353C"))),
        P(ellipse(40, 40, 14.6, 4.2), None, stroke="#6A727C", sw=0.9, salpha=0.8),
    ]
    pad = []
    pad.append(P(rect(0.8, 0.8, 32, 44), "#000000", alpha=0.2))
    pad.append(P(rect(0, 0, 32, 44), "#F3E6A8"))
    pad.append(P(rect(0, 0, 32, 4.2), "#9C3B36"))
    for i in range(8):
        pad.append(P(f"M1.5,{11.6 + i * 4.3} h29", None, stroke="#9DB2C4", sw=0.4, salpha=0.9))
    pad.append(P("M5.5,5 v38.5", None, stroke="#D98C86", sw=0.4))
    pad.append(P(text_path("caveat_medium", "ask about:", 7, 10.6, 5.4), ink))
    pad.append(P(text_path("caveat_regular", "boathouse key", 7, 15.4, 4.4), ink))
    pad.append(P(text_path("caveat_regular", "night_heron", 7, 19.7, 4.4), ink))
    pad.append(P("M6.6,18.4 h19", None, stroke=ink, sw=0.55))
    pad.append(P(text_path("caveat_regular", "9:40 pm?", 7, 24.0, 4.4), ink))
    pad.append(P(text_path("caveat_regular", "the dock mark", 7, 28.3, 4.4), ink))
    kids.append(G(pad, rotate=-5, px=16, py=22, tx=1.5, ty=46))
    return Vec("art_notepad", 64, 92, 64, 92, kids)


def mug() -> Vec:
    body = "M6,12 L6,46 C6,51 14,53.4 23,53.4 C32,53.4 40,51 40,46 L40,12z"
    cream = linear(6, 0, 40, 0, (0, "#BDB4A3"), (0.55, "#F1EADD"), (1, "#CBC2B0"))
    navy = linear(6, 0, 40, 0, (0, "#1F2A40"), (0.55, "#3A4C6E"), (1, "#26324A"))
    return Vec("art_mug", 50, 56, 50, 56, [
        P(ellipse(40.5, 29, 9.6, 11.6) + " " + ellipse(40.5, 29, 5.4, 7.4),
          linear(31, 0, 50, 0, (0, "#D9D1C1"), (1, "#B1A895")), evenodd=True),
        P(body, cream),
        G([P(rect(0, 18, 50, 5.2), navy), P(rect(0, 25, 50, 1.4), navy)], clip=body),
        P(ellipse(23, 12, 17, 4.6), "#E9E2D3"),
        P(ellipse(23, 12.5, 15.0, 3.5), linear(8, 12, 38, 12, (0, "#2A190F"), (1, "#4A2E1E"))),
        P(ellipse(27.5, 12.2, 5.6, 1.0), "#7A5640", alpha=0.45),
        P("M9,15 C9,26 9.6,38 11,46", None, stroke="#FFFFFF", sw=1.2, salpha=0.35),
    ])


def crumpled_note() -> Vec:
    outline = poly([(4, 8), (12, 3), (22, 5.4), (31, 1.4), (42, 6), (44.4, 17), (41, 27), (44, 36), (33, 40.6),
                    (22, 37.4), (12, 41), (3, 34), (5.4, 23), (1.2, 15)])
    paper = "#D8CDB6"
    kids = [
        P(outline, paper),
        G([
            P(poly([(4, 8), (22, 5.4), (14, 19)]), "#E6DDCA", alpha=0.9),
            P(poly([(22, 5.4), (42, 6), (30, 18)]), "#CBBFA6", alpha=0.85),
            P(poly([(14, 19), (30, 18), (24, 30)]), "#E2D8C3", alpha=0.8),
            P(poly([(30, 18), (44.4, 17), (41, 27), (24, 30)]), "#C6B99E", alpha=0.75),
            P(poly([(1.2, 15), (14, 19), (5.4, 23)]), "#C9BCA2", alpha=0.8),
            P(poly([(5.4, 23), (24, 30), (12, 41), (3, 34)]), "#DDD3BE", alpha=0.85),
            P(poly([(24, 30), (44, 36), (33, 40.6), (22, 37.4)]), "#CFC3A9", alpha=0.8),
        ], clip=outline),
        P("M4,8 L14,19 L5.4,23 M22,5.4 L14,19 L30,18 L42,6 M30,18 L24,30 L12,41 M24,30 L41,27 M24,30 L22,37.4",
          None, stroke="#AD9F83", sw=0.45, salpha=0.8),
        G([
            P(text_path("caveat_medium", "same place.", 0, 0, 6.2), "#3A3530", alpha=0.85),
            P(text_path("caveat_medium", "midnight.", 3, 7.2, 6.2), "#3A3530", alpha=0.85),
        ], rotate=-7, tx=8.4, ty=19.6),
    ]
    return Vec("art_note", 46, 42, 46, 42, kids)


def all_objects() -> list[Vec]:
    return [headphones(), clipboard(), newspaper(), notepad_cup(), mug(), crumpled_note()]
