package net.craynex.climbamountain.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.craynex.climbamountain.block.AbstractWallHoldBlock;
import net.craynex.climbamountain.block.ModBlocks;
import net.craynex.climbamountain.block.RockNubBlock;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Worldgen: detect a cliff face at the feature origin's column and stud it with
 * rock nubs so it becomes a climbable route.
 *
 * Cliff detection (per the CLAUDE.md worldgen spec): starting from the surface
 * position the placement modifiers hand us, walk DOWN through the column and
 * count consecutive solid blocks that have at least one horizontally-adjacent
 * air (or water) cell -- i.e. blocks that ARE an exposed vertical face. A run
 * of at least MIN_CLIFF_RUN such blocks is a cliff; anything shorter is a slope
 * or a step that Countered's Terrain Slabs territory covers and we ignore.
 *
 * Placement along a detected run: one nub every 2-3 blocks vertically (the
 * CLAUDE.md density target of ~1 hold per 2-3 blocks of face height), each with
 * +-1 lateral variance along the face so the player reaches across rather than
 * straight up, and a random 3x3 sub-slot for further hand-position variety.
 * Nubs go in the AIR cell in front of the face, FACING = the direction out of
 * the wall (the same convention placement-by-hand uses).
 *
 * Deliberately conservative first cut: rock nubs only (no outcrops/seams mix
 * yet), one column per feature call -- density comes from the placed feature's
 * count modifier. Thresholds are starting points to tune against Tectonic.
 */
public class CliffHandholdsFeature extends Feature<DefaultFeatureConfig> {
	// Minimum consecutive exposed-solid blocks for a column to count as a cliff.
	private static final int MIN_CLIFF_RUN = 5;
	// How far below the surface origin we bother scanning.
	private static final int MAX_SCAN_DEPTH = 64;

	public CliffHandholdsFeature(Codec<DefaultFeatureConfig> configCodec) {
		super(configCodec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		// The heightmap placement puts the origin in the first air block above the
		// surface; start scanning at the first block below it.
		int top = origin.getY() - 1;
		int bottom = Math.max(world.getBottomY(), top - MAX_SCAN_DEPTH);

		List<BlockPos> runWalls = new ArrayList<>();
		List<Direction> runFaces = new ArrayList<>();
		boolean placedAny = false;

		for (int y = top; y >= bottom; y--) {
			BlockPos wallPos = new BlockPos(origin.getX(), y, origin.getZ());
			Direction face = exposedFace(world, wallPos);
			if (face != null) {
				runWalls.add(wallPos);
				runFaces.add(face);
			} else {
				placedAny |= placeAlongRun(world, random, runWalls, runFaces);
				runWalls.clear();
				runFaces.clear();
			}
		}
		placedAny |= placeAlongRun(world, random, runWalls, runFaces);

		return placedAny;
	}

	// A block is part of a cliff face if it is solid and at least one horizontal
	// neighbour is enterable (air or water); returns that neighbour's direction,
	// or null. Water counts so underwater cliffs and shorelines get holds too --
	// the nub is waterloggable.
	private static Direction exposedFace(StructureWorldAccess world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!state.isSolidBlock(world, pos)) {
			return null;
		}
		for (Direction dir : Direction.Type.HORIZONTAL) {
			BlockPos neighbor = pos.offset(dir);
			BlockState neighborState = world.getBlockState(neighbor);
			if (neighborState.isAir() || neighborState.getFluidState().getFluid() == Fluids.WATER) {
				return dir;
			}
		}
		return null;
	}

	private boolean placeAlongRun(StructureWorldAccess world, Random random,
			List<BlockPos> walls, List<Direction> faces) {
		if (walls.size() < MIN_CLIFF_RUN) {
			return false;
		}

		boolean placed = false;
		// Walk bottom-of-run -> top (the lists were filled top-down) every 2-3
		// blocks, like a climber would find holds spaced a reach apart.
		int i = walls.size() - 1;
		while (i >= 0) {
			placed |= placeNub(world, random, walls.get(i), faces.get(i));
			i -= 2 + random.nextInt(2);
		}
		return placed;
	}

	private boolean placeNub(StructureWorldAccess world, Random random, BlockPos wallPos, Direction face) {
		// Lateral variance: slide up to 1 block sideways along the face (the axis
		// perpendicular to the facing), re-validating the shifted spot.
		int slide = random.nextInt(3) - 1;
		if (slide != 0) {
			Direction tangent = face.rotateYClockwise();
			BlockPos slidPos = wallPos.offset(tangent, slide);
			BlockState slidState = world.getBlockState(slidPos);
			BlockState slidFront = world.getBlockState(slidPos.offset(face));
			if (slidState.isSolidBlock(world, slidPos)
					&& (slidFront.isAir() || slidFront.getFluidState().getFluid() == Fluids.WATER)) {
				wallPos = slidPos;
			}
		}

		BlockPos nubPos = wallPos.offset(face);
		BlockState current = world.getBlockState(nubPos);
		boolean water = current.getFluidState().getFluid() == Fluids.WATER;
		if (!current.isAir() && !water) {
			return false;
		}

		// FACING points OUT from the wall -- same convention as hand placement
		// (ctx.getSide()). One random 3x3 slot on; worldgen clusters stay sparse.
		BlockState nub = ModBlocks.ROCK_NUB.getDefaultState()
				.with(RockNubBlock.FACING, face)
				.with(AbstractWallHoldBlock.WATERLOGGED, water)
				.with(RockNubBlock.SLOTS[random.nextInt(3)][random.nextInt(3)], true);

		this.setBlockState(world, nubPos, nub);
		return true;
	}
}
