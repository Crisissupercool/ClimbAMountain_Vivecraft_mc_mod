package net.craynex.climbamountain.client;

import org.vivecraft.client_vr.gameplay.trackers.ClimbTracker;

import net.craynex.climbamountain.ClimbAMountainVr;
import net.fabricmc.api.ClientModInitializer;

public class ClimbAMountainVrClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Deliberate tripwire: referencing ClimbTracker.class forces the class to
		// load NOW, which is when Mixin applies ClimbTrackerMixin (our slip hook).
		// Vivecraft only loads this class lazily on entering VR, so without this a
		// broken mixin (e.g. after a Vivecraft update renames a shadowed field)
		// would pass every flatscreen boot and only blow up in the headset.
		// This turns that into a fail-at-launch instead.
		ClimbAMountainVr.LOGGER.info("Vivecraft climb hook target loaded: {}", ClimbTracker.class.getName());
	}
}
