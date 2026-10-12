package com.ninjachicken784.advancednucruns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.ninjachicken784.advancednucruns.RunStats;
import com.ninjachicken784.advancednucruns.RunTimer;

/**
 * What happens when a run is finished, whether it is real or a test from /anc Run: the stats are
 * updated, the popup appears (saying "PB" for a new best), and the time is printed in chat.
 */
final class RunResults {
	private RunResults() { }

	static void finished(long runNanos, boolean test) {
		RunStats.Result result = AdvancedNucRunsClient.RUN_STATS.record(runNanos);
		RunHud.showPopup(result.personalBest() ? "PB" : "Run Time", runNanos);
		if (result.personalBest()) {
			GuiConfig.save();
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(
				"Run finished in " + RunTimer.format(runNanos) + (test ? " (test run)" : ""))
				.withStyle(ChatFormatting.GREEN));
		}
	}
}
