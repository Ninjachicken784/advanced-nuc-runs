package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.ninjachicken784.advancednucruns.RankTracker;

/**
 * The leaderboard box: "#91 Ninjachicken784", your runs, the runs left to the player above you
 * and the runs left to your goal. Each line is drawn like the other GUI boxes, a yellow label and
 * a white value. Nothing is shown until a rank was entered with /anc Rank.
 */
final class RankHud {
	private record Line(String label, String value) { }

	private RankHud() { }

	/** The player's Minecraft name, or "You" if it is not known. */
	static String playerName() {
		try {
			return Minecraft.getInstance().getUser().getName();
		} catch (RuntimeException e) {
			return "You";
		}
	}

	/** False while there is nothing to show; the move screen always shows a sample. */
	static boolean visible(boolean preview) {
		return preview || AdvancedNucRunsClient.RANK.hasData();
	}

	private static List<Line> lines(boolean preview) {
		RankTracker rank = AdvancedNucRunsClient.RANK;
		List<Line> lines = new ArrayList<>();
		if (!rank.hasData()) {
			// A sample, so the box can be moved before anything was entered.
			lines.add(new Line("#91 " + playerName(), ""));
			lines.add(new Line("Runs:", "3046"));
			lines.add(new Line("Next #90:", "1 run left"));
			lines.add(new Line("Top 10:", "5754 runs left"));
			return lines;
		}
		long index = AdvancedNucRunsClient.LAST_DROPS.runIndex();
		lines.add(new Line(rank.headline(playerName()), ""));
		lines.add(new Line("Runs:", String.valueOf(rank.runs(index))));
		addSplit(lines, rank.nextLine(index));
		addSplit(lines, rank.goalLine(index));
		return lines;
	}

	/** Splits "Top 10: 5754 runs left" into the label "Top 10:" and the value "5754 runs left". */
	private static void addSplit(List<Line> lines, String text) {
		if (text == null) {
			return;
		}
		int split = text.indexOf(": ");
		lines.add(split < 0 ? new Line(text, "") : new Line(text.substring(0, split + 1), text.substring(split + 2)));
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
}
