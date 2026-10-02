"""Converts text to SVG path data using the project's bundled fonts (so vectors carry no font)."""
from __future__ import annotations

import os
from functools import lru_cache

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

FONT_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "res", "font")


@lru_cache(maxsize=None)
def _font(name: str) -> TTFont:
    return TTFont(os.path.join(FONT_DIR, name + ".ttf"))


def text_width(font: str, text: str, size: float, spacing: float = 0.0) -> float:
    tt = _font(font)
    cmap, hmtx, upem = tt.getBestCmap(), tt["hmtx"], tt["head"].unitsPerEm
    adv = sum(hmtx[cmap[ord(c)]][0] for c in text if ord(c) in cmap)
    return adv * size / upem + spacing * max(0, len(text) - 1)


def text_path(font: str, text: str, x: float, y: float, size: float, anchor: str = "start", spacing: float = 0.0) -> str:
    """Outline of [text] with its baseline at y. anchor: start | middle | end.

    Glyphs are separated by newlines, which the VectorDrawable writer uses as safe split points."""
    tt = _font(font)
    glyphs, cmap, hmtx, upem = tt.getGlyphSet(), tt.getBestCmap(), tt["hmtx"], tt["head"].unitsPerEm
    scale = size / upem
    width = text_width(font, text, size, spacing)
    if anchor == "middle":
        x -= width / 2
    elif anchor == "end":
        x -= width
    parts = []
    cursor = x
    for ch in text:
        name = cmap.get(ord(ch))
        if name is None:
            continue
        pen = SVGPathPen(glyphs, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, cursor, y)))
        if pen.getCommands():
            parts.append(pen.getCommands())
        cursor += hmtx[name][0] * scale + spacing
    return "\n".join(parts)
