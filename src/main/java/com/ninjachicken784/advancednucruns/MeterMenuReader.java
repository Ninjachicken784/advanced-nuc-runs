package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the game's Crystal Nucleus RNG Meter menu. The menu has an info item that says how much
 * Nucleus XP is stored, and one item per drop whose tooltip shows "Nucleus XP: 339,900/1,000,000".
 * Drops that are not selected say "Click to select!". Contains no Minecraft code, so it works on
 * plain text and can be tested on its own.
 */
public final class MeterMenuReader {
	/** One item of the menu: its name and its tooltip lines, without colour codes. */
	public record Slot(String name, List<String> lines) { }

	/**
	 * What the menu says.
	 *
	 * @param storedXp       the stored Nucleus XP, or -1 if the menu did not show it
	 * @param selectedName   the selected drop, or null if none
	 * @param required       the XP the selected drop needs, or 0
	 * @param selectionKnown false if the menu could not tell which drop is selected
	 * @param slotIndex      which slot holds the selected drop, or -1
	 */
	public record Result(long storedXp, String selectedName, long required, boolean selectionKnown, int slotIndex) { }

	private static final Pattern STORED = Pattern.compile("^\\s*stored\\s+nucleus\\s+xp\\s*:\\s*([\\d,]+)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern PROGRESS = Pattern.compile("^\\s*nucleus\\s+xp\\s*:\\s*([\\d,]+)\\s*/\\s*([\\d,]+)",
		Pattern.CASE_INSENSITIVE);

	private MeterMenuReader() { }

	/** Returns what the menu says, or null if these items are not a Nucleus RNG meter. */
	public static Result read(List<Slot> slots) {
		long stored = -1L;
		boolean noneSelected = false;
		List<Integer> drops = new ArrayList<>();
		List<Integer> notClickable = new ArrayList<>();
		List<Integer> markedSelected = new ArrayList<>();
		long firstDropXp = -1L;

		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if (slot == null || slot.lines() == null) {
				continue;
			}
			boolean isDrop = false;
			boolean clickToSelect = false;
			boolean selectedMark = false;

			for (String raw : slot.lines()) {
				String line = clean(raw);
				Matcher info = STORED.matcher(line);
				if (info.find()) {
					stored = number(info.group(1));
				}
				if (NucleusItems.normalize(line).contains("donthaveanrngdropselected")) {
					noneSelected = true;
				}
				if (PROGRESS.matcher(line).find()) {
					isDrop = true;
				}
				String normal = NucleusItems.normalize(line);
				if (normal.contains("clicktoselect")) {
					clickToSelect = true;
				} else if (normal.contains("selected") && !normal.contains("yourselected") && !normal.contains("donthave")) {
					selectedMark = true;
				}
			}

			if (isDrop) {
				drops.add(i);
				if (!clickToSelect) {
					notClickable.add(i);
				}
				if (selectedMark) {
					markedSelected.add(i);
				}
				if (firstDropXp < 0L) {
					firstDropXp = progress(slot, true);
				}
			}
		}

		if (drops.isEmpty() && stored < 0L) {
			return null;
		}
		if (stored < 0L) {
			stored = firstDropXp;
		}

		if (noneSelected) {
			return new Result(stored, null, 0L, true, -1);
		}

		// The selected drop is the one that does not offer "Click to select!". If the game marks it
		// some other way too, that mark settles it when several are left.
		int chosen = -1;
		if (notClickable.size() == 1) {
			chosen = notClickable.get(0);
		} else if (!notClickable.isEmpty()) {
			List<Integer> both = new ArrayList<>(notClickable);
			both.retainAll(markedSelected);
			if (both.size() == 1) {
				chosen = both.get(0);
			}
		} else if (markedSelected.size() == 1) {
			chosen = markedSelected.get(0);
		}

		if (chosen < 0) {
			return new Result(stored, null, 0L, false, -1);
		}
		Slot slot = slots.get(chosen);
		return new Result(stored, clean(slot.name()), progress(slot, false), true, chosen);
	}

	/** The XP a drop's tooltip shows: the stored part (first number) or the needed part (second). */
	private static long progress(Slot slot, boolean stored) {
		for (String raw : slot.lines()) {
			Matcher matcher = PROGRESS.matcher(clean(raw));
			if (matcher.find()) {
				return number(matcher.group(stored ? 1 : 2));
			}
		}
		return -1L;
	}

	private static String clean(String text) {
		return text == null ? "" : text.replaceAll("§.", "").trim();
	}

	private static long number(String digits) {
		try {
			return Long.parseLong(digits.replace(",", ""));
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	/** True if a screen title is the Crystal Nucleus RNG Meter. */
	public static boolean isMeterTitle(String title) {
		String normal = NucleusItems.normalize(title).toLowerCase(Locale.ROOT);
		return normal.contains("nucleus") && normal.contains("rngmeter");
	}
}
