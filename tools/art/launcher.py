"""Adaptive launcher icon: a lantern burning at the end of a pier, over still water."""
from __future__ import annotations

from vec import G, P, Vec, circle, linear, poly, radial, rect, rrect

S = 108.0


def background() -> Vec:
    return Vec("ic_launcher_background", S, S, S, S, [
        P(rect(0, 0, S, S), linear(0, 0, 0, S, (0, "#0A1622"), (0.58, "#10283A"), (0.6, "#0C1D2A"), (1, "#050B11"))),
        P(rect(0, 0, S, S), radial(54, 50, 46, (0, "#FFC46B", 0.32), (0.45, "#FFC46B", 0.10), (1, "#FFC46B", 0.0))),
    ])


def _lantern(ink: str, light: str | None) -> list:
    kids = [
        P(rect(52.8, 40, 2.4, 25), ink),
        P(rect(55.2, 40.6, 7.2, 1.8), ink),
        P(poly([(58.4, 42.4), (63.4, 42.4), (64.4, 54.4), (57.4, 54.4)]), ink),
        P(poly([(57.6, 42.4), (64.2, 42.4), (60.9, 38.6)]), ink),
        P(rect(57.0, 54.4, 7.8, 1.6), ink),
    ]
    if light:
        kids.append(P(rect(59.2, 44.2, 3.4, 8.6), light))
    return kids


def foreground() -> Vec:
    ink = "#04080C"
    water = []
    for i, (w, a) in enumerate(((20, 0.85), (15, 0.7), (11, 0.55), (7, 0.4))):
        y = 66.5 + i * 3.4
        water.append(P(rect(60.9 - w / 2, y, w, 1.3), "#FFD28A", alpha=a))
    deck = poly([(30, 66), (78, 66), (80, 69.4), (28, 69.4)])
    return Vec("ic_launcher_foreground", S, S, S, S, [G([
        P(circle(60.9, 48.6, 13), radial(60.9, 48.6, 13, (0, "#FFE3A8", 0.55), (1, "#FFE3A8", 0.0))),
        *_lantern(ink, "#FFE0A0"),
        P(deck, ink),
        P(rect(34, 69, 2.6, 6) + " " + rect(51.6, 69, 2.6, 6) + " " + rect(70, 69, 2.6, 6), ink),
        *water,
    ], sx=1.14, sy=1.14, px=54, py=58)])


def monochrome() -> Vec:
    ink = "#FFFFFF"
    kids = _lantern(ink, None)
    kids.append(P(poly([(30, 66), (78, 66), (80, 69.4), (28, 69.4)]), ink))
    for i, w in enumerate((20, 14, 8)):
        kids.append(P(rect(60.9 - w / 2, 73 + i * 3.6, w, 1.6), ink))
    return Vec("ic_launcher_monochrome", S, S, S, S, [G(kids, sx=1.14, sy=1.14, px=54, py=58)])


def all_launcher() -> list[Vec]:
    return [background(), foreground(), monochrome()]


def preview_composite() -> Vec:
    shape = rrect(0, 0, S, S, 54)
    return Vec("launcher_preview", S, S, S, S, [G(background().children + foreground().children, clip=shape)])
