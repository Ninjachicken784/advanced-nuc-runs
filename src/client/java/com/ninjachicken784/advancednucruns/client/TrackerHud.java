package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import com.ninjachicken784.advancednucruns.Format;
import com.ninjachicken784.advancednucruns.NucleusItems;
import com.ninjachicken784.advancednucruns.PriceBook;
import com.ninjachicken784.advancednucruns.ProfitTracker;

/**
 * The Crystal Nucleus Profit Tracker panel: the best drops, the cost, the profit, the runs and the
 * timer, with a "Total / Session" switch and a "Pause" button that can be clicked while the chat
 * is open. The layout is worked out in plain numbers first so drawing, sizing and clicking agree.
 */
final class TrackerHud {
	private static final int PAD = 4;
	private static final int GAP = 4;
	private static final int BACKGROUND = 0x90000000;

	private static final int GOLD = 0xFFFFAA00;
	private static final int RED = 0xFFFF5555;
	private static final int GREEN = 0xFF55FF55;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF888888;
	private static final int AQUA = 0xFF55FFFF;
	private static final int BLUE = 0xFF5555FF;
	private static final int PURPLE = 0xFFB44BFF;

	private record Part(String text, int color) { }

	private record Seg(String text, int x, int y, int color) { }

	private record Zone(int x, int y, int width, int height) {
		boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	/**
	 * The panel at text size 1: what to draw where, how big it is and where the buttons are. The
	 * total panel has a "Total / Session" toggle (and no reset); the session panel has a "Reset"
	 * button instead. A button that the panel does not have is null.
	 */
	private record Layout(List<Seg> segments, int width, int height, Zone toggle, Zone reset, Zone pause) { }

	/** The two panels: the all-time tracker and the session tracker that is reset by hand. */
	private enum Panel { TOTAL, SESSION }

	private TrackerHud() { }

	private static Panel panelOf(HudBox box) {
		return box.kind == HudBox.Kind.SESSION ? Panel.SESSION : Panel.TOTAL;
	}

	private static ProfitTracker trackerOf(Panel panel) {
		return panel == Panel.SESSION ? AdvancedNucRunsClient.SESSION_TRACKER : AdvancedNucRunsClient.TRACKER;
	}

	static int styleColor(NucleusItems.Style style) {
		return switch (style) {
			case PURPLE -> PURPLE;
			case GREEN -> GREEN;
			default -> BLUE;
		};
	}

	private static Layout layout(Font font, boolean preview, Panel panel) {
		ProfitTracker tracker = trackerOf(panel);
		if (panel == Panel.SESSION) {
			// The session tracker uses the same price mode and number of rows as the main one.
			tracker.copySettingsFrom(AdvancedNucRunsClient.TRACKER);
		}
		PriceBook prices = AdvancedNucRunsClient.PRICES;
		long now = AdvancedNucRunsClient.nowMillis();
		// The session panel always shows everything its own tracker counted since its last reset.
		boolean session = panel == Panel.TOTAL && tracker.sessionView();

		List<ProfitTracker.Row> rows = tracker.topRows(prices, session);
		String cost;
		String profit;
		String perRun;
		String runs;
		String timer;
		boolean paused;
		if (preview && rows.isEmpty() && tracker.runs(session) == 0L) {
			// Nothing tracked yet: show an example so the panel can be placed.
			rows = List.of(
				new ProfitTracker.Row("a", "Divan's Alloy", 1, 1.45e9, true, NucleusItems.Style.BLUE),
				new ProfitTracker.Row("b", "Jaderald", 2, 80e6, true, NucleusItems.Style.PURPLE),
				new ProfitTracker.Row("c", "Jade Dye", 1, 40e6, true, NucleusItems.Style.GREEN));
			cost = "-542.42m";
			profit = "1.5B";
			perRun = "811.98k";
			runs = "1550";
			timer = "1D 12H 42min 32 sec";
			paused = false;
		} else {
			double totalCost = tracker.cost(prices, session);
			cost = ProfitTracker.costKnown(prices) ? (totalCost == 0d ? "0" : Format.coins(-totalCost)) : "?";
			profit = Format.coins(tracker.profit(prices, session));
			perRun = Format.coins(tracker.profitPerRun(prices, session));
			runs = String.valueOf(tracker.runs(session));
			timer = Format.duration(tracker.activeMillis(now, session));
			paused = tracker.isPaused(now);
		}

		// Every line is a list of coloured parts. The title and the last row are placed separately.
		String title = "Crystal Nucleus Profit Tracker";
		List<List<Part>> lines = new ArrayList<>();
		if (rows.isEmpty()) {
			lines.add(List.of(new Part("No drops yet", GRAY)));
		}
		for (ProfitTracker.Row row : rows) {
			lines.add(List.of(
				new Part(Format.count(row.count()) + "X ", WHITE),
				new Part(row.name() + " ", styleColor(row.style())),
				new Part(row.priced() ? Format.coins(row.value()) : "?", row.priced() ? GOLD : GRAY)));
		}
		int itemLines = lines.size();
		lines.add(List.of(new Part("Cost: ", RED), new Part(cost, RED)));
		lines.add(List.of(new Part("Profit: ", GOLD), new Part(profit, GOLD)));
		lines.add(List.of(new Part("Profit Per Run: ", GOLD), new Part(perRun, GOLD)));
		lines.add(List.of(new Part("Runs: ", WHITE), new Part(runs, WHITE)));
		lines.add(List.of(new Part("Timer: ", WHITE), new Part(timer, WHITE)));

		String toggleText = panel == Panel.SESSION ? "Session" : (session ? "Session" : "Total");
		String resetText = "Reset";
		String pauseText = paused ? "Resume" : "Pause";

		int lineHeight = font.lineHeight + 2;
		int contentWidth = font.width(title);
		for (List<Part> line : lines) {
			int w = 0;
			for (Part part : line) {
				w += font.width(part.text());
			}
			contentWidth = Math.max(contentWidth, w);
		}
		int buttonsWidth = font.width(toggleText) + 16 + font.width(pauseText);
		if (panel == Panel.SESSION) {
			buttonsWidth += 16 + font.width(resetText);
		}
		contentWidth = Math.max(contentWidth, buttonsWidth);
		int width = contentWidth + PAD * 2;

		List<Seg> segments = new ArrayList<>();
		int y = PAD;
		segments.add(new Seg(title, (width - font.width(title)) / 2, y, GOLD));
		y += lineHeight + 1;
		for (int i = 0; i < lines.size(); i++) {
			if (i == itemLines) {
				y += GAP;
			}
			int x = PAD;
			for (Part part : lines.get(i)) {
				segments.add(new Seg(part.text(), x, y, part.color()));
				x += font.width(part.text());
			}
			y += lineHeight;
		}
		y += GAP;

		int toggleWidth = font.width(toggleText);
		int pauseWidth = font.width(pauseText);
		int pauseX = width - PAD - pauseWidth;
		segments.add(new Seg(toggleText, PAD, y, AQUA));
		segments.add(new Seg(pauseText, pauseX, y, paused ? GREEN : RED));
		// On the total panel "Total / Session" is a button. On the session panel it is only a label.
		Zone toggle = panel == Panel.TOTAL ? new Zone(PAD - 1, y - 1, toggleWidth + 2, lineHeight) : null;
		Zone pause = new Zone(pauseX - 1, y - 1, pauseWidth + 2, lineHeight);
		Zone reset = null;
		if (panel == Panel.SESSION) {
			int resetWidth = font.width(resetText);
			int resetX = pauseX - 10 - resetWidth;
			segments.add(new Seg(resetText, resetX, y, RED));
			reset = new Zone(resetX - 1, y - 1, resetWidth + 2, lineHeight);
		}
		y += lineHeight + PAD;

		return new Layout(segments, width, y, toggle, reset, pause);
	}

	static int width(Font font, HudBox box, boolean preview) {
		return (int) Math.ceil(layout(font, preview, panelOf(box)).width() * box.scale);
	}

	static int height(Font font, HudBox box, boolean preview) {
		return (int) Math.ceil(layout(font, preview, panelOf(box)).height() * box.scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, HudBox box, boolean dim, boolean preview) {
		Layout layout = layout(font, preview, panelOf(box));
		int width = (int) Math.ceil(layout.width() * box.scale);
		int height = (int) Math.ceil(layout.height() * box.scale);
		box.keepOnScreen(width, height, graphics.guiWidth(), graphics.guiHeight());

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) box.left(width), (float) box.y);
		graphics.pose().scale(box.scale);
		graphics.fill(0, 0, layout.width(), layout.height(), BACKGROUND);
		for (Seg seg : layout.segments()) {
			graphics.text(font, seg.text(), seg.x(), seg.y(), dim ? GRAY : seg.color());
		}
		graphics.pose().popMatrix();
	}

	/** Clicks on "Total / Session", "Reset" and "Pause" of either panel. Returns true if the click hit one of them. */
	static boolean click(Font font, double mouseX, double mouseY) {
		// The box drawn last is on top, so it gets the click first (the Tools Tracker box sits over the others).
		return DivanHud.click(font, mouseX, mouseY) || click(font, GuiConfig.SESSION, mouseX, mouseY)
			|| click(font, GuiConfig.TRACKER, mouseX, mouseY);
	}

	private static boolean click(Font font, HudBox box, double mouseX, double mouseY) {
		if (box.hidden || !HudVisibility.shown(box)) {
			return false;
		}
		Panel panel = panelOf(box);
		Layout layout = layout(font, false, panel);
		int width = (int) Math.ceil(layout.width() * box.scale);
		double x = (mouseX - box.left(width)) / box.scale;
		double y = (mouseY - box.y) / box.scale;

		ProfitTracker tracker = trackerOf(panel);
		if (layout.toggle() != null && layout.toggle().contains(x, y)) {
			tracker.setSessionView(!tracker.sessionView());
			TrackerStore.save();
			return true;
		}
		if (layout.reset() != null && layout.reset().contains(x, y)) {
			SessionActions.reset();
			return true;
		}
		if (layout.pause().contains(x, y)) {
			tracker.togglePause(AdvancedNucRunsClient.nowMillis());
			TrackerStore.save();
			return true;
		}
		return false;
	}

	/** Lets the buttons be clicked while the chat is open, since the mouse is free there. */
	static void registerChatClicks() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof ChatScreen) {
				ScreenMouseEvents.allowMouseClick(screen).register((clicked, event) ->
					!(event.button() == 0 && click(client.font, event.x(), event.y())));
			}
		});
	}
}
