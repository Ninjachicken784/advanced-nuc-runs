package com.ninjachicken784.advancednucruns;

import java.util.Properties;

/**
 * The Crystal Nucleus RNG meter. The game keeps one pool of stored Nucleus XP and shows it against
 * what the drop you selected needs. Every run adds XP to the pool, and the pool resets when the
 * selected item drops. The numbers are copied from the game whenever the meter menu is opened.
 * Contains no Minecraft code so it can be tested on its own.
 */
public final class RngMeter {
	/** Nucleus XP a run adds. */
	public static final long XP_PER_RUN = 1000L;

	/** Nucleus XP a run adds with pity on. */
	public static final long XP_PER_RUN_PITY = 1100L;

	/** True once the stored XP has been copied from the game (or loaded from the save file). */
	private boolean known;
	private long storedXp;
	private String selectedName = "";
	private long required;

	/** True once the stored XP is known, so there is something to show. */
	public synchronized boolean hasData() {
		return known;
	}

	/** True if a drop is selected, so the XP can be shown against what it needs. */
	public synchronized boolean hasSelection() {
		return !selectedName.isEmpty() && required > 0L;
	}

	/**
	 * Copies the meter from the game's menu.
	 *
	 * @param newStoredXp    the stored XP, or a negative number if the menu did not show it
	 * @param name           the selected drop, or null if none is selected
	 * @param requiredXp     the XP the selected drop needs
	 * @param selectionKnown false if the menu could not tell which drop is selected, in which case
	 *                       the earlier selection is kept
	 */
	public synchronized void update(long newStoredXp, String name, long requiredXp, boolean selectionKnown) {
		if (newStoredXp >= 0L) {
			storedXp = newStoredXp;
			known = true;
		}
		if (selectionKnown) {
			if (name == null || name.isBlank() || requiredXp <= 0L) {
				selectedName = "";
				required = 0L;
			} else {
				selectedName = name.trim();
				required = requiredXp;
			}
		}
	}

	/** Adds the XP of one finished run. It is added whether or not a drop is selected. */
	public synchronized void onRun(boolean pity) {
		storedXp += pity ? XP_PER_RUN_PITY : XP_PER_RUN;
	}

	/** Resets the stored XP if this is the selected item. Returns true if it was. */
	public synchronized boolean onDrop(String droppedName) {
		if (hasSelection() && sameItem(droppedName, selectedName)) {
			storedXp = 0L;
			return true;
		}
		return false;
	}

	/** True if both names are the same drop, however they are written. */
	static boolean sameItem(String one, String other) {
		NucleusItems.Item first = NucleusItems.find(one);
		NucleusItems.Item second = NucleusItems.find(other);
		if (first != null && second != null) {
			return first.key().equals(second.key());
		}
		return !NucleusItems.normalize(one).isEmpty()
			&& NucleusItems.normalize(one).equals(NucleusItems.normalize(other));
	}

	public synchronized String selectedName() {
		return selectedName;
	}

	public synchronized long storedXp() {
		return storedXp;
	}

	public synchronized long required() {
		return required;
	}

	/** The text shown under the item, like "320.3k/1m", or just the stored XP if nothing is selected. */
	public synchronized String progressText() {
		return hasSelection() ? Format.xp(storedXp) + "/" + Format.xp(required) : Format.xp(storedXp);
	}

	public synchronized void save(Properties out) {
		out.setProperty("meter.known", Boolean.toString(known));
		out.setProperty("meter.xp", Long.toString(storedXp));
		out.setProperty("meter.name", selectedName);
		out.setProperty("meter.required", Long.toString(required));
	}

	public synchronized void load(Properties in) {
		known = Boolean.parseBoolean(in.getProperty("meter.known", "false").trim());
		storedXp = Math.max(0L, parse(in.getProperty("meter.xp")));
		selectedName = in.getProperty("meter.name", "").trim();
		required = Math.max(0L, parse(in.getProperty("meter.required")));
		if (selectedName.isEmpty() || required <= 0L) {
			selectedName = "";
			required = 0L;
		}
	}

	private static long parse(String text) {
		try {
			return text == null ? 0L : Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
