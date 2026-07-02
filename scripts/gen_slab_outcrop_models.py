#!/usr/bin/env python3
"""Generate the slab-outcrop level models + blockstate.

The outcrop is one wide flat ledge per cell, at one of three heights (level
0..2, low/mid/high). Each level is the same box translated to a different y in
the FACING=NORTH cell; the four wall directions are produced by the
blockstate's y-rotation. Because a variants blockstate can only rotate a model
(never translate it), each level gets its own model file: 3 models + a
12-variant blockstate. This keeps the geometry in lockstep with the
VoxelShapes computed in SlabOutcropBlock.makeShapes().

Re-run after changing the box dimensions here OR in SlabOutcropBlock (keep
them equal).
"""
import json
import os

NS = "climbamountainvr"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", NS)

# Box geometry, in pixels, authored for FACING=NORTH (wall on +Z; ledge hugs z=16).
# 14 wide x 3 thick x 5 deep. Level 2's top sits flush at y=16 so it doubles as a
# step aligned with the floor of the block above.
X = (1, 15)
Y_BY_LEVEL = [(1, 4), (7, 10), (13, 16)]  # level = 0,1,2 -> low / mid / high
Z = (11, 16)

Y_ROTATION = {"north": None, "east": 90, "south": 180, "west": 270}


def model(level):
    x0, x1 = X
    y0, y1 = Y_BY_LEVEL[level]
    z0, z1 = Z
    return {
        "parent": "block/block",
        "textures": {
            "ledge": f"{NS}:block/slab_outcrop",
            "particle": f"{NS}:block/slab_outcrop",
        },
        "elements": [
            {
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "faces": {
                    "north": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#ledge"},
                    "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#ledge"},
                    "east":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#ledge"},
                    "west":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#ledge"},
                    "up":    {"uv": [x0, z0, x1, z1], "texture": "#ledge"},
                    "down":  {"uv": [x0, z0, x1, z1], "texture": "#ledge"},
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
    for level in range(3):
        write(os.path.join(RES, "models", "block", f"slab_outcrop_l{level}.json"), model(level))

    # Variants (not multipart): exactly one (facing, level) combination is ever
    # active, so a plain variants map keyed on both properties is the right form.
    variants = {}
    for facing, rot in Y_ROTATION.items():
        for level in range(3):
            apply = {"model": f"{NS}:block/slab_outcrop_l{level}"}
            if rot is not None:
                apply["y"] = rot
            variants[f"facing={facing},level={level}"] = apply

    write(os.path.join(RES, "blockstates", "slab_outcrop.json"), {"variants": variants})


if __name__ == "__main__":
    main()
