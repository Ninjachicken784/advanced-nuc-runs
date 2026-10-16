package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.ninjachicken784.advancednucruns.RngMeter;

/** The RNG meter GUI: the selected drop's icon and its progress in magenta, like "320.3k/1m". */
final class MeterHud {
	private static final int PAD = 2;
	private static final int ICON = 16;
	private static final int GAP = 3;
	private static final int MAGENTA = 0xFFFF55FF;
	private static final int GRAY = 0xFF888888;
	private static final String SAMPLE = "320.3k/1m";

	private MeterHud() { }

	/** True if there is anything to show: the meter has been copied from the game. */
	static boolean visible(boolean preview) {
		return preview || AdvancedNucRunsClient.METER.hasData();
	}

	private static String text(boolean preview) {
		RngMeter meter = AdvancedNucRunsClient.METER;
		return meter.hasData() ? meter.progressText() : SAMPLE;
	}

	static int width(Font font, HudBox box, boolean preview) {
		return (int) Math.ceil((PAD * 2 + ICON + GAP + font.width(text(preview))) * box.scale);
	}

	static int height(Font font, HudBox box, boolean preview) {
		return (int) Math.ceil((PAD * 2 + ICON) * box.scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, HudBox box, boolean dim, boolean preview) {
		int width = width(font, box, preview);
		int height = height(font, box, preview);
		box.keepOnScreen(width, height, graphics.guiWidth(), graphics.guiHeight());

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) box.left(width), (float) box.y);
		graphics.pose().scale(box.scale);
		graphics.item(MeterReader.icon(), PAD, PAD);
		int textY = PAD + (ICON - font.lineHeight) / 2 + 1;
		graphics.text(font, text(preview), PAD + ICON + GAP, textY, dim ? GRAY : MAGENTA);
		graphics.pose().popMatrix();
	}
}
