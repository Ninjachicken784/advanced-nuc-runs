package com.ninjachicken784.advancednucruns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.ninjachicken784.advancednucruns.Format;
import com.ninjachicken784.advancednucruns.ProfitTracker;
import com.ninjachicken784.advancednucruns.SessionHistory;

/** What the Reset button of the session tracker does. */
final class SessionActions {
	private SessionActions() { }

	/**
	 * Saves the session tracker's numbers to the history and starts it again from zero. A session
	 * with no runs and no drops is not saved, since there is nothing to remember.
	 */
	static void reset() {
		ProfitTracker session = AdvancedNucRunsClient.SESSION_TRACKER;
		Minecraft client = Minecraft.getInstance();

		if (session.isEmpty()) {
			message(client, "The session tracker is already empty.", ChatFormatting.YELLOW);
			return;
		}

		session.copySettingsFrom(AdvancedNucRunsClient.TRACKER);
		SessionHistory.Entry entry = SessionHistory.snapshot(session, AdvancedNucRunsClient.PRICES,
			AdvancedNucRunsClient.nowMillis(), System.currentTimeMillis());
		AdvancedNucRunsClient.SESSION_HISTORY.add(entry);
		session.reset();
		TrackerStore.save();

		message(client, "Session saved: " + entry.runs() + " runs, " + Format.coins(entry.profit())
			+ " profit. See it in /anc > History.",
			ChatFormatting.GREEN);
	}

	private static void message(Minecraft client, String text, ChatFormatting color) {
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(text).withStyle(color));
		}
	}
}
