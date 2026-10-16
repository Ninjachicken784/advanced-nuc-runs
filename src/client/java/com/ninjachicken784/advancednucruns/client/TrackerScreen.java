package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import com.ninjachicken784.advancednucruns.Format;
import com.ninjachicken784.advancednucruns.NucleusItems;
import com.ninjachicken784.advancednucruns.PriceBook;
import com.ninjachicken784.advancednucruns.ProfitTracker;

/**
 * The tracker screen (/anc Tracker): a scrollable list of every drop and the run count, where the
 * numbers can be changed, and the settings (pity, price mode, number of rows, total or session,
 * pause, refresh prices, reset). It is drawn by hand: every frame lists the clickable areas, and a
 * click looks them up.
 */
final class TrackerScreen extends Screen {
	private static final int ROW = 14;
	private static final int BUTTON = 16;
	private static final int SETTINGS_WIDTH = 124;
	private static final int TOP = 30;
	private static final long CONFIRM_MILLIS = 3000L;
	private static final String RUNS_KEY = "__runs__";

	private static final int DIM = 0xB0000000;
	private static final int PANEL = 0xFF1B1B1B;
	private static final int EDGE = 0xFF555555;
	private static final int BUTTON_BG = 0xFF3A3A3A;
	private static final int BUTTON_HOVER = 0xFF5A5A5A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF999999;
	private static final int GOLD = 0xFFFFAA00;
	private static final int RED = 0xFFFF5555;
	private static final int GREEN = 0xFF55FF55;
	private static final int AQUA = 0xFF55FFFF;

	private interface Click {
		void run(int button, int modifiers);
	}

	/** One clickable area of the last frame. editKey is set when it is a number that can be typed. */
	private record Zone(int x, int y, int width, int height, String editKey, Click click) {
		boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private record Entry(String key, String name, int color) { }

	private final List<Zone> zones = new ArrayList<>();
	private int scroll;
	private int listTop;
	private int listBottom;
	private int listLeft;
	private int listRight;

	private String editingKey;
	private final StringBuilder editBuffer = new StringBuilder();
	private boolean editFresh;

	private String confirming;
	private long confirmUntil;

	TrackerScreen() {
		super(Component.literal("Crystal Nucleus Profit Tracker"));
	}

	private static ProfitTracker tracker() {
		return AdvancedNucRunsClient.TRACKER;
	}

	private List<Entry> entries() {
		List<Entry> list = new ArrayList<>();
		list.add(new Entry(RUNS_KEY, "Runs", WHITE));
		for (NucleusItems.Item item : NucleusItems.ALL) {
			list.add(new Entry(item.key(), item.name(), TrackerHud.styleColor(item.style())));
		}
		for (Map.Entry<String, String> custom : tracker().customDrops().entrySet()) {
			list.add(new Entry(custom.getKey(), custom.getValue(), TrackerHud.styleColor(NucleusItems.Style.BLUE)));
		}
		return list;
	}

	private long value(String key) {
		boolean session = tracker().sessionView();
		return key.equals(RUNS_KEY) ? tracker().runs(session) : tracker().count(key, session);
	}

	private void setValue(String key, long value) {
		long clamped = Math.max(0L, Math.min(value, 1_000_000_000_000L));
		boolean session = tracker().sessionView();
		if (key.equals(RUNS_KEY)) {
			tracker().setRuns(clamped, session);
		} else {
			tracker().setCount(key, clamped, session);
		}
		TrackerStore.save();
	}

	private static int step(int modifiers) {
		if ((modifiers & 2) != 0) {
			return 100;
		}
		return (modifiers & 1) != 0 ? 10 : 1;
	}

	// ---- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		zones.clear();
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		graphics.fill(0, 0, guiWidth, guiHeight, DIM);

		int width = Math.min(guiWidth - 16, 470);
		int left = (guiWidth - width) / 2;
		listLeft = left;
		listRight = left + width - SETTINGS_WIDTH - 8;
		listTop = TOP;
		listBottom = guiHeight - 22;
		int settingsLeft = left + width - SETTINGS_WIDTH;

		graphics.centeredText(this.font, "Crystal Nucleus Profit Tracker", guiWidth / 2, 10, GOLD);

		drawList(graphics, mouseX, mouseY);
		drawSettings(graphics, mouseX, mouseY, settingsLeft);

		String view = tracker().sessionView() ? "Session" : "Total";
		graphics.centeredText(this.font, "Editing the " + view + " numbers. Click a number to type it. "
			+ "+ and - change it by 1 (Shift 10, Ctrl 100). Esc closes.", guiWidth / 2, guiHeight - 14, GRAY);
	}

	private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.fill(listLeft, listTop, listRight, listBottom, EDGE);
		graphics.fill(listLeft + 1, listTop + 1, listRight - 1, listBottom - 1, PANEL);

		List<Entry> entries = entries();
		int visible = Math.max(1, (listBottom - listTop - 2) / ROW);
		int maxScroll = Math.max(0, entries.size() - visible);
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		PriceBook prices = AdvancedNucRunsClient.PRICES;
		for (int i = 0; i < visible && scroll + i < entries.size(); i++) {
			Entry entry = entries.get(scroll + i);
			int y = listTop + 1 + i * ROW;
			int textY = y + (ROW - this.font.lineHeight) / 2 + 1;

			graphics.text(this.font, entry.name(), listLeft + 5, textY, entry.color());

			if (!entry.key().equals(RUNS_KEY)) {
				double price = tracker().unitPrice(entry.key(), prices);
				String priceText = Double.isNaN(price) ? "?" : Format.coins(price);
				graphics.text(this.font, priceText, listRight - 92 - this.font.width(priceText), textY,
					Double.isNaN(price) ? GRAY : GOLD);
			}

			drawStepper(graphics, mouseX, mouseY, entry, y);
		}

		if (maxScroll > 0) {
			int trackHeight = listBottom - listTop - 2;
			int barHeight = Math.max(8, trackHeight * visible / entries.size());
			int barY = listTop + 1 + (trackHeight - barHeight) * scroll / maxScroll;
			graphics.fill(listRight - 3, barY, listRight - 1, barY + barHeight, GRAY);
		}
	}

	/** The "- 123 +" controls of one row. */
	private void drawStepper(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Entry entry, int y) {
		String key = entry.key();
		int minusX = listRight - 86;
		int boxX = listRight - 72;
		int plusX = listRight - 14;
		int size = ROW - 2;

		button(graphics, mouseX, mouseY, minusX, y + 1, 12, size, "-", WHITE, null,
			(button, modifiers) -> setValue(key, value(key) - step(modifiers)));
		button(graphics, mouseX, mouseY, plusX, y + 1, 12, size, "+", WHITE, null,
			(button, modifiers) -> setValue(key, value(key) + step(modifiers)));

		boolean editing = key.equals(editingKey);
		String text = editing ? editBuffer + "_" : String.valueOf(value(key));
		graphics.fill(boxX, y + 1, boxX + 56, y + 1 + size, editing ? AQUA : EDGE);
		graphics.fill(boxX + 1, y + 2, boxX + 55, y + size, 0xFF000000);
		graphics.text(this.font, text, boxX + 4, y + (ROW - this.font.lineHeight) / 2 + 1, editing ? AQUA : WHITE);
		zones.add(new Zone(boxX, y + 1, 56, size, key, (button, modifiers) -> startEditing(key)));
	}

	private void drawSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x) {
		ProfitTracker t = tracker();
		long now = AdvancedNucRunsClient.nowMillis();
		int y = TOP;
		int w = SETTINGS_WIDTH;

		button(graphics, mouseX, mouseY, x, y, w, BUTTON, "Pity: " + (t.pity() ? "ON" : "OFF"),
			t.pity() ? GREEN : RED, null, (button, modifiers) -> {
				t.setPity(!t.pity());
				TrackerStore.save();
			});
		y += BUTTON + 4;

		boolean sellOffer = t.priceMode() == PriceBook.Mode.SELL_OFFER;
		button(graphics, mouseX, mouseY, x, y, w, BUTTON, sellOffer ? "Price: Sell offer" : "Price: Instant sell",
			WHITE, null, (button, modifiers) -> {
				t.setPriceMode(sellOffer ? PriceBook.Mode.INSTANT_SELL : PriceBook.Mode.SELL_OFFER);
				TrackerStore.save();
			});
		y += BUTTON + 4;

		graphics.text(this.font, "Rows on screen: " + t.rows(), x, y + 4, WHITE);
		button(graphics, mouseX, mouseY, x + w - 40, y, 18, BUTTON, "-", WHITE, null, (button, modifiers) -> {
			t.setRows(t.rows() - 1);
			TrackerStore.save();
		});
		button(graphics, mouseX, mouseY, x + w - 18, y, 18, BUTTON, "+", WHITE, null, (button, modifiers) -> {
			t.setRows(t.rows() + 1);
			TrackerStore.save();
		});
		y += BUTTON + 4;

		button(graphics, mouseX, mouseY, x, y, w, BUTTON, "View: " + (t.sessionView() ? "Session" : "Total"),
			AQUA, null, (button, modifiers) -> {
				t.setSessionView(!t.sessionView());
				TrackerStore.save();
			});
		y += BUTTON + 4;

		boolean paused = t.isPaused(now);
		button(graphics, mouseX, mouseY, x, y, w, BUTTON, paused ? "Timer: Resume" : "Timer: Pause",
			paused ? GREEN : RED, null, (button, modifiers) -> {
				t.togglePause(AdvancedNucRunsClient.nowMillis());
				TrackerStore.save();
			});
		y += BUTTON + 4;

		button(graphics, mouseX, mouseY, x, y, w, BUTTON, "Refresh prices", WHITE, null,
			(button, modifiers) -> PriceService.refresh());
		y += BUTTON + 10;

		boolean sessionConfirm = "session".equals(confirming) && now < confirmUntil;
		button(graphics, mouseX, mouseY, x, y, w, BUTTON, sessionConfirm ? "Click again to confirm" : "Reset session",
			RED, null, (button, modifiers) -> {
				if (sessionConfirm) {
					t.resetSession();
					TrackerStore.save();
					confirming = null;
				} else {
					askConfirm("session");
				}
			});
		y += BUTTON + 4;

		boolean allConfirm = "all".equals(confirming) && now < confirmUntil;
		button(graphics, mouseX, mouseY, x, y, w, BUTTON, allConfirm ? "Click again to confirm" : "Reset everything",
			RED, null, (button, modifiers) -> {
				if (allConfirm) {
					t.reset();
					TrackerStore.save();
					confirming = null;
				} else {
					askConfirm("all");
				}
			});
		y += BUTTON + 10;

		PriceBook prices = AdvancedNucRunsClient.PRICES;
		graphics.text(this.font, PriceService.status(), x, y, GRAY);
		y += this.font.lineHeight + 3;
		String cost = ProfitTracker.costKnown(prices) ? Format.coins(ProfitTracker.costPerRun(prices)) : "?";
		graphics.text(this.font, "Cost per run: " + cost, x, y, RED);
	}

	private void askConfirm(String what) {
		confirming = what;
		confirmUntil = AdvancedNucRunsClient.nowMillis() + CONFIRM_MILLIS;
	}

	/** Draws a button and remembers it for clicks. A null editKey means it is not a typed number. */
	private void button(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height,
						String label, int color, String editKey, Click click) {
		Zone zone = new Zone(x, y, width, height, editKey, click);
		graphics.fill(x, y, x + width, y + height, zone.contains(mouseX, mouseY) ? BUTTON_HOVER : BUTTON_BG);
		graphics.centeredText(this.font, label, x + width / 2, y + (height - this.font.lineHeight) / 2 + 1, color);
		zones.add(zone);
	}

	// ---- editing numbers

	private void startEditing(String key) {
		editingKey = key;
		editBuffer.setLength(0);
		editBuffer.append(value(key));
		editFresh = true;
	}

	private void commit() {
		if (editingKey != null && editBuffer.length() > 0) {
			try {
				setValue(editingKey, Long.parseLong(editBuffer.toString()));
			} catch (NumberFormatException ignored) {
				// Too many digits: leave the number as it was.
			}
		}
		editingKey = null;
		editBuffer.setLength(0);
	}

	// ---- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mouseX = (int) event.x();
		int mouseY = (int) event.y();
		Zone hit = null;
		for (Zone zone : zones) {
			if (zone.contains(mouseX, mouseY)) {
				hit = zone;
				break;
			}
		}
		// Clicking anywhere except the number being typed finishes the typing.
		if (hit == null || hit.editKey() == null || !hit.editKey().equals(editingKey)) {
			commit();
		}
		if (hit != null) {
			hit.click().run(event.button(), event.modifiers());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
			scroll -= (int) Math.signum(scrollY) * 3;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editingKey != null) {
			int key = event.key();
			int digit = key >= 48 && key <= 57 ? key - 48 : (key >= 320 && key <= 329 ? key - 320 : -1);
			if (digit >= 0) {
				if (editFresh) {
					editBuffer.setLength(0);
					editFresh = false;
				}
				if (editBuffer.length() < 12) {
					editBuffer.append(digit);
				}
			} else if (key == 259) {
				// Backspace.
				if (editFresh) {
					editBuffer.setLength(0);
					editFresh = false;
				} else if (editBuffer.length() > 0) {
					editBuffer.setLength(editBuffer.length() - 1);
				}
			} else if (key == 257 || key == 335) {
				commit();
			} else if (key == 256) {
				// Escape stops typing without changing the number.
				editingKey = null;
				editBuffer.setLength(0);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		commit();
		TrackerStore.save();
		super.onClose();
	}
}
