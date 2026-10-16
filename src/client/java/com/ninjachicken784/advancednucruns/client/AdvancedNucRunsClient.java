package com.ninjachicken784.advancednucruns.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.PriceBook;
import com.ninjachicken784.advancednucruns.ProfitTracker;
import com.ninjachicken784.advancednucruns.RngMeter;
import com.ninjachicken784.advancednucruns.RunStats;
import com.ninjachicken784.advancednucruns.RunTimer;
import com.ninjachicken784.advancednucruns.TrackerEngine;
import com.ninjachicken784.advancednucruns.WaypointStore;

public class AdvancedNucRunsClient implements ClientModInitializer {
	/** The waypoints that are currently active. Shared by the command and the renderer. */
	public static final WaypointStore WAYPOINTS = new WaypointStore();

	/** Times the current run. Shared by the command, the chat listener and the on-screen timer. */
	public static final RunTimer RUN_TIMER = new RunTimer();

	/** The PB and this session's average. The PB is saved between sessions. */
	public static final RunStats RUN_STATS = new RunStats();

	/** The Crystal Nucleus profit tracker: drops, runs, profit and its timer. */
	public static final ProfitTracker TRACKER = new ProfitTracker();

	/** The RNG meter, copied from the in-game menu and kept up to date after every run. */
	public static final RngMeter METER = new RngMeter();

	/** The latest Bazaar and lowest BIN prices. */
	public static final PriceBook PRICES = new PriceBook();

	/** Reads the loot bundle out of chat and feeds the tracker and the meter. */
	public static final TrackerEngine ENGINE = new TrackerEngine(TRACKER, METER, TrackerStore::save);

	/** A clock that only goes forward, in milliseconds. */
	static long nowMillis() {
		return System.nanoTime() / 1_000_000L;
	}

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
		TrackerStore.register();
		PriceService.register();
		MeterReader.register();
		TrackerHud.registerChatClicks();

		AdvancedNucRuns.LOGGER.info("Advanced NUC Runs loaded. Use /anc");
	}
}
