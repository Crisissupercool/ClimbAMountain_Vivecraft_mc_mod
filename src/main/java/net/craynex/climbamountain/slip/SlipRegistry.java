package net.craynex.climbamountain.slip;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * The data-driven slip lookup: block id -> slip chance PER TICK while gripped.
 * Shared infrastructure between the climbable-block system (weak handholds pop
 * your grip) and the future nail system (a wooden nail "slipping out of dirt"
 * is the dirt block having a high slip value for low-tier nails).
 *
 * Contents come from data files (see SlipDataLoader) so packs can tune values
 * without recompiling. Unlisted blocks slip 0.0 -- no slip is the sane default,
 * matching leaves and every ordinary climbable.
 *
 * The map reference is swapped atomically on each datapack (re)load; readers on
 * the client/render threads only ever see a complete immutable snapshot, so no
 * locking is needed on the hot per-tick lookup path.
 */
public final class SlipRegistry {
	private static volatile Map<Identifier, Float> slipByBlockId = Map.of();

	private SlipRegistry() {
	}

	public static float get(Block block) {
		return slipByBlockId.getOrDefault(Registries.BLOCK.getId(block), 0.0F);
	}

	public static float get(BlockState state) {
		return get(state.getBlock());
	}

	public static int size() {
		return slipByBlockId.size();
	}

	static void setAll(Map<Identifier, Float> values) {
		slipByBlockId = Map.copyOf(values);
	}
}
