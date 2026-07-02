package net.craynex.climbamountain;

import net.craynex.climbamountain.block.ModBlocks;
import net.craynex.climbamountain.command.ClimbSlipCommand;
import net.craynex.climbamountain.slip.SlipDataLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.item.ItemGroups;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClimbAMountainVr implements ModInitializer {
	public static final String MOD_ID = "climbamountainvr";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModBlocks.initialize();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
			entries.add(ModBlocks.ROCK_NUB);
			entries.add(ModBlocks.SLAB_OUTCROP);
			entries.add(ModBlocks.VERTICAL_SEAM);
		});

		// SERVER_DATA = datapack side (gameplay data, works on dedicated servers too);
		// fires on world load and /reload, filling the SlipRegistry from
		// data/<ns>/climb_slip/*.json across all packs.
		ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(id("slip_loader"), new SlipDataLoader());

		ClimbSlipCommand.register();

		LOGGER.info("Climb A Mountain initialized");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
