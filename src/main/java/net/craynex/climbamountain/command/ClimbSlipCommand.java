package net.craynex.climbamountain.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.craynex.climbamountain.slip.SlipRegistry;
import net.craynex.climbamountain.tag.ModBlockTags;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Debug command: /climbslip prints the slip value + handhold-tag membership of
 * the block you're looking at (within 8 blocks). Lets the slip registry and the
 * tag wiring be verified in a flat dev world with no headset. Temporary tooling
 * -- remove or gate it once the VR grip loop is testable directly.
 */
public final class ClimbSlipCommand {
	private ClimbSlipCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("climbslip")
						.executes(ctx -> execute(ctx.getSource()))));
	}

	private static int execute(ServerCommandSource source) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();

		// Entity.raycast: eye-position ray along the look vector; tickDelta 0 is
		// fine server-side, and we don't care about fluids for a wall check.
		HitResult hit = player.raycast(8.0, 0.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			source.sendFeedback(() -> Text.literal("[climbslip] Not looking at a block within 8 blocks"), false);
			return 0;
		}

		BlockState state = source.getWorld().getBlockState(blockHit.getBlockPos());
		Identifier id = Registries.BLOCK.getId(state.getBlock());
		float slip = SlipRegistry.get(state);
		boolean handhold = state.isIn(ModBlockTags.HANDHOLD);
		boolean weak = state.isIn(ModBlockTags.WEAK_HANDHOLD);

		source.sendFeedback(() -> Text.literal(String.format(
				"[climbslip] %s -> slip %.4f/tick, handhold=%s, weak_handhold=%s (%d entries loaded)",
				id, slip, handhold, weak, SlipRegistry.size())), false);
		return 1;
	}
}
