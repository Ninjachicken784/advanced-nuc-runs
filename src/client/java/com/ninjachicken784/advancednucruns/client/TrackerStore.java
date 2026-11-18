package com.ninjachicken784.advancednucruns.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;

/**
 * Saves the tracker, the RNG meter and the remembered prices to
 * config/advanced_nuc_runs_tracker.properties, the session tracker to
 * advanced_nuc_runs_session.properties and the reset sessions to advanced_nuc_runs_history.properties:
 * after every run, after every change in the tracker screen, once a minute (for the timers) and
 * when the game closes.
 */
final class TrackerStore {
	private static final String FILE = "advanced_nuc_runs_tracker.properties";
	private static final String SESSION_FILE = "advanced_nuc_runs_session.properties";
	private static final String HISTORY_FILE = "advanced_nuc_runs_history.properties";
	private static final int SAVE_EVERY_TICKS = 20 * 60;

	private static int ticks;

	private TrackerStore() { }

	private static Path path(String file) {
		return FabricLoader.getInstance().getConfigDir().resolve(file);
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
		read(FILE, properties -> {
			AdvancedNucRunsClient.TRACKER.load(properties);
			AdvancedNucRunsClient.METER.load(properties);
			AdvancedNucRunsClient.LAST_DROPS.load(properties);
			AdvancedNucRunsClient.RANK.load(properties);
			AdvancedNucRunsClient.PRICES.load(properties);
		});
		read(SESSION_FILE, AdvancedNucRunsClient.SESSION_TRACKER::load);
		read(HISTORY_FILE, AdvancedNucRunsClient.SESSION_HISTORY::load);
	}

	static synchronized void save() {
		long now = AdvancedNucRunsClient.nowMillis();

		Properties tracker = new Properties();
		AdvancedNucRunsClient.TRACKER.save(tracker, now);
		AdvancedNucRunsClient.METER.save(tracker);
		AdvancedNucRunsClient.LAST_DROPS.save(tracker);
		AdvancedNucRunsClient.RANK.save(tracker);
		AdvancedNucRunsClient.PRICES.save(tracker);
		write(FILE, tracker, "Advanced NUC Runs tracker");

		Properties session = new Properties();
		AdvancedNucRunsClient.SESSION_TRACKER.save(session, now);
		write(SESSION_FILE, session, "Advanced NUC Runs session tracker");

		Properties history = new Properties();
		AdvancedNucRunsClient.SESSION_HISTORY.save(history);
		write(HISTORY_FILE, history, "Advanced NUC Runs session history");
	}

	private static void read(String file, Consumer<Properties> into) {
		Path path = path(file);
		if (!Files.exists(path)) {
			return;
		}
		try (InputStream in = Files.newInputStream(path)) {
			Properties properties = new Properties();
			properties.load(in);
			into.accept(properties);
		} catch (IOException | RuntimeException e) {
			AdvancedNucRuns.LOGGER.warn("Could not read {}, starting it empty", path, e);
		}
	}

	/** Writes to a temporary file first, so a crash while saving cannot leave a half-written file. */
	private static void write(String file, Properties properties, String comment) {
		Path path = path(file);
		Path temporary = path(file + ".tmp");
		try {
			try (OutputStream out = Files.newOutputStream(temporary)) {
				properties.store(out, comment);
			}
			try {
				Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			AdvancedNucRuns.LOGGER.warn("Could not save {}", path, e);
		}
	}
}
