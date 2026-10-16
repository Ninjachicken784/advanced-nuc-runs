package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The Crystal Nucleus profit tracker: how many of each item dropped, how many runs, how long it
 * took, and what that is worth. Everything is counted twice, for all time ("Total") and since
 * the game was started ("Session"). Contains no Minecraft code, and the current time is passed
 * in, so it can be tested on its own.
 *
 * <p>Profit is the value of everything that dropped minus what the runs cost: for every run,
 * one Precursor Apparatus, one Goblin Egg and one Jungle Key.
 */
public final class ProfitTracker {
	/** The timer pauses by itself after this long without a finished run. */
	public static final long AUTO_PAUSE_MILLIS = 5 * 60_000L;

	public static final int DEFAULT_ROWS = 3;
	public static final int MIN_ROWS = 3;
	public static final int MAX_ROWS = 20;

	private static final String CUSTOM_PREFIX = "CUSTOM_";

	/** One line of the tracker: an item, how many dropped and what they are worth. */
	public record Row(String key, String name, long count, double value, boolean priced,
					  NucleusItems.Style style) { }

	// Counts from earlier sessions (loaded from the save file) and from this session.
	private final Map<String, Long> baseCounts = new HashMap<>();
	private final Map<String, Long> sessionCounts = new HashMap<>();
	/** Display names of drops that are not in the built-in list, by key. */
	private final Map<String, String> customNames = new LinkedHashMap<>();
	private long baseRuns;
	private long sessionRuns;

	// The timer. It only counts time while a run was finished within the last few minutes.
	private long baseActiveMillis;
	private long bankedMillis;
	private long segmentStart = -1L;
	private long lastActivity;
	private boolean manualPaused;

	// Settings.
	private boolean pity;
	private PriceBook.Mode mode = PriceBook.Mode.SELL_OFFER;
	private boolean sessionView;
	private int rows = DEFAULT_ROWS;

	// ---- drops and runs

	/** Adds drops from a finished run. */
	public synchronized void addDrop(String key, String customName, long amount) {
		if (amount <= 0L) {
			return;
		}
		sessionCounts.merge(key, amount, Long::sum);
		if (customName != null) {
			customNames.putIfAbsent(key, customName);
		}
	}

	/** Adds drops of a built-in item. */
	public synchronized void addDrop(NucleusItems.Item item, long amount) {
		addDrop(item.key(), null, amount);
	}

	/** Adds drops of an item that is not in the built-in list. */
	public synchronized void addCustomDrop(String name, long amount) {
		addDrop(customKey(name), name, amount);
	}

	public static String customKey(String name) {
		return CUSTOM_PREFIX + NucleusItems.normalize(name).toUpperCase(java.util.Locale.ROOT);
	}

	/** How many of this item dropped, in all time or in this session. */
	public synchronized long count(String key, boolean session) {
		long inSession = sessionCounts.getOrDefault(key, 0L);
		return session ? inSession : baseCounts.getOrDefault(key, 0L) + inSession;
	}

	/** Sets the all-time count of an item, for example after it was typed into the GUI. */
	public synchronized void setTotalCount(String key, long total) {
		long wanted = Math.max(0L, total);
		long inSession = sessionCounts.getOrDefault(key, 0L);
		if (wanted >= inSession) {
			baseCounts.put(key, wanted - inSession);
		} else {
			// A session can never be bigger than the total.
			sessionCounts.put(key, wanted);
			baseCounts.put(key, 0L);
		}
	}

	/** Sets the count of an item in the view being shown (all time or this session). */
	public synchronized void setCount(String key, long value, boolean session) {
		if (session) {
			sessionCounts.put(key, Math.max(0L, value));
		} else {
			setTotalCount(key, value);
		}
	}

	/** Sets the run count in the view being shown (all time or this session). */
	public synchronized void setRuns(long value, boolean session) {
		if (session) {
			sessionRuns = Math.max(0L, value);
		} else {
			setTotalRuns(value);
		}
	}

	public synchronized long runs(boolean session) {
		return session ? sessionRuns : baseRuns + sessionRuns;
	}

	/** Sets the all-time run count, for example after it was typed into the GUI. */
	public synchronized void setTotalRuns(long total) {
		long wanted = Math.max(0L, total);
		if (wanted >= sessionRuns) {
			baseRuns = wanted - sessionRuns;
		} else {
			sessionRuns = wanted;
			baseRuns = 0L;
		}
	}

	/** A run finished: counts it and keeps the timer going. */
	public synchronized void runFinished(long now) {
		sessionRuns++;
		bankSegment(now);
		if (!manualPaused) {
			segmentStart = now;
			lastActivity = now;
		}
	}

	// ---- timer

	/** True if the timer is stopped, by the player or because no run finished for a while. */
	public synchronized boolean isPaused(long now) {
		return manualPaused || isAutoPaused(now);
	}

	private boolean isAutoPaused(long now) {
		return segmentStart >= 0L && now > lastActivity + AUTO_PAUSE_MILLIS;
	}

	/** Pauses the timer, or starts it again if it was paused. */
	public synchronized void togglePause(long now) {
		if (isPaused(now)) {
			bankSegment(now);
			manualPaused = false;
			segmentStart = now;
			lastActivity = now;
		} else {
			bankSegment(now);
			manualPaused = true;
		}
	}

	/** Time counted by the timer, in all time or in this session. */
	public synchronized long activeMillis(long now, boolean session) {
		long inSession = bankedMillis;
		if (segmentStart >= 0L) {
			long end = Math.min(now, lastActivity + AUTO_PAUSE_MILLIS);
			inSession += Math.max(0L, end - segmentStart);
		}
		return session ? inSession : baseActiveMillis + inSession;
	}

	private void bankSegment(long now) {
		if (segmentStart >= 0L) {
			long end = Math.min(now, lastActivity + AUTO_PAUSE_MILLIS);
			bankedMillis += Math.max(0L, end - segmentStart);
			segmentStart = -1L;
		}
	}

	// ---- settings

	public synchronized boolean pity() {
		return pity;
	}

	public synchronized void setPity(boolean on) {
		pity = on;
	}

	public synchronized PriceBook.Mode priceMode() {
		return mode;
	}

	public synchronized void setPriceMode(PriceBook.Mode newMode) {
		mode = newMode;
	}

	public synchronized boolean sessionView() {
		return sessionView;
	}

	public synchronized void setSessionView(boolean session) {
		sessionView = session;
	}

	public synchronized int rows() {
		return rows;
	}

	public synchronized void setRows(int count) {
		rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, count));
	}

	// ---- value and profit

	/** What one run costs: one of each of the three items it uses. Unknown prices count as 0. */
	public static double costPerRun(PriceBook prices) {
		double total = 0d;
		for (NucleusItems.RunCost item : NucleusItems.RUN_COSTS) {
			double cost = prices.buyCost(item);
			if (!Double.isNaN(cost)) {
				total += cost;
			}
		}
		return total;
	}

	/** True if all three run costs have a known price. */
	public static boolean costKnown(PriceBook prices) {
		for (NucleusItems.RunCost item : NucleusItems.RUN_COSTS) {
			if (Double.isNaN(prices.buyCost(item))) {
				return false;
			}
		}
		return true;
	}

	/** The rows of every item that has dropped, most valuable first. */
	public synchronized List<Row> rows(PriceBook prices, boolean session) {
		List<Row> result = new ArrayList<>();
		for (NucleusItems.Item item : NucleusItems.ALL) {
			long count = count(item.key(), session);
			if (count > 0L) {
				result.add(row(item.key(), item.name(), count, prices.price(item, mode), item.style()));
			}
		}
		for (Map.Entry<String, String> custom : customNames.entrySet()) {
			long count = count(custom.getKey(), session);
			if (count > 0L) {
				result.add(row(custom.getKey(), custom.getValue(), count,
					prices.priceByName(custom.getValue(), mode), NucleusItems.Style.BLUE));
			}
		}
		result.sort(Comparator.comparingDouble(Row::value).reversed().thenComparing(Row::name));
		return result;
	}

	/** The rows shown on the screen: the most valuable ones, as many as the rows setting allows. */
	public synchronized List<Row> topRows(PriceBook prices, boolean session) {
		List<Row> all = rows(prices, session);
		return all.subList(0, Math.min(rows, all.size()));
	}

	/** The price one of this item is worth right now, or NaN. */
	public synchronized double unitPrice(String key, PriceBook prices) {
		NucleusItems.Item item = NucleusItems.byKey(key);
		if (item != null) {
			return prices.price(item, mode);
		}
		String name = customNames.get(key);
		return name == null ? Double.NaN : prices.priceByName(name, mode);
	}

	/** Display names of drops that are not in the built-in list, by key. */
	public synchronized Map<String, String> customDrops() {
		return new LinkedHashMap<>(customNames);
	}

	/** Everything that dropped, worth what it would sell for. */
	public synchronized double totalValue(PriceBook prices, boolean session) {
		double total = 0d;
		for (Row row : rows(prices, session)) {
			total += row.value();
		}
		return total;
	}

	/** What the runs cost, as a positive number. */
	public synchronized double cost(PriceBook prices, boolean session) {
		return runs(session) * costPerRun(prices);
	}

	public synchronized double profit(PriceBook prices, boolean session) {
		return totalValue(prices, session) - cost(prices, session);
	}

	public synchronized double profitPerRun(PriceBook prices, boolean session) {
		long runs = runs(session);
		return runs > 0L ? profit(prices, session) / runs : 0d;
	}

	private static Row row(String key, String name, long count, double price, NucleusItems.Style style) {
		boolean priced = !Double.isNaN(price);
		return new Row(key, name, count, priced ? count * price : 0d, priced, style);
	}

	// ---- saving

	/** Forgets this session's counts, runs and time. The all-time numbers from earlier stay. */
	public synchronized void resetSession() {
		sessionCounts.clear();
		sessionRuns = 0L;
		bankedMillis = 0L;
		segmentStart = -1L;
	}

	/** Forgets all counts, runs and time. Settings stay. */
	public synchronized void reset() {
		baseCounts.clear();
		sessionCounts.clear();
		customNames.clear();
		baseRuns = 0L;
		sessionRuns = 0L;
		baseActiveMillis = 0L;
		bankedMillis = 0L;
		segmentStart = -1L;
		manualPaused = false;
	}

	/** Writes the all-time numbers and the settings. */
	public synchronized void save(Properties out, long now) {
		for (NucleusItems.Item item : NucleusItems.ALL) {
			long total = count(item.key(), false);
			if (total > 0L) {
				out.setProperty("count." + item.key(), Long.toString(total));
			}
		}
		for (Map.Entry<String, String> custom : customNames.entrySet()) {
			long total = count(custom.getKey(), false);
			if (total > 0L) {
				out.setProperty("count." + custom.getKey(), Long.toString(total));
				out.setProperty("custom." + custom.getKey(), custom.getValue());
			}
		}
		out.setProperty("tracker.runs", Long.toString(runs(false)));
		out.setProperty("tracker.activeMillis", Long.toString(activeMillis(now, false)));
		out.setProperty("tracker.pity", Boolean.toString(pity));
		out.setProperty("tracker.priceMode", mode.name());
		out.setProperty("tracker.rows", Integer.toString(rows));
	}

	/** Reads what {@link #save} wrote. The loaded numbers count as earlier sessions. */
	public synchronized void load(Properties in) {
		reset();
		for (String name : in.stringPropertyNames()) {
			if (name.startsWith("count.")) {
				long value = parse(in.getProperty(name));
				if (value > 0L) {
					baseCounts.put(name.substring("count.".length()), value);
				}
			} else if (name.startsWith("custom.")) {
				customNames.put(name.substring("custom.".length()), in.getProperty(name));
			}
		}
		baseRuns = Math.max(0L, parse(in.getProperty("tracker.runs")));
		baseActiveMillis = Math.max(0L, parse(in.getProperty("tracker.activeMillis")));
		pity = Boolean.parseBoolean(in.getProperty("tracker.pity", "false").trim());
		try {
			mode = PriceBook.Mode.valueOf(in.getProperty("tracker.priceMode", "SELL_OFFER").trim());
		} catch (IllegalArgumentException e) {
			mode = PriceBook.Mode.SELL_OFFER;
		}
		setRows((int) parse(in.getProperty("tracker.rows", Integer.toString(DEFAULT_ROWS))));
	}

	private static long parse(String text) {
		try {
			return text == null ? 0L : Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
