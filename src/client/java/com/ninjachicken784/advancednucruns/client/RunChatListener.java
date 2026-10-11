package com.ninjachicken784.advancednucruns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import com.ninjachicken784.advancednucruns.RunTimer;

/**
 * Stops the run timer when the Crystal Nucleus loot bundle shows up in chat, and prints the time.
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

			long elapsed = AdvancedNucRunsClient.RUN_TIMER.finish(System.nanoTime());
			if (elapsed < 0L) {
				client.player.sendSystemMessage(Component.literal(
					"Loot received, but no run was being timed. Start one with /anc JT.").withStyle(ChatFormatting.YELLOW));
			} else {
				client.player.sendSystemMessage(Component.literal(
					"Run finished in " + RunTimer.format(elapsed)).withStyle(ChatFormatting.GREEN));
			}
		});
	}
}
