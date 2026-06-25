package net.craynex.climbamountain.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class RockNubBlock extends HorizontalFacingBlock implements Waterloggable {
	public static final MapCodec<RockNubBlock> CODEC = createCodec(RockNubBlock::new);

	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	// One boolean per 3x3 grid slot. A single block holds any subset of these, so
	// up to nine nubs share a cell -- the same trick candles/sea pickles use to put
	// several of one thing in one block. SLOTS[h][v]: h = left/centre/right column,
	// v = low/mid/high row, both face-local.
	public static final BooleanProperty[][] SLOTS = new BooleanProperty[3][3];

	static {
		for (int h = 0; h < 3; h++) {
			for (int v = 0; v < 3; v++) {
				SLOTS[h][v] = BooleanProperty.of("slot_" + h + "_" + v);
			}
		}
	}

	// Per-(facing, h, v) shape for a single nub. The collision/outline of a cluster
	// is the union of the present slots' boxes, cached per BlockState.
	private static final VoxelShape[][][] BASE_SHAPES = makeShapes();

	private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

	public RockNubBlock(Settings settings) {
		super(settings);

		BlockState state = getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(WATERLOGGED, false);
		for (int h = 0; h < 3; h++) {
			for (int v = 0; v < 3; v++) {
				state = state.with(SLOTS[h][v], false);
			}
		}
		setDefaultState(state);
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
		for (int h = 0; h < 3; h++) {
			for (int v = 0; v < 3; v++) {
				builder.add(SLOTS[h][v]);
			}
		}
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFor(state);
	}

	private VoxelShape shapeFor(BlockState state) {
		return shapeCache.computeIfAbsent(state, s -> {
			int fi = facingIndex(s.get(FACING));
			VoxelShape shape = VoxelShapes.empty();
			for (int h = 0; h < 3; h++) {
				for (int v = 0; v < 3; v++) {
					if (s.get(SLOTS[h][v])) {
						shape = VoxelShapes.union(shape, BASE_SHAPES[fi][h][v]);
					}
				}
			}
			return shape;
		});
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
		// Holding a nub and aiming at an empty slot of this cluster -> merge into it
		// (place another nub) instead of placing a fresh block in the next cell.
		if (!ctx.shouldCancelInteraction() && ctx.getStack().isOf(asItem())) {
			int[] slot = aimedSlot(ctx, state.get(FACING));
			if (!state.get(SLOTS[slot[0]][slot[1]])) {
				return true;
			}
		}
		return super.canReplace(state, ctx);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());

		// Merge: clicked an existing cluster (via canReplace) -> turn on the aimed slot.
		if (existing.isOf(this)) {
			int[] slot = aimedSlot(ctx, existing.get(FACING));
			return existing.with(SLOTS[slot[0]][slot[1]], true);
		}

		// Fresh placement in an empty cell.
		Direction side = ctx.getSide();
		boolean waterlogged = ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER;

		Direction facing;
		int h;
		int v;
		if (side.getAxis().isHorizontal()) {
			facing = side;
			int[] slot = aimedSlot(ctx, facing);
			h = slot[0];
			v = slot[1];
		} else {
			facing = ctx.getHorizontalPlayerFacing().getOpposite();
			h = 1;
			v = 1;
		}

		return getDefaultState()
				.with(FACING, facing)
				.with(WATERLOGGED, waterlogged)
				.with(SLOTS[h][v], true);
	}

	// Which 3x3 slot the player's crosshair is pointing at, in face-local terms.
	// Vertical is always world Y; horizontal is world X (north/south walls) or world
	// Z (east/west). We match the hit fraction to the nub's WORLD position, so the
	// +axis walls (south/west) mirror the column with (2 - slot) -- see makeShapes.
	private int[] aimedSlot(ItemPlacementContext ctx, Direction facing) {
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

	private static int slot(double fraction) {
		int i = (int) (fraction * 3.0);
		if (i < 0) {
			return 0;
		}
		if (i > 2) {
			return 2;
		}
		return i;
	}

	private static int facingIndex(Direction facing) {
		return switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 1;
			case WEST -> 2;
			case EAST -> 3;
			default -> 0;
		};
	}

	// The 3x3 of base boxes is authored for FACING=NORTH (wall on +Z, box hugging
	// z=16 and protruding to z=12). Every other facing reuses these boxes, rotated
	// around the cell centre by the same y-rotation the blockstate applies to the
	// model -- so collision and visuals can never drift apart.
	private static VoxelShape[][][] makeShapes() {
		double depth = 4.0;
		double[] yBottom = { 1.0, 5.0, 9.0 };
		double[] yTop = { 6.0, 10.0, 14.0 };

		Direction[] facings = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		VoxelShape[][][] shapes = new VoxelShape[4][3][3];

		for (int h = 0; h < 3; h++) {
			for (int v = 0; v < 3; v++) {
				Box base = new Box(
						(2.0 + 4.0 * h) / 16.0, yBottom[v] / 16.0, (16.0 - depth) / 16.0,
						(6.0 + 4.0 * h) / 16.0, yTop[v] / 16.0, 16.0 / 16.0);

				for (Direction facing : facings) {
					shapes[facingIndex(facing)][h][v] = rotateBoxY(base, facing);
				}
			}
		}

		return shapes;
	}

	private static VoxelShape rotateBoxY(Box b, Direction facing) {
		return switch (facing) {
			case EAST -> VoxelShapes.cuboid(1.0 - b.maxZ, b.minY, b.minX, 1.0 - b.minZ, b.maxY, b.maxX);
			case SOUTH -> VoxelShapes.cuboid(1.0 - b.maxX, b.minY, 1.0 - b.maxZ, 1.0 - b.minX, b.maxY, 1.0 - b.minZ);
			case WEST -> VoxelShapes.cuboid(b.minZ, b.minY, 1.0 - b.maxX, b.maxZ, b.maxY, 1.0 - b.minX);
			default -> VoxelShapes.cuboid(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		};
	}
}
