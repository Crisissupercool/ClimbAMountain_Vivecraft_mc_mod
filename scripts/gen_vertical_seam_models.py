#!/usr/bin/env python3
"""Generate the vertical-seam column models + blockstate.

The seam is one thin full-height ridge per cell, in one of three columns
(column 0..2, left/centre/right on the same 4/8/12px grid centres as the rock
nub). Each column is the same box translated to a different x in the
FACING=NORTH cell; the four wall directions are produced by the blockstate's
y-rotation. Variants can only rotate a model, never translate it, so each
column gets its own model file: 3 models + a 12-variant blockstate, in
lockstep with the VoxelShapes computed in VerticalSeamBlock.makeShapes().

Re-run after changing the box dimensions here OR in VerticalSeamBlock (keep
them equal).
"""
import json
import os

NS = "climbamountainvr"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", NS)

# Box geometry, in pixels, authored for FACING=NORTH (wall on +Z; ridge hugs z=16).
# 4 wide x 16 tall x 3 deep -- spans the full cell height so vertically stacked
# seams weld into one continuous 2+ block rail.
X_BY_COLUMN = [(2, 6), (6, 10), (10, 14)]  # column = 0,1,2 -> left / centre / right
Y = (0, 16)
Z = (13, 16)

Y_ROTATION = {"north": None, "east": 90, "south": 180, "west": 270}


def model(column):
    x0, x1 = X_BY_COLUMN[column]
    y0, y1 = Y
    z0, z1 = Z
    return {
        "parent": "block/block",
        "textures": {
            "seam": f"{NS}:block/vertical_seam",
            "particle": f"{NS}:block/vertical_seam",
        },
        "elements": [
            {
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "faces": {
                    "north": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#seam"},
                    "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#seam"},
                    "east":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#seam"},
                    "west":  {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#seam"},
                    "up":    {"uv": [x0, z0, x1, z1], "texture": "#seam"},
                    "down":  {"uv": [x0, z0, x1, z1], "texture": "#seam"},
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
    for column in range(3):
        write(os.path.join(RES, "models", "block", f"vertical_seam_c{column}.json"), model(column))

    # Variants (not multipart): exactly one (facing, column) combination is ever
    # active, so a plain variants map keyed on both properties is the right form.
    variants = {}
    for facing, rot in Y_ROTATION.items():
        for column in range(3):
            apply = {"model": f"{NS}:block/vertical_seam_c{column}"}
            if rot is not None:
                apply["y"] = rot
            variants[f"facing={facing},column={column}"] = apply

    write(os.path.join(RES, "blockstates", "vertical_seam.json"), {"variants": variants})


if __name__ == "__main__":
    main()
