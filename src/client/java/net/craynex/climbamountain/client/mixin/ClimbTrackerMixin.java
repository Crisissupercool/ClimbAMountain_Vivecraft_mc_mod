package net.craynex.climbamountain.client.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.trackers.ClimbTracker;
import org.vivecraft.client_vr.provider.ControllerType;

import net.craynex.climbamountain.slip.SlipRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * The slip-while-gripped mechanic: every climbing tick, roll the gripped
 * block's slip chance (from the data-driven SlipRegistry); on a failed roll the
 * grip pops open -- the WALL let go, distinct from any future nail-durability
 * damage.
 *
 * Why TAIL of activeProcess and why just clearing `latched`: Vivecraft's own
 * release path (grab button let go) does exactly this -- it flips latched[c]
 * off and lets the next activeProcess tick restore gravity via its
 * gravityOverride cleanup. Mimicking that means we never touch the rest of the
 * state machine. Re-grabbing after a slip requires releasing and re-pressing
 * the grab (or leaving/re-entering the block), which is precisely the "lost my
 * hold, catch it again" feel. The early return in activeProcess only happens
 * when NOT latched, so a TAIL injection always runs while gripped.
 *
 * Which block: `latchStart` is the controller's world position at the moment
 * of the grab, inside Vivecraft's 0.2-thick grab slab straddling the wall
 * plane -- so flooring it can land either in the hold's own cell or in the
 * wall block one cell behind it. When it lands in the wall (slip usually 0),
 * we probe one cell along `grabDirection` (= the hold's FACING, pointing out
 * of the wall), which is where the hold actually is; we use the larger of the
 * two slip values.
 */
@Mixin(ClimbTracker.class)
public abstract class ClimbTrackerMixin {
	@Shadow(remap = false)
	@Final
	private boolean[] latched;

	@Shadow(remap = false)
	public int latchStartController;

	@Shadow(remap = false)
	public Vec3d[] latchStart;

	@Shadow(remap = false)
	@Final
	private Direction[] grabDirection;

	@Shadow(remap = false)
	@Final
	private MinecraftClient mc;

	@Shadow(remap = false)
	@Final
	private ClientDataHolderVR dh;

	@Unique
	private final Random climbamountainvr$slipRandom = new Random();

	@Inject(method = "activeProcess", at = @At("TAIL"), remap = false)
	private void climbamountainvr$slipWhileGripped(ClientPlayerEntity player, CallbackInfo ci) {
		int c = this.latchStartController;
		if (c < 0 || !this.latched[c] || this.mc.world == null) {
			return;
		}

		BlockPos pos = BlockPos.ofFloored(this.latchStart[c]);
		float slip = SlipRegistry.get(this.mc.world.getBlockState(pos));

		Direction out = this.grabDirection[c];
		if (out != null) {
			BlockPos holdPos = pos.offset(out);
			BlockState holdState = this.mc.world.getBlockState(holdPos);
			slip = Math.max(slip, SlipRegistry.get(holdState));
		}

		if (slip <= 0.0F || this.climbamountainvr$slipRandom.nextFloat() >= slip) {
			return;
		}

		// The wall lets go: drop both latches; Vivecraft's next tick restores
		// gravity and the player falls. Strong haptic pulse so the slip is felt,
		// not just seen.
		this.latched[0] = false;
		this.latched[1] = false;
		// ControllerType.values()[c] is Vivecraft's own controller-index idiom
		// (0 = RIGHT, 1 = LEFT); the int,int overload is deprecated.
		this.dh.vr.triggerHapticPulse(ControllerType.values()[c], 1500);
	}
}
