package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects the chat to the tracker and the RNG meter: when the loot bundle shows up, the run is
 * counted, the meter gains its XP, every item in the bundle is added to the tracker, and the
 * meter resets if the drop you selected was in it. Contains no Minecraft code, and the current
 * time is passed in, so it can be tested on its own.
 */
public final class TrackerEngine implements LootParser.Listener {
	private final ProfitTracker tracker;
	private final RngMeter meter;
	private final LootParser parser;
	private final Runnable onChange;
	private final List<String> droppedThisBundle = new ArrayList<>();
	private long now;

	/**
	 * @param onChange called whenever the numbers changed and should be saved
	 */
	public TrackerEngine(ProfitTracker tracker, RngMeter meter, Runnable onChange) {
		this.tracker = tracker;
		this.meter = meter;
		this.onChange = onChange;
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
		meter.onRun(tracker.pity());
		droppedThisBundle.clear();
		onChange.run();
	}

	@Override
	public void drop(String name, NucleusItems.Item item, long count) {
		if (item != null) {
			tracker.addDrop(item, count);
		} else {
			tracker.addCustomDrop(name, count);
		}
		droppedThisBundle.add(name);
	}

	@Override
	public void bundleEnded() {
		for (String name : droppedThisBundle) {
			meter.onDrop(name);
		}
		droppedThisBundle.clear();
		onChange.run();
	}
}
