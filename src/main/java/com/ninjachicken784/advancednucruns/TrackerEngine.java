package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Connects the chat to the tracker and the RNG meter: when the loot bundle shows up, the run is
 * counted, the meter gains its XP, every item in the bundle is added to the tracker, and the
 * meter resets if the drop you selected was in it. Contains no Minecraft code, and the current
 * time is passed in, so it can be tested on its own.
 */
public final class TrackerEngine implements LootParser.Listener {
	private final ProfitTracker tracker;
	private final ProfitTracker sessionTracker;
	private final RngMeter meter;
	private final LastDrops lastDrops;
	private final LootParser parser;
	private final Runnable onChange;
	private final Consumer<String> notifier;
	private final List<String> droppedThisBundle = new ArrayList<>();
	private final List<String> noticesThisBundle = new ArrayList<>();
	private long now;

	/**
	 * @param tracker        the all-time tracker
	 * @param sessionTracker the tracker that is reset by hand; it counts the same runs and drops
	 * @param lastDrops      remembers when the rare drops last dropped
	 * @param onChange       called whenever the numbers changed and should be saved
	 * @param notifier       receives chat lines such as "Last Alloy: 552 runs ago"
	 */
	public TrackerEngine(ProfitTracker tracker, ProfitTracker sessionTracker, RngMeter meter, LastDrops lastDrops,
						 Runnable onChange, Consumer<String> notifier) {
		this.tracker = tracker;
		this.sessionTracker = sessionTracker;
		this.meter = meter;
		this.lastDrops = lastDrops;
		this.onChange = onChange;
		this.notifier = notifier;
		this.parser = new LootParser(this);
	}

	/** Feeds one chat message from the game. */
	public void onMessage(String message, long nowMillis) {
		now = nowMillis;
		parser.onMessage(message, nowMillis);
	}

	/** Call regularly so a bundle ends even if nothing follows it. */
	public void tick(long nowMillis) {
		now = nowMillis;
		parser.tick(nowMillis);
	}

	@Override
	public void runFinished() {
		tracker.runFinished(now);
		sessionTracker.runFinished(now);
		lastDrops.runFinished();
		meter.onRun(tracker.pity());
		droppedThisBundle.clear();
		noticesThisBundle.clear();
		onChange.run();
	}

	@Override
	public void drop(String name, NucleusItems.Item item, long count) {
		if (item != null) {
			tracker.addDrop(item, count);
			sessionTracker.addDrop(item, count);
			String shortName = LastDrops.shortName(item.key());
			if (shortName != null) {
				noticesThisBundle.add(LastDrops.message(shortName, lastDrops.dropped(item.key())));
			}
		} else {
			tracker.addCustomDrop(name, count);
			sessionTracker.addCustomDrop(name, count);
		}
		droppedThisBundle.add(name);
	}

	@Override
	public void bundleEnded() {
		for (String name : droppedThisBundle) {
			meter.onDrop(name);
		}
		droppedThisBundle.clear();
		if (notifier != null) {
			for (String notice : noticesThisBundle) {
				notifier.accept(notice);
			}
		}
		noticesThisBundle.clear();
		onChange.run();
	}
}
