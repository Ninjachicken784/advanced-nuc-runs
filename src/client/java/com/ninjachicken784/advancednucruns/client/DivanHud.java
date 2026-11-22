package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.ninjachicken784.advancednucruns.DivanTracker;
import com.ninjachicken784.advancednucruns.Format;

/**
 * The Divan Tools box: how many of each Scavenged tool were found, the finished sets, tools and
 * sets per hour, the timer, and a Pause / Reset row that can be clicked while the chat is open.
 * Each line is drawn like the other GUI boxes: a yellow label and a white value.
 */
final class DivanHud {
	private static final int PAD = 1;
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	private static final int GREEN_DIM = 0xFF2A7A2A;
	private static final int RED_DIM = 0xFF7A2A2A;

	private record Line(String label, String value) { }

	private DivanHud() { }

	private static List<Line> lines(boolean preview) {
		DivanTracker tracker = AdvancedNucRunsClient.DIVAN;
		long now = AdvancedNucRunsClient.nowMillis();
		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Divan Tools", ""));
		lines.add(new Line("Chests:", String.valueOf(tracker.chests())));
		lines.add(new Line("Sets:", String.valueOf(tracker.sets())));
		lines.add(new Line("Chests/h:", rate(tracker.chestsPerHour(now))));
		lines.add(new Line("Sets/h:", rate(tracker.setsPerHour(now))));
		lines.add(new Line("Timer:", Format.duration(tracker.activeMillis(now))));
		return lines;
	}

	private static String rate(double value) {
		return Double.isNaN(value) ? "--" : String.format(Locale.ROOT, "%.1f", value);
	}

	/** The last row: the Pause / Resume / Start button and Reset. */
	private static String buttonText() {
		return AdvancedNucRunsClient.DIVAN.buttonText(AdvancedNucRunsClient.nowMillis());
	}

	static int width(Font font, HudBox box, boolean preview) {
		int width = HudText.width(font, buttonText(), "Reset", box.scale);
		for (Line line : lines(preview)) {
			width = Math.max(width, HudText.width(font, line.label(), line.value(), box.scale));
		}
		return width;
	}

	static int height(Font font, HudBox box, boolean preview) {
		return (lines(preview).size() + 1) * HudText.height(font, box.scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, HudBox box, boolean dim, boolean preview) {
		List<Line> lines = lines(preview);
		int width = width(font, box, preview);
		int lineHeight = HudText.height(font, box.scale);
		box.keepOnScreen(width, lineHeight * (lines.size() + 1), graphics.guiWidth(), graphics.guiHeight());
		int left = box.left(width);
		int y = box.y;
		for (Line line : lines) {
			HudText.draw(graphics, font, line.label(), line.value(), left, y, box.scale,
				dim ? HudText.LABEL_DIM : HudText.LABEL, dim ? HudText.VALUE_DIM : HudText.VALUE);
			y += lineHeight;
		}
		boolean running = AdvancedNucRunsClient.DIVAN.running(AdvancedNucRunsClient.nowMillis());
		int buttonColor = running ? (dim ? RED_DIM : RED) : (dim ? GREEN_DIM : GREEN);
		HudText.draw(graphics, font, buttonText(), "Reset", left, y, box.scale, buttonColor, dim ? RED_DIM : RED);
	}

	/** Clicks on Pause / Resume / Start and Reset. Returns true if the click hit one of them. */
	static boolean click(Font font, double mouseX, double mouseY) {
		HudBox box = GuiConfig.DIVAN;
		if (box.hidden || !HudVisibility.shown(box)) {
			return false;
		}
		List<Line> lines = lines(false);
		int width = width(font, box, false);
		int lineHeight = HudText.height(font, box.scale);
		double x = (mouseX - box.left(width)) / box.scale - PAD;
		double y = (mouseY - (box.y + lines.size() * lineHeight)) / box.scale;
		if (y < -1 || y > font.lineHeight + PAD * 2 + 1) {
			return false;
		}
		int rowWidth = (int) Math.ceil(width / box.scale);
		int resetStart = font.width(buttonText() + " ");
		if (x < -PAD || x > rowWidth) {
			return false;
		}
		boolean reset = x >= resetStart - 2;
		if (reset) {
			AdvancedNucRunsClient.DIVAN.reset();
			TrackerStore.save();
			AdvancedNucRunsClient.chatNotice("Tools Tracker reset.");
		} else {
			AdvancedNucRunsClient.DIVAN.toggle(AdvancedNucRunsClient.nowMillis());
			TrackerStore.save();
			AdvancedNucRunsClient.chatNotice("Tools Tracker timer " + (buttonText().equals("Pause") ? "running." : "paused."));
		}
		return true;
	}
}
