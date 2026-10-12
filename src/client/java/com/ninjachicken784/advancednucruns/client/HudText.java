package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a GUI line like "PB 1min 22sec": a yellow label, a white value, both with a thick black
 * outline, scaled to the size of its box.
 */
final class HudText {
	private static final int PAD = 1;
	private static final int OUTLINE = 0xFF000000;

	static final int LABEL = 0xFFFFFF55;
	static final int VALUE = 0xFFFFFFFF;
	static final int LABEL_DIM = 0xFF7A7A44;
	static final int VALUE_DIM = 0xFF888888;

	private HudText() { }

	/** Width on screen in GUI pixels. */
	static int width(Font font, String label, String value, float scale) {
		return (int) Math.ceil((font.width(label + " " + value) + PAD * 2) * scale);
	}

	/** Height on screen in GUI pixels. */
	static int height(Font font, float scale) {
		return (int) Math.ceil((font.lineHeight + PAD * 2) * scale);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, String label, String value,
					 int x, int y, float scale, int labelColor, int valueColor) {
		int valueOffset = font.width(label + " ");

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) x, (float) y);
		graphics.pose().scale(scale);

		// The outline is the same text in black, shifted one pixel in every direction.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx != 0 || dy != 0) {
					graphics.text(font, label, PAD + dx, PAD + dy, OUTLINE);
					graphics.text(font, value, PAD + valueOffset + dx, PAD + dy, OUTLINE);
				}
			}
		}
		graphics.text(font, label, PAD, PAD, labelColor);
		graphics.text(font, value, PAD + valueOffset, PAD, valueColor);

		graphics.pose().popMatrix();
	}
}
