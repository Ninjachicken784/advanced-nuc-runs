package com.ninjachicken784.advancednucruns.client;

/**
 * One movable, hideable, resizable box on the screen. The position is the top-left corner in
 * GUI pixels. Contains no Minecraft code so the resizing can be tested on its own.
 */
final class HudBox {
	enum Kind {
		/** The live timer counting the current run. */
		TIMER,
		/** The popup shown for a few seconds after a run: "Run Time", or "PB" for a new best. */
		RUN_TIME,
		/** The fastest run. */
		PB,
		/** The average of this session's runs. */
		AVERAGE
	}

	static final float DEFAULT_SCALE = 1.5f;
	static final float MIN_SCALE = 0.5f;
	static final float MAX_SCALE = 5.0f;
	private static final float SCALE_STEP = 0.1f;

	final Kind kind;
	/** Name used in the config file. */
	final String key;
	int x;
	int y;
	boolean hidden;
	/** How big the text is drawn. 1 is the normal Minecraft text size. */
	float scale = DEFAULT_SCALE;

	HudBox(Kind kind, String key, int defaultX, int defaultY) {
		this.kind = kind;
		this.key = key;
		this.x = defaultX;
		this.y = defaultY;
	}

	/**
	 * Makes the box one step bigger (direction above 0) or smaller (direction below 0), staying
	 * between the smallest and biggest allowed size. Returns true if the size changed.
	 */
	boolean resize(int direction) {
		float next = clampScale(Math.round((scale + Integer.signum(direction) * SCALE_STEP) * 100f) / 100f);
		boolean changed = next != scale;
		scale = next;
		return changed;
	}

	/** Keeps a size, for example one read from the config file, between the allowed limits. */
	static float clampScale(float value) {
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
	}
}
