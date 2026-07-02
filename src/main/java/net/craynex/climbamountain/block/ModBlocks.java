package net.craynex.climbamountain.block;

import java.util.function.Function;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	private ModBlocks() {
	}

	public static final Block ROCK_NUB = register(
			"rock_nub",
			RockNubBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.STONE_GRAY)
					.strength(1.5F, 6.0F)
					.requiresTool()
					.nonOpaque()
					.sounds(BlockSoundGroup.STONE)
					.pistonBehavior(PistonBehavior.DESTROY),
			true);

	public static final Block SLAB_OUTCROP = register(
			"slab_outcrop",
			SlabOutcropBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.STONE_GRAY)
					.strength(1.5F, 6.0F)
					.requiresTool()
					.nonOpaque()
					.sounds(BlockSoundGroup.STONE)
					.pistonBehavior(PistonBehavior.DESTROY),
			true);

	public static final Block VERTICAL_SEAM = register(
			"vertical_seam",
			VerticalSeamBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.STONE_GRAY)
					.strength(1.5F, 6.0F)
					.requiresTool()
					.nonOpaque()
					.sounds(BlockSoundGroup.STONE)
					.pistonBehavior(PistonBehavior.DESTROY),
			true);

	public static void initialize() {
		ClimbAMountainVr.LOGGER.info("Registering blocks for {}", ClimbAMountainVr.MOD_ID);
	}

	private static Block register(String path, Function<AbstractBlock.Settings, Block> factory,
			AbstractBlock.Settings settings, boolean withItem) {
		Identifier id = ClimbAMountainVr.id(path);

		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = factory.apply(settings.registryKey(blockKey));

		if (withItem) {
			RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
			BlockItem item = new BlockItem(block,
					new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
			Registry.register(Registries.ITEM, itemKey, item);
		}

		return Registry.register(Registries.BLOCK, blockKey, block);
	}
}
