package com.ninjachicken784.advancednucruns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.DivanTracker;
import com.ninjachicken784.advancednucruns.LastDrops;
import com.ninjachicken784.advancednucruns.ModSettings;
import com.ninjachicken784.advancednucruns.PriceBook;
import com.ninjachicken784.advancednucruns.ProfitTracker;
import com.ninjachicken784.advancednucruns.RngMeter;
import com.ninjachicken784.advancednucruns.RunStats;
import com.ninjachicken784.advancednucruns.RankTracker;
import com.ninjachicken784.advancednucruns.RunTimer;
import com.ninjachicken784.advancednucruns.SessionHistory;
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

	/**
	 * The session tracker: counts the same drops and runs as {@link #TRACKER}, but starts again from
	 * zero whenever the player resets it. Every reset is kept in {@link #SESSION_HISTORY}.
	 */
	public static final ProfitTracker SESSION_TRACKER = new ProfitTracker();

	/** Every session that was reset, newest first. */
	public static final SessionHistory SESSION_HISTORY = new SessionHistory();

	/** When Alloy, Quick Claw and Jade Dye last dropped, in runs. */
	public static final LastDrops LAST_DROPS = new LastDrops();

	/** The Scavenged tools found with the Metal Detector, the sets, and the rates per hour. */
	public static final DivanTracker DIVAN = new DivanTracker();

	/** Your place on the Nucleus runs leaderboard and the goal, entered with /anc Rank. */
	public static final RankTracker RANK = new RankTracker();

	/** The on/off switches from the /anc menu. */
	public static final ModSettings SETTINGS = new ModSettings();

	/** Reads the loot bundle out of chat and feeds the trackers and the meter. */
	public static final TrackerEngine ENGINE = new TrackerEngine(TRACKER, SESSION_TRACKER, METER, LAST_DROPS,
		TrackerStore::save, AdvancedNucRunsClient::chatNotice);

	/** Prints a line in the player's own chat, for example "Last Alloy: 552 runs ago". */
	static void chatNotice(String text) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
		}
	}

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
		ChestFinderClient.register();
		RunHud.register();
		HudVisibility.register();
		RunChatListener.register();
		TrackerStore.register();
		PriceService.register();
		MeterReader.register();
		TrackerHud.registerChatClicks();

		AdvancedNucRuns.LOGGER.info("Advanced NUC Runs loaded. Use /anc");
	}
}
