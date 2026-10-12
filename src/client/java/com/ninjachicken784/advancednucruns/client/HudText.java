package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a GUI line like "PB 1min 22sec": a bold yellow label and a bold white value, each with a
 * thick black outline and a soft drop shadow, scaled to the size of its box.
 */
final class HudText {
	/** Minecraft's formatting code for bold text. */
	private static final String BOLD = "§l";

	/** Room around the text for the outline (1 pixel) and the shadow (2 pixels). */
	private static final int PAD = 2;

	private static final int OUTLINE = 0xFF000000;
	private static final int SHADOW = 0x80000000;

	static final int LABEL = 0xFFFFFF55;
	static final int VALUE = 0xFFFFFFFF;
	static final int LABEL_DIM = 0xFF7A7A44;
	static final int VALUE_DIM = 0xFF888888;

	private HudText() { }

	/** Width on screen in GUI pixels. */
	static int width(Font font, String label, String value, float scale) {
		return (int) Math.ceil((font.width(BOLD + label + " " + value) + PAD * 2) * scale);
	}

	/** Height on screen in GUI pixels. */
	static int height(Font font, float scale) {
		return (int) Math.ceil((font.lineHeight + PAD * 2) * scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, String label, String value,
					 int x, int y, float scale, int labelColor, int valueColor) {
		String boldLabel = BOLD + label;
		String boldValue = BOLD + value;
		int valueOffset = font.width(BOLD + label + " ");

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) x, (float) y);
		graphics.pose().scale(scale);

		// Drop shadow, down and to the right, under everything else.
		graphics.text(font, boldLabel, PAD + 2, PAD + 2, SHADOW);
		graphics.text(font, boldValue, PAD + valueOffset + 2, PAD + 2, SHADOW);

		// The outline is the same text in black, shifted one pixel in every direction.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx != 0 || dy != 0) {
					graphics.text(font, boldLabel, PAD + dx, PAD + dy, OUTLINE);
					graphics.text(font, boldValue, PAD + valueOffset + dx, PAD + dy, OUTLINE);
				}
			}
		}

		graphics.text(font, boldLabel, PAD, PAD, labelColor);
		graphics.text(font, boldValue, PAD + valueOffset, PAD, valueColor);

		graphics.pose().popMatrix();
	}
}
