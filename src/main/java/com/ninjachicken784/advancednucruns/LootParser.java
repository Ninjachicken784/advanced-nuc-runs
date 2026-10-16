package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the Crystal Nucleus loot bundle out of the chat. Every chat line is fed in; the parser
 * notices the "CRYSTAL NUCLEUS LOOT BUNDLE" title, then reads the item lines under REWARDS
 * (like "Fine Ruby Gemstone x2") until the bundle ends. Contains no Minecraft code, and the
 * current time is passed in, so it can be tested on its own.
 */
public final class LootParser {
	/** What the parser reports. */
	public interface Listener {
		/** The bundle title was seen: a run just finished. */
		void runFinished();

		/**
		 * An item was in the bundle. The item is null for a drop that is not in the built-in list,
		 * in which case only the name is known.
		 */
		void drop(String name, NucleusItems.Item item, long count);

		/** The bundle is complete. */
		void bundleEnded();
	}

	/** The title line, normalized (letters and digits only), so a player's chat message cannot fake it. */
	private static final String TITLE = "crystalnucleuslootbundle";

	/** The bundle is over this long after its last line. */
	private static final long IDLE_MILLIS = 2500L;

	/** No bundle takes longer than this to arrive. */
	private static final long MAX_MILLIS = 10_000L;

	private static final int MAX_NAME_LENGTH = 48;

	/** A trailing " x2" or " ×12" on an item line. */
	private static final Pattern COUNT = Pattern.compile("^(.*?)\\s+[x×]\\s*([\\d,]+)$", Pattern.CASE_INSENSITIVE);

	private record Pending(String name, long count) { }

	private final Listener listener;
	private boolean collecting;
	private boolean sawKnownItem;
	private long startedAt;
	private long lastLineAt;
	private final List<Pending> unknownBeforeKnown = new ArrayList<>();

	public LootParser(Listener listener) {
		this.listener = listener;
	}

	/** True while a bundle is being read. */
	public boolean isCollecting() {
		return collecting;
	}

	/** Feeds one chat message. A message with several lines is read line by line. */
	public void onMessage(String message, long nowMillis) {
		if (message == null) {
			return;
		}
		for (String line : message.split("\\R")) {
			onLine(line, nowMillis);
		}
	}

	/** Call regularly (for example every tick) so a bundle ends even if nothing follows it. */
	public void tick(long nowMillis) {
		if (collecting && (nowMillis - lastLineAt > IDLE_MILLIS || nowMillis - startedAt > MAX_MILLIS)) {
			end();
		}
	}

	private void onLine(String rawLine, long now) {
		String line = clean(rawLine);

		if (NucleusItems.normalize(line).equals(TITLE)) {
			if (collecting) {
				end();
			}
			collecting = true;
			sawKnownItem = false;
			startedAt = now;
			lastLineAt = now;
			unknownBeforeKnown.clear();
			listener.runFinished();
			return;
		}
		if (!collecting || line.isEmpty()) {
			return;
		}
		lastLineAt = now;

		if (isSeparator(line)) {
			// A line of dashes after the items closes the bundle. One before them is just decoration.
			if (sawKnownItem) {
				end();
			}
			return;
		}
		if (NucleusItems.normalize(line).equals("rewards")) {
			return;
		}

		Matcher matcher = COUNT.matcher(stripTrailingSymbols(line));
		String name = (matcher.matches() ? matcher.group(1) : stripTrailingSymbols(line)).trim();
		long count = matcher.matches() ? parseCount(matcher.group(2)) : 1L;
		if (count < 1L) {
			return;
		}

		// Crystals (Ruby Crystal, Jasper Crystal) are not tracked.
		if (NucleusItems.normalize(name).endsWith("crystal")) {
			return;
		}

		NucleusItems.Item item = NucleusItems.find(name);
		if (item != null) {
			if (!sawKnownItem) {
				sawKnownItem = true;
				for (Pending pending : unknownBeforeKnown) {
					listener.drop(pending.name(), null, pending.count());
				}
				unknownBeforeKnown.clear();
			}
			listener.drop(item.name(), item, count);
		} else if (looksLikeItemName(name)) {
			// Maybe a brand new drop. It only counts once a known item shows this is a loot bundle.
			if (sawKnownItem) {
				listener.drop(cleanName(name), null, count);
			} else {
				unknownBeforeKnown.add(new Pending(cleanName(name), count));
			}
		}
	}

	private void end() {
		boolean hadItems = sawKnownItem;
		collecting = false;
		sawKnownItem = false;
		unknownBeforeKnown.clear();
		if (hadItems) {
			listener.bundleEnded();
		}
	}

	/** Removes colour codes (the section sign and the character after it) and trims. */
	static String clean(String text) {
		return text == null ? "" : text.replaceAll("§.", "").trim();
	}

	/** Removes icons and symbols after the last letter, digit or closing bracket. */
	private static String stripTrailingSymbols(String text) {
		int end = text.length();
		while (end > 0) {
			char c = text.charAt(end - 1);
			if (Character.isLetterOrDigit(c) || c == ')') {
				break;
			}
			end--;
		}
		return text.substring(0, end).trim();
	}

	/** Removes icons and symbols before the first letter or digit. */
	private static String cleanName(String name) {
		int start = 0;
		while (start < name.length() && !Character.isLetterOrDigit(name.charAt(start))) {
			start++;
		}
		return name.substring(start).trim();
	}

	private static boolean isSeparator(String line) {
		if (line.length() < 8) {
			return false;
		}
		for (int i = 0; i < line.length(); i++) {
			if (Character.isLetterOrDigit(line.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/** A short line of words, not something a player said or a system message. */
	private static boolean looksLikeItemName(String name) {
		String cleaned = cleanName(name);
		return !cleaned.isEmpty()
			&& cleaned.length() <= MAX_NAME_LENGTH
			&& Character.isUpperCase(cleaned.charAt(0))
			&& !cleaned.startsWith("You ")
			&& !cleaned.startsWith("Your ")
			&& cleaned.indexOf(':') < 0
			&& cleaned.indexOf('!') < 0
			&& cleaned.indexOf('[') < 0;
	}

	private static long parseCount(String digits) {
		try {
			return Long.parseLong(digits.replace(",", ""));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
