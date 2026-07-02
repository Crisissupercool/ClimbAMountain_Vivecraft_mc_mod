package net.craynex.climbamountain.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * The "rail" -- a thin vertical ridge running the FULL height of the cell, used
 * for sideways traverses. The taxonomy calls for a seam ~2 blocks tall: that is
 * achieved by STACKING two of these (the ridge spans y 0..16, so vertically
 * adjacent seams weld into one continuous rail with no gap), the same emergence
 * trick as the rock-nub cluster welding into a slab. One block stays one cell,
 * which avoids the door/tall-plant two-half machinery (paired placement, joint
 * breaking) entirely -- and worldgen gets 1/2/3-tall seams for free.
 *
 * COLUMN (0=left / 1=centre / 2=right as seen looking at the wall) picks which
 * third of the cell the ridge sits in, from the horizontal component of the aim.
 * Offsetting columns between vertically-stacked seams gives diagonal crack
 * lines; offsetting between horizontally-adjacent ones varies a traverse.
 */
public class VerticalSeamBlock extends AbstractWallHoldBlock {
	public static final MapCodec<VerticalSeamBlock> CODEC = createCodec(VerticalSeamBlock::new);

	public static final IntProperty COLUMN = IntProperty.of("column", 0, 2);

	// [facingIndex][column] -- precomputed, tiny fixed set.
	private static final VoxelShape[][] SHAPES = makeShapes();

	public VerticalSeamBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(WATERLOGGED, false)
				.with(COLUMN, 1));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, COLUMN);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[facingIndex(state.get(FACING))][state.get(COLUMN)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[facingIndex(state.get(FACING))][state.get(COLUMN)];
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		boolean waterlogged = ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER;

		Direction facing;
		int column;
		if (side.getAxis().isHorizontal()) {
			facing = side;
			// Only the horizontal component of the aim matters for a full-height ridge.
			column = aimedSlot(ctx, facing)[0];
		} else {
			facing = ctx.getHorizontalPlayerFacing().getOpposite();
			column = 1;
		}

		return getDefaultState()
				.with(FACING, facing)
				.with(WATERLOGGED, waterlogged)
				.with(COLUMN, column);
	}

	// Authored for FACING=NORTH (wall on +Z, ridge hugging z=16, protruding to
	// z=13): 4px wide, full 16px tall, 3px deep -- thinner protrusion than the
	// nub, on the same 3-column grid centres (4/8/12px). Keep dims EQUAL to
	// scripts/gen_vertical_seam_models.py.
	private static VoxelShape[][] makeShapes() {
		Direction[] facings = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		VoxelShape[][] shapes = new VoxelShape[4][3];

		for (int column = 0; column < 3; column++) {
			Box base = new Box(
					(2.0 + 4.0 * column) / 16.0, 0.0, 13.0 / 16.0,
					(6.0 + 4.0 * column) / 16.0, 1.0, 16.0 / 16.0);
			for (Direction facing : facings) {
				shapes[facingIndex(facing)][column] = rotateBoxY(base, facing);
			}
		}

		return shapes;
	}
}
