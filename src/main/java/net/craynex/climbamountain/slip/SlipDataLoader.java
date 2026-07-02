package net.craynex.climbamountain.slip;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

/**
 * Loads slip values from datapacks into SlipRegistry on every datapack
 * (re)load, including world load and /reload.
 *
 * File layout: any datapack can ship data/&lt;namespace&gt;/climb_slip/*.json,
 * each file a flat JSON object of "block_id": slipPerTick, e.g.
 *
 *   { "climbamountainvr:rock_nub": 0.02, "minecraft:vine": 0.0 }
 *
 * All files found across all namespaces are merged; files are visited in
 * sorted-identifier order so a later (alphabetically greater) file id
 * deterministically overrides an earlier one for the same block. Within one
 * file id, normal datapack stacking applies (the top enabled pack wins) --
 * which is how a pack maker retunes our defaults: ship a file with the same
 * path and higher pack priority.
 *
 * Registered against SERVER_DATA (this is gameplay data, not assets), which
 * also makes it work on dedicated servers. We implement vanilla's
 * SynchronousResourceReloader (fine here -- a handful of tiny JSON files) and
 * register it through Fabric's resource-loader-v1 ResourceLoader; the older
 * SimpleSynchronousResourceReloadListener path is deprecated in this Fabric
 * API version.
 */
public class SlipDataLoader implements SynchronousResourceReloader {
	public static final String DIRECTORY = "climb_slip";

	@Override
	public void reload(ResourceManager manager) {
		Map<Identifier, Resource> files = manager.findResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
		Map<Identifier, Float> merged = new HashMap<>();

		// TreeMap = deterministic file order (Identifier is Comparable).
		for (Map.Entry<Identifier, Resource> entry : new TreeMap<>(files).entrySet()) {
			try (InputStreamReader reader = new InputStreamReader(
					entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : json.entrySet()) {
					Identifier blockId = Identifier.tryParse(e.getKey());
					if (blockId == null) {
						ClimbAMountainVr.LOGGER.warn("Ignoring invalid block id '{}' in {}",
								e.getKey(), entry.getKey());
						continue;
					}
					merged.put(blockId, e.getValue().getAsFloat());
				}
			} catch (Exception ex) {
				ClimbAMountainVr.LOGGER.error("Failed to load climb-slip data file {}", entry.getKey(), ex);
			}
		}

		SlipRegistry.setAll(merged);
		ClimbAMountainVr.LOGGER.info("Loaded {} climb-slip entries from {} data file(s)",
				merged.size(), files.size());
	}
}
