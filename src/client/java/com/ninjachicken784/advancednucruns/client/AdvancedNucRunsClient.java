package com.ninjachicken784.advancednucruns.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.RunStats;
import com.ninjachicken784.advancednucruns.RunTimer;
import com.ninjachicken784.advancednucruns.WaypointStore;

public class AdvancedNucRunsClient implements ClientModInitializer {
	/** The waypoints that are currently active. Shared by the command and the renderer. */
	public static final WaypointStore WAYPOINTS = new WaypointStore();

	/** Times the current run. Shared by the command, the chat listener and the on-screen timer. */
	public static final RunTimer RUN_TIMER = new RunTimer();

	/** The PB and this session's average. The PB is saved between sessions. */
	public static final RunStats RUN_STATS = new RunStats();

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			// Minecraft command names are case-sensitive, so register both spellings.
			dispatcher.register(AncCommand.build("anc"));
			dispatcher.register(AncCommand.build("Anc"));
		});

		WaypointRenderer.register();
		RunHud.register();
		RunChatListener.register();

		AdvancedNucRuns.LOGGER.info("Advanced NUC Runs loaded. Use /anc");
	}
}
