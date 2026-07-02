package net.craynex.climbamountain.worldgen;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * Only the feature TYPE is registered in code -- it's the piece with logic JSON
 * can't express (the column-scan cliff detection). Everything tunable lives in
 * data files instead, so placement iterates without recompiling:
 * configured_feature + placed_feature JSONs under data/climbamountainvr/worldgen/,
 * and the biome attachment via Lithostitched's add_features worldgen modifier
 * under data/climbamountainvr/lithostitched/worldgen_modifier/.
 */
public final class ModFeatures {
	private ModFeatures() {
	}

	public static final Feature<DefaultFeatureConfig> CLIFF_HANDHOLDS = Registry.register(
			Registries.FEATURE,
			ClimbAMountainVr.id("cliff_handholds"),
			new CliffHandholdsFeature(DefaultFeatureConfig.CODEC));

	public static void initialize() {
		ClimbAMountainVr.LOGGER.info("Registering worldgen features for {}", ClimbAMountainVr.MOD_ID);
	}
}
