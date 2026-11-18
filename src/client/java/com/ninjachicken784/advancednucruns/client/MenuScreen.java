package com.ninjachicken784.advancednucruns.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import com.ninjachicken784.advancednucruns.Format;
import com.ninjachicken784.advancednucruns.ModSettings;
import com.ninjachicken784.advancednucruns.SessionHistory;

/**
 * The /anc menu. A column of tabs on the left, and on the right the rows of the open tab: a name
 * with a short explanation and a button. Tabs: General (mod on/off, where things are shown),
 * Tracker, GUI and History (every session that was reset). Drawn by hand, the same way as the
 * tracker screen: every frame lists the clickable areas, and a click looks them up.
 */
final class MenuScreen extends Screen {
	private enum Tab {
		GENERAL("General"),
		TRACKER("Tracker"),
		GUI("GUI"),
		RANK("Rank"),
		HISTORY("History");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static final int SIDEBAR = 112;
	private static final int TAB_HEIGHT = 26;
	private static final int ROW_HEIGHT = 44;
	private static final int BUTTON_WIDTH = 112;
	private static final int BUTTON_HEIGHT = 18;
	private static final int LIST_ROW = 16;
	private static final long CONFIRM_MILLIS = 3000L;

	private static final int DIM = 0xB0000000;
	private static final int PANEL = 0xFF1B1B1B;
	private static final int EDGE = 0xFF555555;
	private static final int TAB_BG = 0xFF262626;
	private static final int TAB_ACTIVE = 0xFF3F3F3F;
	private static final int BUTTON_BG = 0xFF3A3A3A;
	private static final int BUTTON_HOVER = 0xFF5A5A5A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF999999;
	private static final int GOLD = 0xFFFFAA00;
	private static final int RED = 0xFFFF5555;
	private static final int GREEN = 0xFF55FF55;
	private static final int AQUA = 0xFF55FFFF;

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH);

	private interface Click {
		void run();
	}

	/** One line of the details of a session. */
	private record Line(String text, int color) { }

	private record Zone(int x, int y, int width, int height, Click click) {
		boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private final List<Zone> zones = new ArrayList<>();
	private Tab tab = Tab.GENERAL;

	// History tab.
	private int selected = -1;
	private int listScroll;
	private int detailScroll;
	private int listTop;
	private int listBottom;
	private int listLeft;
	private int listRight;
	private int detailLeft;
	private int detailRight;
	private int detailTop;
	private int detailBottom;
	private boolean confirmingDelete;
	private long confirmUntil;

	MenuScreen() {
		super(Component.literal("Advanced Nuc Runs"));
	}

	private static ModSettings settings() {
		return AdvancedNucRunsClient.SETTINGS;
	}

	/** Saves the switches and the box visibility. */
	private static void saveSettings() {
		GuiConfig.save();
	}

	// ---- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		zones.clear();
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		graphics.fill(0, 0, guiWidth, guiHeight, DIM);

		int width = Math.min(guiWidth - 16, 460);
		int height = Math.min(guiHeight - 16, 262);
		int left = (guiWidth - width) / 2;
		int top = (guiHeight - height) / 2;

		graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, EDGE);
		graphics.fill(left, top, left + width, top + height, PANEL);
		graphics.fill(left + SIDEBAR, top, left + SIDEBAR + 1, top + height, EDGE);

		// The tabs.
		graphics.centeredText(this.font, "Advanced Nuc Runs", left + SIDEBAR / 2, top + 8, GOLD);
		int tabY = top + 24;
		for (Tab entry : Tab.values()) {
			Tab target = entry;
			boolean active = entry == tab;
			int color = active ? TAB_ACTIVE : TAB_BG;
			boolean hover = mouseX >= left + 4 && mouseX < left + SIDEBAR - 4 && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT - 3;
			graphics.fill(left + 4, tabY, left + SIDEBAR - 4, tabY + TAB_HEIGHT - 3, hover && !active ? BUTTON_HOVER : color);
			graphics.text(this.font, entry.label, left + 12, tabY + (TAB_HEIGHT - 3 - this.font.lineHeight) / 2 + 1,
				active ? GOLD : WHITE);
			zones.add(new Zone(left + 4, tabY, SIDEBAR - 8, TAB_HEIGHT - 3, () -> openTab(target)));
			tabY += TAB_HEIGHT;
		}

		int contentLeft = left + SIDEBAR + 10;
		int contentRight = left + width - 10;
		int contentTop = top + 10;
		int contentBottom = top + height - 10;

		graphics.text(this.font, tab.label, contentLeft, contentTop, GOLD);
		contentTop += 16;

		switch (tab) {
			case GENERAL -> drawGeneral(graphics, mouseX, mouseY, contentLeft, contentRight, contentTop);
			case TRACKER -> drawTracker(graphics, mouseX, mouseY, contentLeft, contentRight, contentTop);
			case GUI -> drawGui(graphics, mouseX, mouseY, contentLeft, contentRight, contentTop);
			case RANK -> drawRank(graphics, mouseX, mouseY, contentLeft, contentRight, contentTop);
			case HISTORY -> drawHistory(graphics, mouseX, mouseY, contentLeft, contentRight, contentTop, contentBottom);
		}

		graphics.centeredText(this.font, "Esc closes. Everything is saved when you change it.", guiWidth / 2,
			top + height + 6, GRAY);
	}

	private void openTab(Tab target) {
		tab = target;
		confirmingDelete = false;
	}

	/**
	 * One row: a name, a short explanation under it (wrapped so it stays left of the button), and
	 * a button on the right.
	 */
	private int row(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int y, String name,
					String description, String buttonText, int buttonColor, Click click) {
		int buttonX = right - BUTTON_WIDTH;
		int textWidth = buttonX - 8 - left;
		graphics.text(this.font, name, left, y + 3, WHITE);
		int lineY = y + 3 + this.font.lineHeight + 3;
		for (String line : wrap(description, textWidth)) {
			graphics.text(this.font, line, left, lineY, GRAY);
			lineY += this.font.lineHeight + 2;
		}
		button(graphics, mouseX, mouseY, buttonX, y + (ROW_HEIGHT - 3 - BUTTON_HEIGHT) / 2, BUTTON_WIDTH, BUTTON_HEIGHT,
			buttonText, buttonColor, click);
		graphics.fill(left, y + ROW_HEIGHT - 3, right, y + ROW_HEIGHT - 2, TAB_BG);
		return y + ROW_HEIGHT;
	}

	/** Breaks a text into lines that are at most this wide on screen. */
	private List<String> wrap(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
			String attempt = current.length() == 0 ? word : current + " " + word;
			if (current.length() > 0 && this.font.width(attempt) > maxWidth) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(attempt);
			}
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines;
	}

	private void drawGeneral(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int y) {
		ModSettings s = settings();
		y = row(graphics, mouseX, mouseY, left, right, y, "Mod",
			"Off turns everything off: boxes, tracker, waypoints, timer.",
			s.modEnabled() ? "ON" : "OFF", s.modEnabled() ? GREEN : RED, () -> {
				s.setModEnabled(!s.modEnabled());
				saveSettings();
			});
		boolean always = s.visibility() == ModSettings.Visibility.ALWAYS;
		row(graphics, mouseX, mouseY, left, right, y, "Show tracker and GUI",
			"Always, or only while you are in the Crystal Hollows.",
			always ? "Always" : "Crystal Hollows only", AQUA, () -> {
				s.setVisibility(always ? ModSettings.Visibility.CRYSTAL_HOLLOWS : ModSettings.Visibility.ALWAYS);
				saveSettings();
			});
	}

	private void drawTracker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int y) {
		ModSettings s = settings();
		y = row(graphics, mouseX, mouseY, left, right, y, "Tracker",
			"Off: nothing is counted and the panels and meter are hidden.",
			s.trackerEnabled() ? "ON" : "OFF", s.trackerEnabled() ? GREEN : RED, () -> {
				s.setTrackerEnabled(!s.trackerEnabled());
				saveSettings();
			});
		y = row(graphics, mouseX, mouseY, left, right, y, "Total panel",
			"The tracker with everything ever counted.",
			GuiConfig.TRACKER.hidden ? "Hidden" : "Shown", GuiConfig.TRACKER.hidden ? RED : GREEN, () -> {
				GuiConfig.TRACKER.hidden = !GuiConfig.TRACKER.hidden;
				saveSettings();
			});
		y = row(graphics, mouseX, mouseY, left, right, y, "Session panel",
			"The tracker you reset yourself. Reset saves it to History.",
			GuiConfig.SESSION.hidden ? "Hidden" : "Shown", GuiConfig.SESSION.hidden ? RED : GREEN, () -> {
				GuiConfig.SESSION.hidden = !GuiConfig.SESSION.hidden;
				saveSettings();
			});
		row(graphics, mouseX, mouseY, left, right, y, "Open Tracker",
			"Change drops and runs, prices, rows on screen.",
			"Open", WHITE, () -> Minecraft.getInstance().setScreen(new TrackerScreen(this)));
	}

	private void drawGui(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int y) {
		ModSettings s = settings();
		y = row(graphics, mouseX, mouseY, left, right, y, "GUI",
			"Off hides the Timer, Run Time, PB and Average boxes.",
			s.guiEnabled() ? "ON" : "OFF", s.guiEnabled() ? GREEN : RED, () -> {
				s.setGuiEnabled(!s.guiEnabled());
				saveSettings();
			});
		row(graphics, mouseX, mouseY, left, right, y, "Change GUI",
			"Move, resize or hide every box.",
			"Open", WHITE, () -> Minecraft.getInstance().setScreen(new GuiEditScreen(this)));
	}

	private void drawRank(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int y) {
		y = row(graphics, mouseX, mouseY, left, right, y, "Rank box",
			"Your leaderboard place, your runs and the runs left to your goal.",
			GuiConfig.RANK.hidden ? "Hidden" : "Shown", GuiConfig.RANK.hidden ? RED : GREEN, () -> {
				GuiConfig.RANK.hidden = !GuiConfig.RANK.hidden;
				saveSettings();
			});

		y += 4;
		for (String line : wrap("Now: " + AncCommand.rankStatus(), right - left)) {
			graphics.text(this.font, line, left, y, AQUA);
			y += this.font.lineHeight + 2;
		}
		y += 6;
		for (String line : wrap("Read your place on the leaderboard (for example with the SkyHelper bot) and type it"
			+ " in. The mod then adds every run you finish.", right - left)) {
			graphics.text(this.font, line, left, y, GRAY);
			y += this.font.lineHeight + 2;
		}
		y += 6;
		String[][] commands = {
			{"/anc Rank 91 3046", "your place and your runs"},
			{"/anc Rank next 3047", "runs of the player above you"},
			{"/anc Rank goal 10 8800", "the place you want + its runs"},
			{"/anc Rank clear", "forget all of it"}
		};
		for (String[] command : commands) {
			graphics.text(this.font, command[0], left, y, GOLD);
			graphics.text(this.font, command[1], left + 126, y, GRAY);
			y += this.font.lineHeight + 4;
		}
	}

	// ---- history tab

	private void drawHistory(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int right, int top,
							 int bottom) {
		List<SessionHistory.Entry> entries = AdvancedNucRunsClient.SESSION_HISTORY.entries();
		if (selected >= entries.size()) {
			selected = entries.isEmpty() ? -1 : entries.size() - 1;
		}

		long now = AdvancedNucRunsClient.nowMillis();
		int buttonY = bottom - BUTTON_HEIGHT;
		button(graphics, mouseX, mouseY, left, buttonY, 140, BUTTON_HEIGHT, "Reset session now", RED, SessionActions::reset);

		boolean confirm = confirmingDelete && now < confirmUntil;
		int deleteX = right - 130;
		if (selected >= 0) {
			button(graphics, mouseX, mouseY, deleteX, buttonY, 130, BUTTON_HEIGHT,
				confirm ? "Click again to confirm" : "Delete this entry", RED, () -> {
					if (confirm) {
						AdvancedNucRunsClient.SESSION_HISTORY.remove(selected);
						TrackerStore.save();
						confirmingDelete = false;
					} else {
						confirmingDelete = true;
						confirmUntil = AdvancedNucRunsClient.nowMillis() + CONFIRM_MILLIS;
					}
				});
		}

		listLeft = left;
		listRight = left + 150;
		listTop = top;
		listBottom = buttonY - 6;
		detailLeft = listRight + 8;
		detailRight = right;
		detailTop = top;
		detailBottom = buttonY - 6;

		// The list of sessions, newest first.
		graphics.fill(listLeft, listTop, listRight, listBottom, EDGE);
		graphics.fill(listLeft + 1, listTop + 1, listRight - 1, listBottom - 1, 0xFF111111);
		if (entries.isEmpty()) {
			graphics.text(this.font, "No sessions yet.", listLeft + 5, listTop + 6, GRAY);
			graphics.text(this.font, "Click Reset on the", listLeft + 5, listTop + 6 + 12, GRAY);
			graphics.text(this.font, "session panel to save one.", listLeft + 5, listTop + 6 + 24, GRAY);
		}
		int visible = Math.max(1, (listBottom - listTop - 2) / LIST_ROW);
		int maxScroll = Math.max(0, entries.size() - visible);
		listScroll = Math.max(0, Math.min(listScroll, maxScroll));
		for (int i = 0; i < visible && listScroll + i < entries.size(); i++) {
			int index = listScroll + i;
			SessionHistory.Entry entry = entries.get(index);
			int y = listTop + 1 + i * LIST_ROW;
			boolean hover = mouseX >= listLeft + 1 && mouseX < listRight - 1 && mouseY >= y && mouseY < y + LIST_ROW;
			if (index == selected) {
				graphics.fill(listLeft + 1, y, listRight - 1, y + LIST_ROW, TAB_ACTIVE);
			} else if (hover) {
				graphics.fill(listLeft + 1, y, listRight - 1, y + LIST_ROW, TAB_BG);
			}
			String text = date(entry) + "  " + Format.count(entry.runs()) + " runs";
			graphics.text(this.font, text, listLeft + 5, y + (LIST_ROW - this.font.lineHeight) / 2 + 1,
				index == selected ? GOLD : WHITE);
			zones.add(new Zone(listLeft + 1, y, listRight - listLeft - 2, LIST_ROW, () -> {
				selected = index;
				detailScroll = 0;
				confirmingDelete = false;
			}));
		}
		if (maxScroll > 0) {
			int trackHeight = listBottom - listTop - 2;
			int barHeight = Math.max(8, trackHeight * visible / entries.size());
			int barY = listTop + 1 + (trackHeight - barHeight) * listScroll / maxScroll;
			graphics.fill(listRight - 3, barY, listRight - 1, barY + barHeight, GRAY);
		}

		// The details of the selected session.
		graphics.fill(detailLeft, detailTop, detailRight, detailBottom, EDGE);
		graphics.fill(detailLeft + 1, detailTop + 1, detailRight - 1, detailBottom - 1, 0xFF111111);
		if (selected >= 0 && selected < entries.size()) {
			drawDetails(graphics, entries.get(selected));
		} else if (!entries.isEmpty()) {
			graphics.text(this.font, "Click a session.", detailLeft + 5, detailTop + 6, GRAY);
		}
	}

	private void drawDetails(GuiGraphicsExtractor graphics, SessionHistory.Entry e) {
		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Date: " + date(e), WHITE));
		lines.add(new Line("In these " + e.runs() + " runs you got:", AQUA));
		for (SessionHistory.Drop drop : e.drops()) {
			lines.add(new Line(Format.count(drop.count()) + "X " + drop.name() + " "
				+ (drop.priced() ? Format.coins(drop.value()) : "?"), GOLD));
		}
		if (e.drops().isEmpty()) {
			lines.add(new Line("No drops.", GRAY));
		}
		lines.add(new Line("Cost: " + (e.costKnown() ? Format.coins(-e.cost()) : "?"), RED));
		lines.add(new Line("Profit: " + Format.coins(e.profit()), GOLD));
		lines.add(new Line("Profit Per Run: " + Format.coins(e.profitPerRun()), GOLD));
		lines.add(new Line("Runs: " + e.runs(), WHITE));
		lines.add(new Line("Timer: " + Format.duration(e.activeMillis()), WHITE));

		int lineHeight = this.font.lineHeight + 3;
		int visible = Math.max(1, (detailBottom - detailTop - 8) / lineHeight);
		int maxScroll = Math.max(0, lines.size() - visible);
		detailScroll = Math.max(0, Math.min(detailScroll, maxScroll));
		for (int i = 0; i < visible && detailScroll + i < lines.size(); i++) {
			Line line = lines.get(detailScroll + i);
			graphics.text(this.font, line.text(), detailLeft + 5, detailTop + 5 + i * lineHeight, line.color());
		}
		if (maxScroll > 0) {
			int trackHeight = detailBottom - detailTop - 2;
			int barHeight = Math.max(8, trackHeight * visible / lines.size());
			int barY = detailTop + 1 + (trackHeight - barHeight) * detailScroll / maxScroll;
			graphics.fill(detailRight - 3, barY, detailRight - 1, barY + barHeight, GRAY);
		}
	}

	private static String date(SessionHistory.Entry entry) {
		return DATE.format(Instant.ofEpochMilli(entry.endedAtMillis()).atZone(ZoneId.systemDefault()));
	}

	/** Draws a button and remembers it for clicks. */
	private void button(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height,
						String label, int color, Click click) {
		Zone zone = new Zone(x, y, width, height, click);
		graphics.fill(x, y, x + width, y + height, zone.contains(mouseX, mouseY) ? BUTTON_HOVER : BUTTON_BG);
		graphics.centeredText(this.font, label, x + width / 2, y + (height - this.font.lineHeight) / 2 + 1, color);
		zones.add(zone);
	}

	// ---- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int mouseX = (int) event.x();
			int mouseY = (int) event.y();
			// The last zone drawn is on top.
			for (int i = zones.size() - 1; i >= 0; i--) {
				Zone zone = zones.get(i);
				if (zone.contains(mouseX, mouseY)) {
					zone.click().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == Tab.HISTORY && scrollY != 0) {
			int step = -(int) Math.signum(scrollY) * 3;
			if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
				listScroll += step;
				return true;
			}
			if (mouseX >= detailLeft && mouseX < detailRight && mouseY >= detailTop && mouseY < detailBottom) {
				detailScroll += step;
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		saveSettings();
		super.onClose();
	}
}
