package com.ninjachicken784.advancednucruns.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;

/**
 * Saves the tracker and the RNG meter to config/advanced_nuc_runs_tracker.properties: after every
 * run, after every change in the tracker screen, once a minute (for the timer) and when the game
 * closes.
 */
final class TrackerStore {
	private static final String FILE = "advanced_nuc_runs_tracker.properties";
	private static final int SAVE_EVERY_TICKS = 20 * 60;

	private static int ticks;

	private TrackerStore() { }

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE);
	}

	static void register() {
		load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticks >= SAVE_EVERY_TICKS) {
				ticks = 0;
				save();
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());
	}

	static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try (InputStream in = Files.newInputStream(path)) {
			Properties properties = new Properties();
			properties.load(in);
			AdvancedNucRunsClient.TRACKER.load(properties);
			AdvancedNucRunsClient.METER.load(properties);
		} catch (IOException | RuntimeException e) {
			AdvancedNucRuns.LOGGER.warn("Could not read {}, starting the tracker empty", path, e);
		}
	}

	static synchronized void save() {
		Properties properties = new Properties();
		AdvancedNucRunsClient.TRACKER.save(properties, AdvancedNucRunsClient.nowMillis());
		AdvancedNucRunsClient.METER.save(properties);

		Path path = path();
		try (OutputStream out = Files.newOutputStream(path)) {
			properties.store(out, "Advanced NUC Runs tracker");
		} catch (IOException e) {
			AdvancedNucRuns.LOGGER.warn("Could not save {}", path, e);
		}
	}
}
