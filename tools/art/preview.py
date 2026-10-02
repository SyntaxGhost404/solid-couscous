"""Contact-sheet previews of generated art, rendered with headless Chromium."""
from __future__ import annotations

import glob
import math
import os
import shutil
import subprocess
import tempfile

from vec import Vec, to_svg


def _chromium() -> str:
    """A Chrome/Chromium binary: $CHROMIUM, then PATH, then Playwright's browser caches."""
    if os.environ.get("CHROMIUM"):
        return os.environ["CHROMIUM"]
    for name in ("chromium", "chromium-browser", "google-chrome", "headless_shell"):
        found = shutil.which(name)
        if found:
            return found
    caches = [os.environ.get("PLAYWRIGHT_BROWSERS_PATH", ""), os.path.expanduser("~/.cache/ms-playwright")]
    for cache in filter(None, caches):
        hits = sorted(glob.glob(os.path.join(cache, "chromium_headless_shell-*", "chrome-linux", "headless_shell")))
        if hits:
            return hits[-1]
    raise RuntimeError("No Chromium found for previews; set CHROMIUM to a Chrome or Chromium binary")


def screenshot_html(html: str, out_png: str, width: int, height: int) -> None:
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False) as fh:
        fh.write(html)
        path = fh.name
    try:
        subprocess.run(
            [
                _chromium(),
                "--headless",
                "--no-sandbox",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                f"--window-size={width},{height}",
                f"--screenshot={out_png}",
                f"file://{path}",
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    finally:
        os.unlink(path)


def sheet(vecs: list[Vec], out_png: str, scale: float = 2.0, cols: int = 4, bg: str = "#11161C") -> None:
    cell_w = max(v.w for v in vecs) * scale + 24
    cell_h = max(v.h for v in vecs) * scale + 40
    rows = math.ceil(len(vecs) / cols)
    width, height = int(cell_w * cols), int(cell_h * rows)
    cells = "".join(
        f'<div style="width:{cell_w}px;height:{cell_h}px;display:flex;flex-direction:column;'
        f'align-items:center;justify-content:center;gap:6px">{to_svg(v, scale)}'
        f'<span style="font:12px monospace;color:#8a929c">{v.name}</span></div>'
        for v in vecs
    )
    html = (
        f'<html><body style="margin:0;background:{bg};display:flex;flex-wrap:wrap;width:{width}px">'
        f"{cells}</body></html>"
    )
    screenshot_html(html, out_png, width, height)
