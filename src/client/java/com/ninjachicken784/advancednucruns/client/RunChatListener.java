package com.ninjachicken784.advancednucruns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import com.ninjachicken784.advancednucruns.RunTimer;

/**
 * Ends the current run when the Crystal Nucleus loot bundle shows up in chat, reports its time,
 * and starts timing the next run from that same moment.
 */
final class RunChatListener {
	private RunChatListener() { }

	static void register() {
		// Hypixel sends these lines as game messages, which players cannot fake from chat.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || !RunTimer.isFinishMessage(message.getString())) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				return;
			}

			long elapsed = AdvancedNucRunsClient.RUN_TIMER.finishRun(System.nanoTime());
			if (elapsed < 0L) {
				client.player.sendSystemMessage(Component.literal(
					"Timer started. Your next run is timed from this finish.").withStyle(ChatFormatting.YELLOW));
			} else {
				RunResults.finished(elapsed, false);
			}
		});
	}
}
