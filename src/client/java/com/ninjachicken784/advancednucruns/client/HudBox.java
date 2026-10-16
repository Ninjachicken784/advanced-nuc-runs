package com.ninjachicken784.advancednucruns.client;

/**
 * One movable, hideable, resizable box on the screen. Contains no Minecraft code so the
 * resizing and positioning can be tested on its own.
 *
 * <p>For a normal box, x is the left edge. For a centered box (the Run Time / PB popup), x is the
 * middle, so the text stays lined up when the label or the time changes length. For both, y is
 * the top. Positions are in GUI pixels.
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
		AVERAGE,
		/** The Crystal Nucleus Profit Tracker panel. */
		TRACKER,
		/** The RNG meter: the selected drop's icon and its progress. */
		METER
	}

	static final float DEFAULT_SCALE = 1.5f;
	static final float MIN_SCALE = 0.5f;
	static final float MAX_SCALE = 5.0f;
	private static final float SCALE_STEP = 0.1f;

	final Kind kind;
	/** Name used in the config file. */
	final String key;
	/** True if x is the middle of the box instead of its left edge. */
	final boolean centered;
	int x;
	int y;
	boolean hidden;
	/** How big the text is drawn. 1 is the normal Minecraft text size. */
	float scale;

	HudBox(Kind kind, String key, boolean centered, int defaultX, int defaultY) {
		this(kind, key, centered, defaultX, defaultY, DEFAULT_SCALE);
	}

	HudBox(Kind kind, String key, boolean centered, int defaultX, int defaultY, float defaultScale) {
		this.scale = defaultScale;
		this.kind = kind;
		this.key = key;
		this.centered = centered;
		this.x = defaultX;
		this.y = defaultY;
	}

	/** The left edge of the box when it is this wide. */
	int left(int width) {
		return centered ? x - width / 2 : x;
	}

	/** True if the point is inside the box when it is this wide and tall. */
	boolean contains(int width, int height, int pointX, int pointY) {
		int left = left(width);
		return pointX >= left && pointX < left + width && pointY >= y && pointY < y + height;
	}

	/** Moves the box back onto the screen if any part of it is outside. */
	void keepOnScreen(int width, int height, int guiWidth, int guiHeight) {
		int left = Math.max(0, Math.min(left(width), guiWidth - width));
		x = centered ? left + width / 2 : left;
		y = Math.max(0, Math.min(y, guiHeight - height));
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
