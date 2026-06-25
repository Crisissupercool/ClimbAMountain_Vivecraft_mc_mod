#!/usr/bin/env python3
"""Generate the rock-nub position models + blockstate.

The nub can sit in a 3x3 grid on a wall face (h_offset/v_offset 0..2). Each grid
slot is the same small box translated to a different (x, y) in the FACING=NORTH
cell; the four wall directions are produced by the blockstate's y-rotation. This
script emits the 9 model files and the 36-entry blockstate so the geometry stays
in lockstep with the VoxelShapes computed in RockNubBlock.makeShapes().

Re-run after changing the box dimensions here OR in RockNubBlock (keep them equal).
"""
import json
import os

NS = "climbamountainvr"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", NS)

# Box geometry, in pixels, authored for FACING=NORTH (wall on +Z; box hugs z=16).
DEPTH = 4
X_BY_H = [(2, 6), (6, 10), (10, 14)]      # h = 0,1,2  -> left / centre / right
Y_BY_V = [(1, 6), (5, 10), (9, 14)]       # v = 0,1,2  -> low / mid / high
Z = (16 - DEPTH, 16)

Y_ROTATION = {"north": None, "east": 90, "south": 180, "west": 270}


def model(h, v):
    x0, x1 = X_BY_H[h]
    y0, y1 = Y_BY_V[v]
    z0, z1 = Z
    return {
        "parent": "block/block",
        "textures": {
            "nub": f"{NS}:block/rock_nub",
            "particle": f"{NS}:block/rock_nub",
        },
        "elements": [
            {
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "faces": {
                    "north": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#nub"},
                    "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#nub"},
                    "east":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#nub"},
                    "west":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#nub"},
                    "up":    {"uv": [x0, z0, x1, z1], "texture": "#nub"},
                    "down":  {"uv": [x0, z0, x1, z1], "texture": "#nub"},
                },
            }
        ],
    }


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent="\t")
        f.write("\n")
    print("wrote", os.path.relpath(path, os.path.join(os.path.dirname(__file__), "..")))


def main():
    for h in range(3):
        for v in range(3):
            write(os.path.join(RES, "models", "block", f"rock_nub_h{h}_v{v}.json"), model(h, v))

    # Multipart: one part per (facing, slot). The slot's nub renders when its boolean
    # is true AND the block faces that wall, so any subset of the 9 nubs can coexist.
    multipart = []
    for facing, rot in Y_ROTATION.items():
        for h in range(3):
            for v in range(3):
                apply = {"model": f"{NS}:block/rock_nub_h{h}_v{v}"}
                if rot is not None:
                    apply["y"] = rot
                multipart.append({
                    "when": {"facing": facing, f"slot_{h}_{v}": "true"},
                    "apply": apply,
                })

    write(os.path.join(RES, "blockstates", "rock_nub.json"), {"multipart": multipart})


if __name__ == "__main__":
    main()
