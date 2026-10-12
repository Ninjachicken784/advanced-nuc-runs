package com.ninjachicken784.advancednucruns.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;

/**
 * The GUI boxes and what is saved between sessions: where each box is, how big it is, whether it
 * is hidden, and the PB. Everything is stored in config/advanced_nuc_runs.properties.
 */
final class GuiConfig {
	private static final String CONFIG_FILE = "advanced_nuc_runs.properties";
	private static final String PB_KEY = "pbNanos";

	static final HudBox TIMER = new HudBox(HudBox.Kind.TIMER, "timer", 5, 5);
	static final HudBox RUN_TIME = new HudBox(HudBox.Kind.RUN_TIME, "run_time", 5, 30);
	static final HudBox PB = new HudBox(HudBox.Kind.PB, "pb", 5, 55);
	static final HudBox AVERAGE = new HudBox(HudBox.Kind.AVERAGE, "average", 5, 80);

	/** Every box, in the order they are drawn. */
	static final List<HudBox> ALL = List.of(TIMER, RUN_TIME, PB, AVERAGE);

	private GuiConfig() { }

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
	}

	static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try (InputStream in = Files.newInputStream(path)) {
			Properties properties = new Properties();
			properties.load(in);

			for (HudBox box : ALL) {
				// Older versions only saved the timer, as timerX and timerY.
				String legacyX = box == TIMER ? "timerX" : null;
				String legacyY = box == TIMER ? "timerY" : null;
				box.x = readInt(properties, box.key + ".x", legacyX, box.x);
				box.y = readInt(properties, box.key + ".y", legacyY, box.y);
				box.hidden = Boolean.parseBoolean(properties.getProperty(box.key + ".hidden", "false").trim());

				String scale = properties.getProperty(box.key + ".scale");
				if (scale != null) {
					box.scale = HudBox.clampScale(Float.parseFloat(scale.trim()));
				}
			}

			String pb = properties.getProperty(PB_KEY);
			if (pb != null) {
				AdvancedNucRunsClient.RUN_STATS.restorePb(Long.parseLong(pb.trim()));
			}
		} catch (IOException | NumberFormatException e) {
			AdvancedNucRuns.LOGGER.warn("Could not read {}, using the defaults", path, e);
		}
	}

	static void save() {
		Properties properties = new Properties();
		for (HudBox box : ALL) {
			properties.setProperty(box.key + ".x", String.valueOf(box.x));
			properties.setProperty(box.key + ".y", String.valueOf(box.y));
			properties.setProperty(box.key + ".hidden", String.valueOf(box.hidden));
			properties.setProperty(box.key + ".scale", String.valueOf(box.scale));
		}
		long pb = AdvancedNucRunsClient.RUN_STATS.pbNanos();
		if (pb > 0L) {
			properties.setProperty(PB_KEY, String.valueOf(pb));
		}

		Path path = path();
		try (OutputStream out = Files.newOutputStream(path)) {
			properties.store(out, "Advanced NUC Runs");
		} catch (IOException e) {
			AdvancedNucRuns.LOGGER.warn("Could not save {}", path, e);
		}
	}

	private static int readInt(Properties properties, String key, String legacyKey, int fallback) {
		String value = properties.getProperty(key);
		if (value == null && legacyKey != null) {
			value = properties.getProperty(legacyKey);
		}
		return value == null ? fallback : Integer.parseInt(value.trim());
	}
}
