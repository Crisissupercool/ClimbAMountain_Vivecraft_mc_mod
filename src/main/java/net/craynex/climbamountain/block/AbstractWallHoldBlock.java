package net.craynex.climbamountain.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Shared behaviour for every wall-mounted handhold block (rock nub, slab outcrop,
 * vertical seam). All of them follow the same conventions, first proven on the
 * rock nub:
 *
 * - FACING = the direction the hold points OUT from the wall = ctx.getSide() at
 *   placement. Vanilla ItemPlacementContext.getBlockPos() already offsets into
 *   the air cell against the clicked face, so position is never offset manually.
 * - Base geometry is authored for FACING=NORTH (wall on the cell's +Z edge, hold
 *   hugging z=16 and protruding toward -Z). The other three walls reuse the same
 *   boxes rotated around the cell centre by the SAME y-rotation the blockstate
 *   applies to the model (north=0 / east=90 / south=180 / west=270, the furnace
 *   convention) via rotateBoxY -- so collision and visuals can never drift.
 * - Waterloggable, with the standard fluid-state plumbing.
 * - rotate/mirror only remap FACING, not any sub-cell slot properties (known
 *   limitation; only matters for structure-block / worldgen rotation).
 */
public abstract class AbstractWallHoldBlock extends HorizontalFacingBlock implements Waterloggable {
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	protected AbstractWallHoldBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	// Index into the per-facing shape tables: north/south/west/east -> 0..3.
	protected static int facingIndex(Direction facing) {
		return switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 1;
			case WEST -> 2;
			case EAST -> 3;
			default -> 0;
		};
	}

	// Rotate a FACING=NORTH-authored box around the vertical axis through the cell
	// centre, matching the blockstate's y-rotation for that facing.
	protected static VoxelShape rotateBoxY(Box b, Direction facing) {
		return switch (facing) {
			case EAST -> VoxelShapes.cuboid(1.0 - b.maxZ, b.minY, b.minX, 1.0 - b.minZ, b.maxY, b.maxX);
			case SOUTH -> VoxelShapes.cuboid(1.0 - b.maxX, b.minY, 1.0 - b.maxZ, 1.0 - b.minX, b.maxY, 1.0 - b.minZ);
			case WEST -> VoxelShapes.cuboid(b.minZ, b.minY, 1.0 - b.maxX, b.maxZ, b.maxY, 1.0 - b.minX);
			default -> VoxelShapes.cuboid(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		};
	}

	// Map a 0..1 fraction of the cell to a 0..2 grid index, clamped.
	protected static int slot(double fraction) {
		int i = (int) (fraction * 3.0);
		if (i < 0) {
			return 0;
		}
		if (i > 2) {
			return 2;
		}
		return i;
	}

	/**
	 * Which 3x3 slot the player's crosshair is pointing at, in face-local terms:
	 * {h, v} with h = column 0..2 left-to-right as seen looking AT the wall, and
	 * v = row 0..2 bottom-to-top.
	 *
	 * Vertical is always world Y; horizontal is world X (north/south walls) or
	 * world Z (east/west). We match the hit fraction to the hold's WORLD position,
	 * so the +axis walls (south/west) mirror the column with (2 - slot) -- the
	 * blockstate y-rotation flips the authored x-axis on those walls.
	 */
	protected static int[] aimedSlot(ItemPlacementContext ctx, Direction facing) {
		Vec3d hit = ctx.getHitPos();
		double fx = hit.x - Math.floor(hit.x);
		double fy = hit.y - Math.floor(hit.y);
		double fz = hit.z - Math.floor(hit.z);

		int v = slot(fy);
		int h;
		if (facing == Direction.NORTH) {
			h = slot(fx);
		} else if (facing == Direction.SOUTH) {
			h = 2 - slot(fx);
		} else if (facing == Direction.EAST) {
			h = slot(fz);
		} else {
			h = 2 - slot(fz);
		}
		return new int[] { h, v };
	}
}
