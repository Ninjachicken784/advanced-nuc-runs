package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Every session the player has reset, newest first, so it can be looked at later: how many runs it
 * had, how long it took, what dropped and what it was worth when it was reset. Contains no
 * Minecraft code, so it can be tested on its own.
 */
public final class SessionHistory {
	/** One item that dropped in a session. */
	public record Drop(String name, long count, double value, boolean priced) { }

	/** One finished session. */
	public record Entry(long endedAtMillis, long runs, long activeMillis, double totalValue, double cost,
						boolean costKnown, double profit, List<Drop> drops) {
		public double profitPerRun() {
			return runs > 0L ? profit / runs : 0d;
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	/** Takes a snapshot of a tracker's numbers, worth what the prices say right now. */
	public static Entry snapshot(ProfitTracker tracker, PriceBook prices, long nowMillis, long epochMillis) {
		List<Drop> drops = new ArrayList<>();
		for (ProfitTracker.Row row : tracker.rows(prices, false)) {
			drops.add(new Drop(row.name(), row.count(), row.value(), row.priced()));
		}
		return new Entry(epochMillis, tracker.runs(false), tracker.activeMillis(nowMillis, false),
			tracker.totalValue(prices, false), tracker.cost(prices, false), ProfitTracker.costKnown(prices),
			tracker.profit(prices, false), List.copyOf(drops));
	}

	/** Adds a session at the top of the list. */
	public synchronized void add(Entry entry) {
		entries.add(0, entry);
	}

	/** All sessions, newest first. */
	public synchronized List<Entry> entries() {
		return List.copyOf(entries);
	}

	/** Removes the session at this position of {@link #entries()}. Returns true if there was one. */
	public synchronized boolean remove(int index) {
		if (index < 0 || index >= entries.size()) {
			return false;
		}
		entries.remove(index);
		return true;
	}

	public synchronized void save(Properties out) {
		out.setProperty("sessions.count", Integer.toString(entries.size()));
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			String p = "session." + i + ".";
			out.setProperty(p + "endedAt", Long.toString(e.endedAtMillis()));
			out.setProperty(p + "runs", Long.toString(e.runs()));
			out.setProperty(p + "active", Long.toString(e.activeMillis()));
			out.setProperty(p + "value", Double.toString(e.totalValue()));
			out.setProperty(p + "cost", Double.toString(e.cost()));
			out.setProperty(p + "costKnown", Boolean.toString(e.costKnown()));
			out.setProperty(p + "profit", Double.toString(e.profit()));
			out.setProperty(p + "drops", Integer.toString(e.drops().size()));
			for (int j = 0; j < e.drops().size(); j++) {
				Drop d = e.drops().get(j);
				String q = p + "drop." + j + ".";
				out.setProperty(q + "name", d.name());
				out.setProperty(q + "count", Long.toString(d.count()));
				out.setProperty(q + "value", Double.toString(d.value()));
				out.setProperty(q + "priced", Boolean.toString(d.priced()));
			}
		}
	}

	public synchronized void load(Properties in) {
		entries.clear();
		int count = (int) Math.min(100_000L, parseLong(in.getProperty("sessions.count")));
		for (int i = 0; i < count; i++) {
			String p = "session." + i + ".";
			if (in.getProperty(p + "runs") == null) {
				continue;
			}
			int dropCount = (int) Math.min(10_000L, parseLong(in.getProperty(p + "drops")));
			List<Drop> drops = new ArrayList<>();
			for (int j = 0; j < dropCount; j++) {
				String q = p + "drop." + j + ".";
				String name = in.getProperty(q + "name");
				if (name != null) {
					drops.add(new Drop(name, parseLong(in.getProperty(q + "count")),
						parseDouble(in.getProperty(q + "value")), Boolean.parseBoolean(in.getProperty(q + "priced", "false"))));
				}
			}
			entries.add(new Entry(parseLong(in.getProperty(p + "endedAt")), parseLong(in.getProperty(p + "runs")),
				parseLong(in.getProperty(p + "active")), parseDouble(in.getProperty(p + "value")),
				parseDouble(in.getProperty(p + "cost")), Boolean.parseBoolean(in.getProperty(p + "costKnown", "true")),
				parseDouble(in.getProperty(p + "profit")), List.copyOf(drops)));
		}
	}

	private static long parseLong(String text) {
		try {
			return text == null ? 0L : Math.max(0L, Long.parseLong(text.trim()));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static double parseDouble(String text) {
		try {
			double value = text == null ? 0d : Double.parseDouble(text.trim());
			return Double.isNaN(value) || Double.isInfinite(value) ? 0d : value;
		} catch (NumberFormatException e) {
			return 0d;
		}
	}
}
