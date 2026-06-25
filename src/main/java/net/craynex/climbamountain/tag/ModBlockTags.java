package net.craynex.climbamountain.tag;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public final class ModBlockTags {
	private ModBlockTags() {
	}

	public static final TagKey<Block> HANDHOLD = of("handhold");
	public static final TagKey<Block> WEAK_HANDHOLD = of("weak_handhold");

	private static TagKey<Block> of(String path) {
		return TagKey.of(RegistryKeys.BLOCK, ClimbAMountainVr.id(path));
	}
}
