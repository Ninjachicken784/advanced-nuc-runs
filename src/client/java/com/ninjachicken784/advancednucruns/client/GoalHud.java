package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.ninjachicken784.advancednucruns.GoalTracker;

/**
 * The goals box: for every goal "Goal 10k", the runs left and the time that takes at your average
 * run time. Nothing is shown until a goal was set with /anc Goal.
 */
final class GoalHud {
	private record Line(String label, String value) { }

	private GoalHud() { }

	/** False while there is nothing to show; the move screen always shows a sample. */
	static boolean visible(boolean preview) {
		return preview || AdvancedNucRunsClient.GOALS.hasData();
	}

	private static List<Line> lines(boolean preview) {
		GoalTracker goals = AdvancedNucRunsClient.GOALS;
		List<Line> lines = new ArrayList<>();
		if (!goals.hasData()) {
			lines.add(new Line("Goal 10k", ""));
			lines.add(new Line("Left:", "6700 runs"));
			lines.add(new Line("Time:", "223h 30min"));
			return lines;
		}
		long index = AdvancedNucRunsClient.LAST_DROPS.runIndex();
		long average = AdvancedNucRunsClient.RUN_STATS.averageNanos();
		for (long goal : goals.goals()) {
			long left = goals.remaining(goal, index);
			lines.add(new Line("Goal " + GoalTracker.shortNumber(goal), left == 0L ? "reached" : ""));
			if (left > 0L) {
				lines.add(new Line("Left:", left + (left == 1L ? " run" : " runs")));
				lines.add(new Line("Time:", GoalTracker.timeLeft(left, average)));
			}
		}
		return lines;
	}

	static int width(Font font, HudBox box, boolean preview) {
		int width = 0;
		for (Line line : lines(preview)) {
			width = Math.max(width, HudText.width(font, line.label(), line.value(), box.scale));
		}
		return width;
	}

	static int height(Font font, HudBox box, boolean preview) {
		return lines(preview).size() * HudText.height(font, box.scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, HudBox box, boolean dim, boolean preview) {
		List<Line> lines = lines(preview);
		int width = width(font, box, preview);
		int lineHeight = HudText.height(font, box.scale);
		box.keepOnScreen(width, lineHeight * lines.size(), graphics.guiWidth(), graphics.guiHeight());
		int y = box.y;
		for (Line line : lines) {
			HudText.draw(graphics, font, line.label(), line.value(), box.left(width), y, box.scale,
				dim ? HudText.LABEL_DIM : HudText.LABEL, dim ? HudText.VALUE_DIM : HudText.VALUE);
			y += lineHeight;
		}
	}

	/** "10k: 6700 runs left, 223h 30min", one chat line for all goals. */
	static String status() {
		GoalTracker goals = AdvancedNucRunsClient.GOALS;
		if (!goals.hasData()) {
			return "No goal yet. Use /anc Goal <goal> <your runs>, for example /anc Goal 10000 3300.";
		}
		long index = AdvancedNucRunsClient.LAST_DROPS.runIndex();
		long average = AdvancedNucRunsClient.RUN_STATS.averageNanos();
		StringBuilder text = new StringBuilder("Runs: " + goals.runs(index));
		for (long goal : goals.goals()) {
			long left = goals.remaining(goal, index);
			text.append(". ").append(GoalTracker.shortNumber(goal)).append(": ")
				.append(left == 0L ? "reached" : left + " runs left, " + GoalTracker.timeLeft(left, average));
		}
		return text.toString();
	}
}
