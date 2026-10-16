package com.ninjachicken784.advancednucruns;

import java.util.Locale;

/**
 * Turns numbers into the short text used on the screen: coins like 1.5B, XP like 320.3k, and
 * times like 1D 12H 42min 32 sec. Contains no Minecraft code so it can be tested on its own.
 */
public final class Format {
	private static final String[] COIN_UNITS = {"", "k", "m", "B", "T"};

	private Format() { }

	/** Coins as 950, 811.98k, 542.42m, 1.5B or 2.1T, with a minus sign when negative. */
	public static String coins(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return "?";
		}
		double abs = Math.abs(value);
		String text;
		if (abs < 1000d) {
			text = Long.toString(Math.round(abs));
		} else {
			int unit = 0;
			double scaled = abs;
			while (scaled >= 1000d && unit < COIN_UNITS.length - 1) {
				scaled /= 1000d;
				unit++;
			}
			// 999.999k rounds to 1000k, which should read 1m instead.
			if (round(scaled, 2) >= 1000d && unit < COIN_UNITS.length - 1) {
				scaled /= 1000d;
				unit++;
			}
			text = trim(scaled, 2) + COIN_UNITS[unit];
		}
		return value < 0 && !text.equals("0") ? "-" + text : text;
	}

	/** A count as a plain number up to 9999, then like coins (12.34k). */
	public static String count(long value) {
		return value < 10_000L ? Long.toString(value) : coins(value);
	}

	/** XP as 950, 320.3k or 1.25m: one decimal for thousands, two for millions. */
	public static String xp(long value) {
		long abs = Math.abs(value);
		String text;
		if (abs >= 999_950L) {
			text = trim(abs / 1_000_000d, 2) + "m";
		} else if (abs >= 1000L) {
			text = trim(abs / 1000d, 1) + "k";
		} else {
			text = Long.toString(abs);
		}
		return value < 0 ? "-" + text : text;
	}

	/** A time like "1D 12H 42min 32 sec". Units that are zero at the front are left out. */
	public static String duration(long millis) {
		long totalSeconds = Math.max(0L, millis) / 1000L;
		long days = totalSeconds / 86_400L;
		long hours = (totalSeconds % 86_400L) / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;

		StringBuilder text = new StringBuilder();
		if (days > 0L) {
			text.append(days).append("D ");
		}
		if (days > 0L || hours > 0L) {
			text.append(hours).append("H ");
		}
		if (days > 0L || hours > 0L || minutes > 0L) {
			text.append(minutes).append("min ");
		}
		text.append(seconds).append(" sec");
		return text.toString();
	}

	private static double round(double value, int decimals) {
		double factor = Math.pow(10, decimals);
		return Math.round(value * factor) / factor;
	}

	/** Rounds to at most this many decimals and drops trailing zeros: 1.50 -> 1.5, 2.00 -> 2. */
	private static String trim(double value, int decimals) {
		String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
		if (text.indexOf('.') >= 0) {
			text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return text;
	}
}
