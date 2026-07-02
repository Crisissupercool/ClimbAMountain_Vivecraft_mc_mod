#!/usr/bin/env python3
"""Generate 16x16 placeholder stone-noise textures (stdlib only -- no PIL).

Each handhold block gets its own tint so the three hold types read differently
in-game even with placeholder art: slab outcrop slightly lighter (big friendly
jug), vertical seam slightly darker (shadowed crack). Deterministic (seeded)
so re-running produces identical bytes.
"""
import os
import random
import struct
import zlib

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "climbamountainvr", "textures", "block")

SIZE = 16

# name -> (base grey, tint applied to r/g/b)
TEXTURES = {
    "slab_outcrop": (138, (4, 4, 2)),     # lighter, faintly warm
    "vertical_seam": (105, (-4, -4, 0)),  # darker, faintly cool
}


def png(pixels):
    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c))

    raw = b""
    for y in range(SIZE):
        raw += b"\x00"  # filter type 0 (None) per scanline
        for x in range(SIZE):
            raw += bytes(pixels[y][x])

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw))
            + chunk(b"IEND", b""))


def main():
    os.makedirs(RES, exist_ok=True)
    for name, (base, (tr, tg, tb)) in TEXTURES.items():
        rng = random.Random(name)  # per-name seed -> distinct but stable noise
        pixels = []
        for y in range(SIZE):
            row = []
            for x in range(SIZE):
                n = rng.randint(-14, 14)
                g = base + n
                row.append((max(0, min(255, g + tr)),
                            max(0, min(255, g + tg)),
                            max(0, min(255, g + tb))))
            pixels.append(row)
        path = os.path.join(RES, f"{name}.png")
        with open(path, "wb") as f:
            f.write(png(pixels))
        print("wrote", os.path.relpath(path, os.path.join(os.path.dirname(__file__), "..")))


if __name__ == "__main__":
    main()
