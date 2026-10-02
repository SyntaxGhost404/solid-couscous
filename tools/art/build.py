"""Regenerates every vector drawable used by the game.

    python3 tools/art/build.py            # write res/drawable/*.xml
    python3 tools/art/build.py --preview  # also render contact sheets to tools/art/out/
"""
from __future__ import annotations

import os
import sys

import icons
import launcher
import objects
import portraits
import title
import wallpapers
from vec import to_vd

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res", "drawable")

GROUPS = {
    "portraits": portraits.all_portraits,
    "objects": objects.all_objects,
    "title": title.all_title,
    "wallpapers": wallpapers.all_wallpapers,
    "icons": icons.all_icons,
    "launcher": launcher.all_launcher,
}


def main() -> None:
    os.makedirs(RES, exist_ok=True)
    count = 0
    for build in GROUPS.values():
        for vec in build():
            with open(os.path.join(RES, vec.name + ".xml"), "w") as fh:
                fh.write(to_vd(vec))
            count += 1
    print(f"wrote {count} vector drawables to {os.path.normpath(RES)}")
    if "--preview" in sys.argv:
        import preview

        out = os.path.join(HERE, "out")
        os.makedirs(out, exist_ok=True)
        for name, build in GROUPS.items():
            vecs = build()
            scale = 1.0 if name == "wallpapers" else 2.0
            preview.sheet(vecs, os.path.join(out, name + ".png"), scale=scale, cols=4)
        print(f"previews in {out}")


if __name__ == "__main__":
    main()
