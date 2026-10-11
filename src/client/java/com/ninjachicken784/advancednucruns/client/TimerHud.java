package com.ninjachicken784.advancednucruns.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.RunTimer;

/**
 * The run timer on the screen: a small box with the current time. Its position is saved in
 * the config folder and can be changed with /anc Timer move.
 */
final class TimerHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(AdvancedNucRuns.MOD_ID, "run_timer");
	private static final String CONFIG_FILE = "advanced_nuc_runs.properties";
	private static final int PADDING = 3;

	private static final int BACKGROUND = 0x90000000;
	private static final int BORDER = 0xFFFFFFFF;
	private static final int COLOR_IDLE = 0xFFAAAAAA;
	private static final int COLOR_RUNNING = 0xFFFFFF55;
	private static final int COLOR_FINISHED = 0xFF55FF55;

	/** Top-left corner of the box, in GUI pixels. */
	static int x = 5;
	static int y = 5;

	/** Set by /anc Timer move. The screen is opened on the next tick, after the chat box has closed. */
	private static volatile boolean openMoveScreen;

	private TimerHud() { }

	static void register() {
		load();
		HudElementRegistry.addLast(ID, TimerHud::render);
		ClientTickEvents.END_CLIENT_TICK.register(TimerHud::tick);
	}

	/** Asks for the move screen to open as soon as no other screen (such as chat) is open. */
	static void requestMoveScreen() {
		openMoveScreen = true;
	}

	private static void tick(Minecraft client) {
		if (openMoveScreen && client.screen == null) {
			openMoveScreen = false;
			client.setScreen(new TimerMoveScreen());
		}
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		// The move screen draws the timer itself, so skip it here to avoid drawing it twice.
		if (client.screen instanceof TimerMoveScreen) {
			return;
		}
		draw(graphics, client.font, false);
	}

	/** Draws the timer box. With highlight on, it gets a white border so it is easy to grab. */
	static void draw(GuiGraphicsExtractor graphics, Font font, boolean highlight) {
		String text = text();
		int width = boxWidth(font);
		int height = boxHeight(font);

		// Keep the box on screen, for example after the window was made smaller.
		x = Math.max(0, Math.min(x, graphics.guiWidth() - width));
		y = Math.max(0, Math.min(y, graphics.guiHeight() - height));

		graphics.fill(x, y, x + width, y + height, BACKGROUND);
		if (highlight) {
			graphics.fill(x - 1, y - 1, x + width + 1, y, BORDER);
			graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, BORDER);
			graphics.fill(x - 1, y, x, y + height, BORDER);
			graphics.fill(x + width, y, x + width + 1, y + height, BORDER);
		}
		graphics.text(font, text, x + PADDING, y + PADDING, color());
	}

	static boolean contains(int mouseX, int mouseY, Font font) {
		return mouseX >= x && mouseX < x + boxWidth(font)
			&& mouseY >= y && mouseY < y + boxHeight(font);
	}

	private static int boxWidth(Font font) {
		return font.width(text()) + PADDING * 2;
	}

	private static int boxHeight(Font font) {
		return font.lineHeight + PADDING * 2;
	}

	private static String text() {
		RunTimer timer = AdvancedNucRunsClient.RUN_TIMER;
		if (timer.state() == RunTimer.State.IDLE) {
			return "Run --:--";
		}
		return "Run " + RunTimer.format(timer.elapsedNanos(System.nanoTime()));
	}

	private static int color() {
		return switch (AdvancedNucRunsClient.RUN_TIMER.state()) {
			case IDLE -> COLOR_IDLE;
			case RUNNING -> COLOR_RUNNING;
			case FINISHED -> COLOR_FINISHED;
		};
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
	}

	private static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			return;
		}
		try (InputStream in = Files.newInputStream(path)) {
			Properties properties = new Properties();
			properties.load(in);
			x = Integer.parseInt(properties.getProperty("timerX", String.valueOf(x)).trim());
			y = Integer.parseInt(properties.getProperty("timerY", String.valueOf(y)).trim());
		} catch (IOException | NumberFormatException e) {
			AdvancedNucRuns.LOGGER.warn("Could not read {}, using the default timer position", path, e);
		}
	}

	static void save() {
		Properties properties = new Properties();
		properties.setProperty("timerX", String.valueOf(x));
		properties.setProperty("timerY", String.valueOf(y));
		Path path = configPath();
		try (OutputStream out = Files.newOutputStream(path)) {
			properties.store(out, "Advanced NUC Runs");
		} catch (IOException e) {
			AdvancedNucRuns.LOGGER.warn("Could not save {}", path, e);
		}
	}
}
