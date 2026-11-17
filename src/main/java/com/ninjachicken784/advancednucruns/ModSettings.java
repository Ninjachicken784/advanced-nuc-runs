package com.ninjachicken784.advancednucruns;

import java.util.Properties;

/**
 * The on/off switches of the mod, set from the /anc menu: the whole mod, the tracker, the GUI
 * boxes, and whether they show everywhere or only in the Crystal Hollows. Contains no Minecraft
 * code, so it can be tested on its own.
 */
public final class ModSettings {
	/** Where the tracker and the GUI boxes are shown. */
	public enum Visibility {
		ALWAYS,
		CRYSTAL_HOLLOWS
	}

	private volatile boolean modEnabled = true;
	private volatile boolean trackerEnabled = true;
	private volatile boolean guiEnabled = true;
	private volatile Visibility visibility = Visibility.ALWAYS;

	/** False turns everything off except the /anc command itself. */
	public boolean modEnabled() {
		return modEnabled;
	}

	public void setModEnabled(boolean on) {
		modEnabled = on;
	}

	/** The tracker counts drops and runs, and its panels and the RNG meter are shown. */
	public boolean trackerEnabled() {
		return trackerEnabled;
	}

	public void setTrackerEnabled(boolean on) {
		trackerEnabled = on;
	}

	/** The Timer, Run Time, PB and Average boxes are shown. */
	public boolean guiEnabled() {
		return guiEnabled;
	}

	public void setGuiEnabled(boolean on) {
		guiEnabled = on;
	}

	public Visibility visibility() {
		return visibility;
	}

	public void setVisibility(Visibility value) {
		visibility = value;
	}

	public void save(Properties out) {
		out.setProperty("settings.mod", Boolean.toString(modEnabled));
		out.setProperty("settings.tracker", Boolean.toString(trackerEnabled));
		out.setProperty("settings.gui", Boolean.toString(guiEnabled));
		out.setProperty("settings.visibility", visibility.name());
	}

	public void load(Properties in) {
		modEnabled = Boolean.parseBoolean(in.getProperty("settings.mod", "true").trim());
		trackerEnabled = Boolean.parseBoolean(in.getProperty("settings.tracker", "true").trim());
		guiEnabled = Boolean.parseBoolean(in.getProperty("settings.gui", "true").trim());
		try {
			visibility = Visibility.valueOf(in.getProperty("settings.visibility", "ALWAYS").trim());
		} catch (IllegalArgumentException e) {
			visibility = Visibility.ALWAYS;
		}
	}
}
