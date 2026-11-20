# -*- coding: utf-8 -*-
import zlib, struct, math, os
S = 192
px = [[(0,0,0,0) for _ in range(S)] for _ in range(S)]
def heart(x, y):
    x = (x - S/2) / (S/2.0) * 1.15
    y = (y - S/2) / (S/2.0) * 1.15
    a = x*x + y*y - 0.62
    return a*a*a - x*x*y*y*y <= 0
for y in range(S):
    for x in range(S):
        dx, dy = x - S/2, y - S/2
        d = math.hypot(dx, dy) / (S/2)
        if d <= 0.5:
            # rounded background
            pass
        r0 = S/2
        if abs(dx) > r0*0.72 and abs(dy) > r0*0.72:
            pass
        inside_bg = d <= 0.99
        if not inside_bg:
            continue
        # background vertical gradient
        t = y / float(S)
        rr = int(22 + 8*t); gg = int(10 + 4*t); bb = int(24 + 6*t)
        px[y][x] = (rr, gg, bb, 255)
for y in range(S):
    for x in range(S):
        if px[y][x][3] == 0:
            continue
        if heart(x, y):
            t = (y/S)
            rr = int(255 - 60*t); gg = int(90 - 50*t); bb = int(122 - 40*t)
            # crack: jagged vertical line
            cx = abs(x - S/2 + (8 * math.sin(y/9.0)))
            if cx < 3.0 and y > S*0.34 and y < S*0.86:
                px[y][x] = (18, 8, 16, 255)
            else:
                px[y][x] = (rr, gg, bb, 255)
raw = b""
for y in range(S):
    raw += b"\x00" + b"".join(bytes(px[y][x]) for x in range(S))
def chunk(t, d):
    c = struct.pack(">I", len(d)) + t + d
    return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", S, S, 8, 6, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(raw, 9))
png += chunk(b"IEND", b"")
out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/res/mipmap-xxhdpi/ic_launcher.png")
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, "wb").write(png)
print("icon", out, len(png))
