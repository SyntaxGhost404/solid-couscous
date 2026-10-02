"""Tiny vector-art model that renders to both SVG (for previews) and Android VectorDrawable XML.

Art is described once as nested groups of paths. Paths take SVG path data; fills are a colour
string or a gradient. Group transforms follow VectorDrawable semantics exactly
(translate(pivot + t) * rotate * scale * translate(-pivot)) so both outputs match.
"""
from __future__ import annotations

import math
import re
from dataclasses import dataclass, field
from typing import Optional, Sequence, Union
from xml.sax.saxutils import escape


def f(v: float) -> str:
    """Compact number formatting (2 decimals, no trailing zeros)."""
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# ---------------------------------------------------------------- colours

def hex_rgb(c: str) -> tuple[int, int, int]:
    c = c.lstrip("#")
    return int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16)


def rgb_hex(r: float, g: float, b: float) -> str:
    return "#%02X%02X%02X" % tuple(max(0, min(255, round(x))) for x in (r, g, b))


def mix(a: str, b: str, t: float) -> str:
    ra, ga, ba = hex_rgb(a)
    rb, gb, bb = hex_rgb(b)
    return rgb_hex(ra + (rb - ra) * t, ga + (gb - ga) * t, ba + (bb - ba) * t)


def shade(c: str, t: float) -> str:
    """t < 0 darkens towards black, t > 0 lightens towards white."""
    return mix(c, "#000000", -t) if t < 0 else mix(c, "#FFFFFF", t)


# ---------------------------------------------------------------- paint

@dataclass
class Grad:
    kind: str  # "linear" | "radial"
    stops: Sequence[tuple]  # (offset, "#RRGGBB", alpha)
    x1: float = 0
    y1: float = 0
    x2: float = 0
    y2: float = 0
    cx: float = 0
    cy: float = 0
    r: float = 1


def linear(x1, y1, x2, y2, *stops) -> Grad:
    return Grad("linear", [s if len(s) == 3 else (s[0], s[1], 1.0) for s in stops], x1=x1, y1=y1, x2=x2, y2=y2)


def radial(cx, cy, r, *stops) -> Grad:
    return Grad("radial", [s if len(s) == 3 else (s[0], s[1], 1.0) for s in stops], cx=cx, cy=cy, r=r)


Paint = Union[str, Grad, None]


@dataclass
class P:
    d: str
    fill: Paint = None
    alpha: float = 1.0
    stroke: Optional[str] = None
    sw: float = 0.0
    salpha: float = 1.0
    cap: str = "round"
    join: str = "round"
    evenodd: bool = False


@dataclass
class G:
    children: list
    rotate: float = 0.0
    px: float = 0.0
    py: float = 0.0
    tx: float = 0.0
    ty: float = 0.0
    sx: float = 1.0
    sy: float = 1.0
    clip: Optional[str] = None
    name: Optional[str] = None


@dataclass
class Vec:
    name: str
    w: float  # intrinsic size in dp
    h: float
    vw: float  # viewport size in drawing units
    vh: float
    children: list = field(default_factory=list)
    tint_note: str = ""


# ---------------------------------------------------------------- path builders

def rect(x, y, w, h) -> str:
    return f"M{f(x)},{f(y)}h{f(w)}v{f(h)}h{f(-w)}z"


def rrect(x, y, w, h, r, r2=None) -> str:
    """Rounded rect; r2 (optional) gives a different radius to the bottom corners."""
    r = min(r, w / 2, h / 2)
    rb = r if r2 is None else min(r2, w / 2, h / 2)
    return (
        f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(r)}"
        f"v{f(h - r - rb)}a{f(rb)},{f(rb)} 0 0 1 {f(-rb)},{f(rb)}h{f(-(w - 2 * rb))}"
        f"a{f(rb)},{f(rb)} 0 0 1 {f(-rb)},{f(-rb)}v{f(-(h - r - rb))}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(-r)}z"
    )


def ellipse(cx, cy, rx, ry) -> str:
    return (
        f"M{f(cx - rx)},{f(cy)}a{f(rx)},{f(ry)} 0 1 0 {f(2 * rx)},0"
        f"a{f(rx)},{f(ry)} 0 1 0 {f(-2 * rx)},0z"
    )


def circle(cx, cy, r) -> str:
    return ellipse(cx, cy, r, r)


def poly(pts, close=True) -> str:
    s = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts)
    return s + ("z" if close else "")


def smooth(pts, closed=True, tension=1.0) -> str:
    """Catmull-Rom spline through pts, as cubic Beziers."""
    n = len(pts)
    if n < 3:
        return poly(pts, closed)
    out = [f"M{f(pts[0][0])},{f(pts[0][1])}"]
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if (closed or i > 0) else pts[0]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if (closed or i + 2 < n) else pts[-1]
        k = tension / 6.0
        c1 = (p1[0] + (p2[0] - p0[0]) * k, p1[1] + (p2[1] - p0[1]) * k)
        c2 = (p2[0] - (p3[0] - p1[0]) * k, p2[1] - (p3[1] - p1[1]) * k)
        out.append(f"C{f(c1[0])},{f(c1[1])} {f(c2[0])},{f(c2[1])} {f(p2[0])},{f(p2[1])}")
    return " ".join(out) + ("z" if closed else "")


def mirror(pts, cx):
    """Mirror points around x = cx (returned in reverse order, for building symmetric outlines)."""
    return [(2 * cx - x, y) for x, y in reversed(pts)]


def rot_pts(pts, deg, ox, oy):
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    return [(ox + (x - ox) * ca - (y - oy) * sa, oy + (x - ox) * sa + (y - oy) * ca) for x, y in pts]


def join(*ds: str) -> str:
    return " ".join(d for d in ds if d)


# ---------------------------------------------------------------- writers

def _svg_paint(paint: Paint, defs: list, ids: list) -> str:
    if paint is None:
        return "none"
    if isinstance(paint, str):
        return paint
    gid = f"{ids[0]}g{len(ids)}"
    ids.append(gid)
    stops = "".join(
        f'<stop offset="{f(o)}" stop-color="{c}" stop-opacity="{f(a)}"/>' for o, c, a in paint.stops
    )
    if paint.kind == "linear":
        defs.append(
            f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{f(paint.x1)}" y1="{f(paint.y1)}" '
            f'x2="{f(paint.x2)}" y2="{f(paint.y2)}">{stops}</linearGradient>'
        )
    else:
        defs.append(
            f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{f(paint.cx)}" cy="{f(paint.cy)}" '
            f'r="{f(paint.r)}">{stops}</radialGradient>'
        )
    return f"url(#{gid})"


def _svg_node(n, defs, ids) -> str:
    if isinstance(n, P):
        attrs = [f'd="{n.d}"', f'fill="{_svg_paint(n.fill, defs, ids)}"']
        if n.fill is not None and n.alpha != 1:
            attrs.append(f'fill-opacity="{f(n.alpha)}"')
        if n.evenodd:
            attrs.append('fill-rule="evenodd"')
        if n.stroke:
            attrs += [
                f'stroke="{n.stroke}"',
                f'stroke-width="{f(n.sw)}"',
                f'stroke-linecap="{n.cap}"',
                f'stroke-linejoin="{n.join}"',
            ]
            if n.salpha != 1:
                attrs.append(f'stroke-opacity="{f(n.salpha)}"')
        return f"<path {' '.join(attrs)}/>"
    assert isinstance(n, G)
    tr = (
        f"translate({f(n.tx + n.px)},{f(n.ty + n.py)}) rotate({f(n.rotate)}) "
        f"scale({f(n.sx)},{f(n.sy)}) translate({f(-n.px)},{f(-n.py)})"
    )
    inner = "".join(_svg_node(c, defs, ids) for c in n.children)
    if n.clip:
        cid = f"{ids[0]}c{len(ids)}"
        ids.append(cid)
        defs.append(f'<clipPath id="{cid}"><path d="{n.clip}"/></clipPath>')
        inner = f'<g clip-path="url(#{cid})">{inner}</g>'
    return f'<g transform="{tr}">{inner}</g>'


def to_svg(v: Vec, scale: float = 1.0) -> str:
    defs: list = []
    ids: list = [re.sub(r"[^A-Za-z0-9_]", "_", v.name)]  # ids[0] namespaces this SVG's defs on shared pages
    body = "".join(_svg_node(c, defs, ids) for c in v.children)
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(v.w * scale)}" height="{f(v.h * scale)}" '
        f'viewBox="0 0 {f(v.vw)} {f(v.vh)}"><defs>{"".join(defs)}</defs>{body}</svg>'
    )


def _vd_color(c: str, alpha: float = 1.0) -> str:
    a = round(max(0.0, min(1.0, alpha)) * 255)
    return "#%02X%s" % (a, c.lstrip("#").upper())


def _vd_gradient(paint: Grad, ind: str) -> str:
    items = "".join(
        f'{ind}    <item android:offset="{f(o)}" android:color="{_vd_color(c, a)}"/>\n' for o, c, a in paint.stops
    )
    if paint.kind == "linear":
        head = (
            f'<gradient android:type="linear" android:startX="{f(paint.x1)}" android:startY="{f(paint.y1)}" '
            f'android:endX="{f(paint.x2)}" android:endY="{f(paint.y2)}">'
        )
    else:
        head = (
            f'<gradient android:type="radial" android:centerX="{f(paint.cx)}" android:centerY="{f(paint.cy)}" '
            f'android:gradientRadius="{f(paint.r)}">'
        )
    return f"{ind}  {head}\n{items}{ind}  </gradient>\n"


MAX_PATH = 28000  # binary XML stores strings up to 32767 bytes


def _vd_chunks(d: str) -> list[str]:
    """Splits overlong path data at newlines (glyph boundaries), keeping each chunk encodable."""
    if len(d) <= MAX_PATH:
        return [d]
    chunks, cur = [], ""
    for part in d.split("\n"):
        if cur and len(cur) + len(part) + 1 > MAX_PATH:
            chunks.append(cur)
            cur = part
        else:
            cur = f"{cur} {part}" if cur else part
    if cur:
        chunks.append(cur)
    if any(len(c) > MAX_PATH for c in chunks):
        raise ValueError("path data too long to split safely; add newline split points")
    return chunks


def _vd_node(n, ind: str) -> str:
    if isinstance(n, P) and len(n.d) > MAX_PATH:
        return "".join(_vd_node(P(**{**n.__dict__, "d": c}), ind) for c in _vd_chunks(n.d))
    if isinstance(n, P):
        attrs = [f'android:pathData="{escape(n.d.replace(chr(10), " "))}"']
        gradient = None
        if isinstance(n.fill, str):
            attrs.append(f'android:fillColor="{_vd_color(n.fill)}"')
            if n.alpha != 1:
                attrs.append(f'android:fillAlpha="{f(n.alpha)}"')
        elif isinstance(n.fill, Grad):
            gradient = n.fill
            if n.alpha != 1:
                attrs.append(f'android:fillAlpha="{f(n.alpha)}"')
        if n.evenodd:
            attrs.append('android:fillType="evenOdd"')
        if n.stroke:
            attrs += [
                f'android:strokeColor="{_vd_color(n.stroke)}"',
                f'android:strokeWidth="{f(n.sw)}"',
                f'android:strokeLineCap="{n.cap}"',
                f'android:strokeLineJoin="{n.join}"',
            ]
            if n.salpha != 1:
                attrs.append(f'android:strokeAlpha="{f(n.salpha)}"')
        a = f"\n{ind}    ".join(attrs)
        if gradient is None:
            return f"{ind}<path\n{ind}    {a} />\n"
        return (
            f"{ind}<path\n{ind}    {a}>\n{ind}  <aapt:attr name=\"android:fillColor\">\n"
            f"{_vd_gradient(gradient, ind + '  ')}{ind}  </aapt:attr>\n{ind}</path>\n"
        )
    assert isinstance(n, G)
    attrs = []
    if n.name:
        attrs.append(f'android:name="{n.name}"')
    for key, val, default in (
        ("rotation", n.rotate, 0),
        ("pivotX", n.px, 0),
        ("pivotY", n.py, 0),
        ("translateX", n.tx, 0),
        ("translateY", n.ty, 0),
        ("scaleX", n.sx, 1),
        ("scaleY", n.sy, 1),
    ):
        if val != default:
            attrs.append(f'android:{key}="{f(val)}"')
    inner = ""
    if n.clip:
        inner += f'{ind}  <clip-path android:pathData="{escape(n.clip)}" />\n'
    inner += "".join(_vd_node(c, ind + "  ") for c in n.children)
    a = (" " + " ".join(attrs)) if attrs else ""
    return f"{ind}<group{a}>\n{inner}{ind}</group>\n"


def to_vd(v: Vec) -> str:
    body = "".join(_vd_node(c, "    ") for c in v.children)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/art; edit the generator, not this file. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        f'    android:width="{f(v.w)}dp"\n    android:height="{f(v.h)}dp"\n'
        f'    android:viewportWidth="{f(v.vw)}"\n    android:viewportHeight="{f(v.vh)}">\n'
        f"{body}</vector>\n"
    )
