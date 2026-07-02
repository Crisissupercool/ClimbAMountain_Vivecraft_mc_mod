package net.craynex.climbamountain.tag;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModBlockTags {
	private ModBlockTags() {
	}

	public static final TagKey<Block> HANDHOLD = of("handhold");
	public static final TagKey<Block> WEAK_HANDHOLD = of("weak_handhold");

	// Vivecraft's own climbable tag. Its core grip check
	// (ClimbTracker.isClimbableBlock) accepts any block in #vivecraft:climbable,
	// and tag files merge across mods -- so datagenning our two tags INTO this
	// tag is the whole grip hookup, with no mixin into the check itself.
	public static final TagKey<Block> VIVECRAFT_CLIMBABLE =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of("vivecraft", "climbable"));

	private static TagKey<Block> of(String path) {
		return TagKey.of(RegistryKeys.BLOCK, ClimbAMountainVr.id(path));
	}
}
