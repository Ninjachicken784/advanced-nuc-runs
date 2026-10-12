package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.RunStats;
import com.ninjachicken784.advancednucruns.RunTimer;

/**
 * The on-screen GUIs: the live timer, the Run Time / PB popup after a run, the PB, and the
 * session average. Where they are, how big, and whether they are hidden lives in {@link GuiConfig}.
 */
final class RunHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(AdvancedNucRuns.MOD_ID, "run_gui");

	/** How long the popup stays after a run. */
	private static final long POPUP_NANOS = 5_000_000_000L;

	/** The time shown on the popup while moving the GUIs. */
	private static final long SAMPLE_NANOS = 82_000_000_000L;

	/** What one box says: for example label "PB" and value "1min 22sec". */
	record Content(String label, String value) { }

	/** Set by /anc GUI. The screen opens on the next tick, after the chat box has closed. */
	private static volatile boolean openEditScreen;

	private static volatile String popupLabel;
	private static volatile long popupNanos;
	private static volatile long popupStart;

	private RunHud() { }

	static void register() {
		GuiConfig.load();
		HudElementRegistry.addLast(ID, RunHud::render);
		ClientTickEvents.END_CLIENT_TICK.register(RunHud::tick);
	}

	/** Asks for the move screen to open as soon as no other screen (such as chat) is open. */
	static void requestEditScreen() {
		openEditScreen = true;
	}

	/** Shows the popup with this label ("Run Time" or "PB") and run time for a few seconds. */
	static void showPopup(String label, long runNanos) {
		popupNanos = runNanos;
		popupStart = System.nanoTime();
		popupLabel = label;
	}

	private static void tick(Minecraft client) {
		if (openEditScreen && client.screen == null) {
			openEditScreen = false;
			client.setScreen(new GuiEditScreen());
		}
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		// The move screen draws the boxes itself, so skip them here to avoid drawing twice.
		if (client.screen instanceof GuiEditScreen) {
			return;
		}
		for (HudBox box : GuiConfig.ALL) {
			if (box.hidden) {
				continue;
			}
			Content content = content(box, false);
			if (content != null) {
				draw(graphics, client.font, box, content, false);
			}
		}
	}

	/**
	 * What a box says right now, or null if it has nothing to show (the popup between runs).
	 * With preview on, the popup shows a sample so it can be moved.
	 */
	static Content content(HudBox box, boolean preview) {
		RunTimer timer = AdvancedNucRunsClient.RUN_TIMER;
		RunStats stats = AdvancedNucRunsClient.RUN_STATS;
		return switch (box.kind) {
			case TIMER -> new Content("Timer", timer.state() == RunTimer.State.IDLE
				? "--"
				: RunTimer.formatShort(timer.elapsedNanos(System.nanoTime())));
			case RUN_TIME -> {
				String label = popupLabel;
				if (label != null && System.nanoTime() - popupStart < POPUP_NANOS) {
					yield new Content(label, RunTimer.formatShort(popupNanos));
				}
				yield preview ? new Content("Run Time", RunTimer.formatShort(SAMPLE_NANOS)) : null;
			}
			case PB -> new Content("PB", valueOrDashes(stats.pbNanos()));
			case AVERAGE -> new Content("Average", valueOrDashes(stats.averageNanos()));
		};
	}

	private static String valueOrDashes(long nanos) {
		return nanos < 0L ? "--" : RunTimer.formatShort(nanos);
	}

	static int width(Font font, HudBox box, Content content) {
		return HudText.width(font, content.label(), content.value(), box.scale);
	}

	static int height(Font font, HudBox box) {
		return HudText.height(font, box.scale);
	}

	static boolean contains(HudBox box, Content content, Font font, int mouseX, int mouseY) {
		return mouseX >= box.x && mouseX < box.x + width(font, box, content)
			&& mouseY >= box.y && mouseY < box.y + height(font, box);
	}

	/** Draws one box, keeping it on screen, for example after the window was made smaller. */
	static void draw(GuiGraphicsExtractor graphics, Font font, HudBox box, Content content, boolean dim) {
		box.x = Math.max(0, Math.min(box.x, graphics.guiWidth() - width(font, box, content)));
		box.y = Math.max(0, Math.min(box.y, graphics.guiHeight() - height(font, box)));

		HudText.draw(graphics, font, content.label(), content.value(), box.x, box.y, box.scale,
			dim ? HudText.LABEL_DIM : HudText.LABEL, dim ? HudText.VALUE_DIM : HudText.VALUE);
	}
}
