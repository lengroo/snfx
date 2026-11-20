#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Resize a square icon image into the five mipmap densities.

Usage: python3 tools/gen_icon_from_image.py <path-to-square-image.png>
"""
import os
import sys

from PIL import Image

if len(sys.argv) < 2:
    print("usage: python3 tools/gen_icon_from_image.py <image.png>")
    sys.exit(1)

src = sys.argv[1]
img = Image.open(src).convert("RGBA")
sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
base = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/res/")
for d, s in sizes.items():
    out = base + "mipmap-" + d
    os.makedirs(out, exist_ok=True)
    img.resize((s, s), Image.LANCZOS).save(out + "/ic_launcher.png", "PNG")
    print(d, s)
