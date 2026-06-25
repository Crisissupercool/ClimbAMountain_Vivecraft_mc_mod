# CLAUDE.md — ClimbAMountain

Project context for Claude Code. Read this first every session. Keep the **Status** section at the bottom updated as work progresses.

---

## What this mod is

A Minecraft VR climbing mod that extends **Vivecraft** so the world becomes physically climbable in roomscale VR. The core fantasy: you read a cliff face, find the holds, and climb it with your hands — the way you would in Mirror's Edge or real bouldering. The terrain dictates where you can move; your skill is reading it.

Two parallel systems make this work:

1. **Grip extension (vanilla blocks).** Certain existing blocks — leaves, logs, vines, and anything tagged — become grippable so you can climb trees and existing foliage naturally. No worldgen needed; this just makes tagged blocks climbable wherever they already occur.

2. **Worldgen handholds (custom blocks).** Custom handhold blocks are placed by worldgen on *detected cliff faces* — steep/vertical stone surfaces that you'd otherwise be unable to scale. These give natural mountains climbable routes.

**Design rule that defines the whole mod:** smooth surfaces with no holds are *unclimbable*. You cannot climb a bare stone wall. You rely on natural holds (foliage, worldgen handholds) or on nails you place yourself. The constraint is the point — it's what makes climbing feel like a skill rather than a movement toggle.

---

## Target environment (LOCKED — do not change versions)

- **Loader:** Fabric
- **Minecraft:** 1.21.11
- **Java:** 21 (Temurin)
- **Mappings:** Yarn
- **Vivecraft:** `1.21.11-1.3.9-fabric` (the actively-maintained Fabric port, not the old jrbudda codebase)
- **Worldgen library:** Lithostitched (match the version shipped in the target pack)
- **Fabric API:** match pack version (`0.141.4+1.21.11` in pack at time of writing)
- **Source layout:** split client / common sources (client-only VR code must NOT be referenced from common)
- **Data generation:** enabled (generate tags, recipes, loot tables, blockstates from code)

> Version lock matters because Vivecraft mixin targets are version-specific. A mixin that works on 1.21.11 will not work on a different MC version. Develop against these exact versions.

### Target modpack
**Ryan Clifford's VR Experience 3** — Fabric, MC 1.21.11. Uses **Tectonic** for dramatic overworld terrain (taller/steeper cliffs than vanilla), **Voxy** + **Voxy WorldGen** for LOD streaming, **Countered's Terrain Slabs** for terrain smoothing, **ImmersiveMC**, and Vivecraft. No Terralith — the pack reshapes *vanilla* biomes only, so vanilla biome tags cover all terrain. Lithostitched is already present (other mods depend on it), so build worldgen on top of it.

**Tectonic implications:** cliff-detection thresholds must be tuned against Tectonic's tall faces, not vanilla hills. Handhold density should scale with face height (target ~1 hold per 2–3 blocks vertically) rather than a flat per-chunk density.

**Voxy caveat:** worldgen handholds only appear in newly generated chunks AND won't show in Voxy's pre-generated LOD until its cache is rebuilt. Document "regenerate Voxy after install" in the README. Fine for fresh worlds; a headache for explored ones.

---

## Block taxonomy (the worldgen handholds)

All three share a "protrudes a few pixels from a parent wall" silhouette. All are **directional** (attach to one horizontal face), have **partial collision shapes** (the protruding part — this IS the grippable shape Vivecraft checks against), are **waterloggable**, and have **biome-tinted texture variants** (deepslate/stone/snowy) so they blend with surroundings. **Not craftable** — worldgen-only.

| Block | Climbing analogy | Difficulty | Shape |
|---|---|---|---|
| **Slab outcrop** | Jug (big easy hold) | Easy | Wide, flat top — a small ledge |
| **Rock nub** | Crimp (fingertip edge) | Hard | Small, rounded, asymmetric protrusion |
| **Vertical seam** | Rail (traverse hold) | Medium | Thin ridge, ~2 blocks tall, for sideways movement |

Start implementation with the **rock nub** — it's the smallest and most representative.

---

## The nail (the player-placed override)

A craftable item (iron-ish recipe) that the player hammers into a wall to create their own grip point. This is the deliberate, costly override for "I really need to climb this specific unclimbable face" — without dissolving the terrain-is-the-puzzle constraint.

- Placeable on the side of a block; protrudes a few pixels; grippable via the tag system.
- **Damage-on-retrieval:** the nail has durability (~3 uses). Retrieving it costs 1 durability; it breaks after the limit. Makes placement semi-permanent and forces route planning.
- **VR pull-out gesture (the signature interaction):** when the controller is near a nail and grip is pressed, check the controller's motion over ~0.5s. If it's pulling *outward* (along the nail's facing direction), retrieve the nail into the hand. If the player just holds, treat it as a climbing grip. This double-purpose grip — *hold to climb, pull to retrieve* — is the kind of thing only VR can do, and it's a tiny prototype of the input-disambiguation work the future VR parkour game will need.
- Possible later progression (NOT MVP): wooden peg (1 use, breaks on retrieval) / iron nail (3 uses) / permanent piton (expensive, never retrievable).

---

## Tag + slip system

Climbable blocks are defined by tags so the mod (and other datapacks) can contribute:

- `climbamountainvr:handhold` — normal grip, ~0% slip
- `climbamountainvr:weak_handhold` — slip-prone

**Slip mechanic:** a per-block slip *chance per tick* while gripped, stored in a data-driven registry (JSON mapping block id → slip value). Leaves ≈ 0%, slab outcrop low, vertical seam medium, rock nub highest. Combined with Vivecraft's existing "hold sneak to not fall when you let go," this gives a difficulty curve without a full stamina system. **Do NOT build stamina/XP/fatigue for MVP.**

Block-shape communicates difficulty visually (jug/crimp/sloper vocabulary), so players read routes without a tutorial.

---

## Worldgen approach

- **Cliff detection** via column scanning at candidate positions: walk vertically through solid blocks, count consecutive solid blocks that have ≥1 horizontally-adjacent air block. If the run exceeds a threshold (~4–5, tuned for Tectonic), the column is a cliff face.
- **Placement:** along detected faces, place handholds ~every 2–3 blocks vertically with slight left/right lateral variance (so the player reaches across, not just straight up). Mix block types — mostly nubs, occasional slab outcrops as rests, occasional seams for traverses.
- **Biome identity** via vanilla biome tags (no custom biomes in pack):
  - Forested mountains → dense leaves/roots + easy holds ("beginner cliff")
  - Bare stone peaks/windswept hills → sparse rock nubs ("advanced route")
  - Snowy peaks → sparsest, longest reaches ("hardest")
- **Prefer JSON-driven via Lithostitched** (configured features, placed features, biome modifiers, placement predicates) over hardcoded registration, so placement can be iterated without recompiling. Write a custom placement predicate in code only where JSON can't express the cliff-detection logic.
- Use **`minecraft:`/`c:` convention biome tags**, never hardcoded biome IDs, for compatibility across vanilla/Tectonic.

**Do NOT** mark Countered's Terrain Slabs as climbable — they're placed on gentle walkable slopes, not the steep faces we care about. Our job is specifically the steep faces Countered's ignores.

---

## Architecture summary

- Fabric mod, MC 1.21.11, deps: Vivecraft, Lithostitched, Fabric API.
- **One mixin** into Vivecraft's climbable-block check that also consults our `handhold`/`weak_handhold` tags. (~small; this is the climbing hookup.)
- **Data-driven slip registry** (block id → slip-per-tick).
- **Three custom blocks** (directional, partial collision, waterloggable, tinted variants) + **the nail item**.
- **Worldgen as Lithostitched JSON** + a custom cliff-detection predicate.
- **Block/item tags, recipes, loot tables via datagen.**
- VR-specific code (gesture detection, grip handling) lives in `src/client/` ONLY.

### Decisions already made
- Java, not Kotlin. Yarn mappings. Split sources. Datagen on.
- Handholds are worldgen-only (not craftable). The nail is the player override.
- No stamina/XP/fatigue systems in MVP.
- Build worldgen on Lithostitched (already in pack), not raw Fabric biome APIs.

---

## Conventions / preferences

- Explanations go **above or below** code blocks, not as inline comments. Include the *reasoning* behind syntax/API choices (this is a learning project as much as a shipping one).
- Keep the mod id `climbamountainvr` consistent across `gradle.properties`, `fabric.mod.json`, and the Java package. **Mod id is permanent** — renaming it breaks existing world saves (every placed block reference changes).
- Small, testable vertical slices. Prefer "register one block, see it in-game" over "build the whole block system then test."

---

## How to test
- `./gradlew runClient` launches a dev Minecraft with the mod loaded. The task sits at ~92% the whole time Minecraft is open — that's normal; it completes when the game closes. Close the game window normally (don't Ctrl+C the Gradle process — it orphans Java processes).
- Verify mod loads via the Mods menu (Mod Menu is in the template).
- For climbing/gesture work, you must actually test in VR on the dev machine — feel can't be unit-tested.

---

## Scope
~9–12 weeks of evening work (Solum-scale). Time distribution: grip extension (1wk) · 3 custom blocks (2wk) · cliff detection iteration (1–2wk) · worldgen route placement (1–2wk) · nail + VR gesture (1wk) · recipes/config/tags/polish (1wk) · ongoing pack testing.

This is deliberate prep for a future VR parkour game (grip-based traversal, handhold placement, affordance design) — solved in a forgiving sandbox where engine/physics/multiplayer already work.

---

## STATUS (update this every session)

**Current state:** First vertical slice built — the **rock nub** block. Compiles (common + client), datagen runs and emits a correct drop-self loot table. NOT yet verified in `runClient` (next action). Climbing/grip NOT wired (needs Vivecraft dep, separate task).

**Rock nub — what exists:**
- `block/RockNubBlock.java` — extends `HorizontalFacingBlock`, implements `Waterloggable`. `FACING` + `WATERLOGGED` state. Custom per-facing `VoxelShape` (5px-deep protrusion) used for BOTH outline and collision (so it's solid/grippable, not decoration). MapCodec via `createCodec` + `getCodec()` override (required in 1.21.11). **Convention: FACING = direction the nub points OUT from the wall, = `ctx.getSide()` at placement.** Vanilla `ItemPlacementContext.getBlockPos()` already offsets the placed block into the air cell adjacent to the clicked face (when the clicked block isn't replaceable), so we do NOT offset position manually. The box must HUG the wall (the cell edge OPPOSITE the protrusion direction) and stick out only ~5px — e.g. FACING=north → wall is on the cell's +Z/south edge, so the box sits at z[11,16] and protrudes toward -Z. Base model (y:0) = facing=north; blockstate Y-rotations north=0/east=90/south=180/west=270 (furnace convention). Model element and VoxelShape MUST stay byte-for-byte consistent across all 4 rotations or collision won't match visuals.
- **Multi-nub cluster (NEW):** a single block holds up to 9 nubs in a 3×3 grid on one wall — candle/sea-pickle pattern. Properties: `FACING` (wall) + `WATERLOGGED` + nine booleans `slot_<h>_<v>` (`SLOTS[h][v]`). Blockstate is **multipart** (each present slot renders its nub, rotated per facing); collision/outline = union of present slots' boxes, cached per `BlockState` in `shapeCache`. Re-using the same item on the block adds another nub: `canReplace` returns true when you hold the nub, aren't sneaking, and aim at an EMPTY slot (so it merges instead of placing in the next cell); `getPlacementState` detects an existing cluster and flips the aimed slot on. Aiming through a gap hits the wall behind → resolves to the cluster cell → adds there.
- **Aim → slot (3×3):** `aimedSlot()` reads `ctx.getHitPos()`, matches the WORLD-coord fraction along the wall (X for N/S, Z for E/W) + vertical Y to the nub's world position, so it's chirality-safe; +axis walls (south/west) mirror via `2 - slot`. Per-(facing,h,v) boxes are the base boxes ROTATED by the same y-rotation the blockstate applies (`rotateBoxY`), so visuals + collision can't drift. Models/blockstate generated by `scripts/gen_rock_nub_models.py` (9 models + 36-part multipart) — re-run if box dims change in EITHER the script or `RockNubBlock.makeShapes()` (keep equal). Item icon uses centred `block/rock_nub_h1_v1`.
- **Known limits (cluster):** breaking the block removes ALL its nubs and the loot table still drops only ONE item (worldgen-only/creative for now, so low priority — revisit for survival/drops-per-nub). Removing a single nub from a cluster isn't possible yet — that's a natural fit for the future nail-pull VR gesture. `rotate`/`mirror` only remap `FACING`, not the slot grid (matters only for structure-block/worldgen rotation).
- `block/ModBlocks.java` — central registry + `register(path, factory, settings, withItem)` helper. In 1.21.2+ blocks AND items need their `RegistryKey` baked into `Settings` (`.registryKey(key)`); the helper does this. Reuse this for the next two handholds.
- Registration + creative tab in `ClimbAMountainVr.onInitialize()` (added to `ItemGroups.NATURAL` via `ItemGroupEvents`). Not craftable (no recipe), as intended.
- Datagen: **loot table only** (`client/datagen/ModBlockLootTableProvider`, wired in `ClimbAMountainVrDataGenerator`). Generated to `src/main/generated/`.
- **Hand-authored assets** (deliberate deviation from "datagen everything" — see note below): `models/block/rock_nub.json` (custom `elements` geometry, matches the VoxelShape), `blockstates/rock_nub.json` (4 facing variants via Y-rotation), `items/rock_nub.json` (1.21.4+ item-model definition pointing at the block model — block-items don't need a separate `models/item/` file), `textures/block/rock_nub.png` (16×16 placeholder stone noise), `lang/en_us.json`.

**Asset-strategy note (user-approved hybrid):** custom protruding geometry can't be expressed by vanilla model templates, and the 1.21.11 `BlockStateModelGenerator` builder API is verbose/volatile — so block model + blockstate + item-model are hand-authored; datagen is used only for the loot table (where it's stable and earns its keep). Revisit full model datagen when adding the slab outcrop / vertical seam if it proves worthwhile.

**API gotchas learned (1.21.11-specific):** blocks override `getCodec()` returning a `MapCodec`; blocks+items require `RegistryKey` in `Settings`; model generators live in `net.minecraft.client.data`; `FabricModelProvider` moved to the `api.client.datagen.v1.provider` package; every item needs an `assets/<ns>/items/<name>.json` model definition (new item-model system). Mappings source of truth: `~/.gradle/caches/fabric-loom/1.21.11/.../mappings.tiny`.

**Verify on first session:**
- [x] Mod id is `climbamountainvr` (in `fabric.mod.json` `id` and loom `mods` block). NOTE: package is `net.craynex.climbamountain` (no `vr`) while mod id has `vr` — both permanent, don't "fix" the mismatch.
- [x] Java package: `net.craynex.climbamountain` (common), `net.craynex.climbamountain.client` (client). `maven_group=net.craynex.climbamountain`. Helper `ClimbAMountainVr.id(path)` exists for namespaced Identifiers.
- [ ] Lithostitched + Vivecraft maven coordinates NOT yet confirmed — deferred to the dependency task (no repos/deps added to build.gradle yet; only minecraft/yarn/loader/fabric-api present).

Confirmed too: split client/common source sets ARE set up (`splitEnvironmentSourceSets()`); datagen IS wired (`configureDataGeneration { client = true }`, entrypoint `ClimbAMountainVrDataGenerator`, currently empty).

**Next up:**
- [ ] **VERIFY rock nub in `runClient`** (immediate next action): place it on a wall, confirm it faces outward, protrudes, has collision (walk into it), and waterlogs. Then check the Mods menu + creative Natural tab.
- [ ] Block tag scaffolding (`handhold`, `weak_handhold`) — datagen the tag JSONs.
- [ ] Add Vivecraft + Lithostitched as dependencies (separate task — needs a registry/maven lookup; not done yet)
- [ ] Generalize the rock nub pattern to slab outcrop + vertical seam (reuse `ModBlocks.register`)

**Open questions / parked:**
- Exact cliff-detection thresholds (tune in-game against Tectonic)
- Whether to ship a Voxy-cache-rebuild note or an existing-world rescan tool (rescan is +1–2wk, defer)
