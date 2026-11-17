package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.ninjachicken784.advancednucruns.ModSettings;

/**
 * Decides what is shown right now: nothing while the mod is off, only the boxes whose group
 * (tracker or GUI) is on, and, if the menu says so, only while the player is in the Crystal
 * Hollows. The area is read from the "Area:" line of the tab list, once a second.
 */
final class HudVisibility {
	private static final int CHECK_EVERY_TICKS = 20;

	private static volatile boolean inCrystalHollows;
	private static int ticks;

	private HudVisibility() { }

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticks >= CHECK_EVERY_TICKS) {
				ticks = 0;
				inCrystalHollows = detectCrystalHollows(client);
			}
		});
	}

	/** True while the tab list says the player is in the Crystal Hollows. */
	static boolean inCrystalHollows() {
		return inCrystalHollows;
	}

	private static boolean detectCrystalHollows(Minecraft client) {
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			return false;
		}
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			Component name = info.getTabListDisplayName();
			if (name == null) {
				continue;
			}
			String text = name.getString().replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT);
			if (text.contains("area:") && text.contains("crystal hollows")) {
				return true;
			}
		}
		return false;
	}

	/** True if the whole mod is on. */
	static boolean modOn() {
		return AdvancedNucRunsClient.SETTINGS.modEnabled();
	}

	/** True if the tracker should count drops and runs. */
	static boolean trackerOn() {
		ModSettings settings = AdvancedNucRunsClient.SETTINGS;
		return settings.modEnabled() && settings.trackerEnabled();
	}

	/** True if the box may be drawn on the normal game screen right now (hiding by hand is checked separately). */
	static boolean shown(HudBox box) {
		ModSettings settings = AdvancedNucRunsClient.SETTINGS;
		if (!settings.modEnabled()) {
			return false;
		}
		boolean trackerGroup = box.kind == HudBox.Kind.TRACKER || box.kind == HudBox.Kind.SESSION
			|| box.kind == HudBox.Kind.METER;
		if (trackerGroup ? !settings.trackerEnabled() : !settings.guiEnabled()) {
			return false;
		}
		return settings.visibility() == ModSettings.Visibility.ALWAYS || inCrystalHollows;
	}
}
