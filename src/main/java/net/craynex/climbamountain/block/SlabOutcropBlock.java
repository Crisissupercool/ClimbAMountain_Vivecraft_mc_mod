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
 * The "jug" -- a wide flat ledge protruding from a wall. Easiest hold: big flat
 * top you can hang from or stand on as a rest.
 *
 * Unlike the rock nub's 3x3 boolean grid, one cell holds exactly ONE outcrop:
 * a ledge already spans (almost) the full cell width, so only its HEIGHT within
 * the cell varies. That's a single LEVEL int property (0=low / 1=mid / 2=high),
 * picked from where on the wall you aim -- same aim-snapping feel as the nub,
 * one axis instead of two. LEVEL=2 puts the flat top flush with the cell's top
 * edge, so it doubles as a step aligned with the floor of the block above.
 */
public class SlabOutcropBlock extends AbstractWallHoldBlock {
	public static final MapCodec<SlabOutcropBlock> CODEC = createCodec(SlabOutcropBlock::new);

	public static final IntProperty LEVEL = IntProperty.of("level", 0, 2);

	// [facingIndex][level] -- one box per state, precomputed. No cache map needed:
	// the shape count is tiny and fixed.
	private static final VoxelShape[][] SHAPES = makeShapes();

	public SlabOutcropBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(WATERLOGGED, false)
				.with(LEVEL, 1));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, LEVEL);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[facingIndex(state.get(FACING))][state.get(LEVEL)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[facingIndex(state.get(FACING))][state.get(LEVEL)];
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		boolean waterlogged = ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER;

		Direction facing;
		int level;
		if (side.getAxis().isHorizontal()) {
			facing = side;
			// Only the vertical component of the aim matters for a ledge.
			level = aimedSlot(ctx, facing)[1];
		} else {
			facing = ctx.getHorizontalPlayerFacing().getOpposite();
			level = 1;
		}

		return getDefaultState()
				.with(FACING, facing)
				.with(WATERLOGGED, waterlogged)
				.with(LEVEL, level);
	}

	// Authored for FACING=NORTH (wall on +Z, ledge hugging z=16, protruding to
	// z=11): 14px wide, 3px thick, 5px deep -- wide flat top, a small ledge.
	// Keep dims EQUAL to scripts/gen_slab_outcrop_models.py.
	private static VoxelShape[][] makeShapes() {
		double[] yBottom = { 1.0, 7.0, 13.0 };
		double[] yTop = { 4.0, 10.0, 16.0 };

		Direction[] facings = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		VoxelShape[][] shapes = new VoxelShape[4][3];

		for (int level = 0; level < 3; level++) {
			Box base = new Box(
					1.0 / 16.0, yBottom[level] / 16.0, 11.0 / 16.0,
					15.0 / 16.0, yTop[level] / 16.0, 16.0 / 16.0);
			for (Direction facing : facings) {
				shapes[facingIndex(facing)][level] = rotateBoxY(base, facing);
			}
		}

		return shapes;
	}
}
