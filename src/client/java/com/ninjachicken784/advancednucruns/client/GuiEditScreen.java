package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Lets the player move and hide the GUIs. Drag a box with the left mouse button, right-click it
 * to hide or show it. Closing the screen (Esc) saves everything.
 */
final class GuiEditScreen extends Screen {
	private static final int DIM = 0x50000000;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int RED = 0xFFFF5555;
	private static final int LEFT_BUTTON = 0;
	private static final int RIGHT_BUTTON = 1;

	private HudBox dragging;
	private int grabOffsetX;
	private int grabOffsetY;
	private int lastMouseX;
	private int lastMouseY;

	GuiEditScreen() {
		super(Component.literal("Move GUIs"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;

		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), DIM);

		if (dragging != null) {
			dragging.x = mouseX - grabOffsetX;
			dragging.y = mouseY - grabOffsetY;
		}

		for (HudBox box : GuiConfig.ALL) {
			RunHud.Content content = RunHud.content(box, true);
			RunHud.draw(graphics, this.font, box, content, box.hidden);
			frame(graphics, box, content, box.hidden ? RED : WHITE);
		}

		int centerX = graphics.guiWidth() / 2;
		graphics.centeredText(this.font, "Drag a box to move it. Right-click a box to hide or show it.", centerX, 20, WHITE);
		graphics.centeredText(this.font, "Hidden boxes are grey with a red frame. Press Esc to save.", centerX, 32, WHITE);
	}

	/** A thin frame around a box so it is easy to see and grab. */
	private void frame(GuiGraphicsExtractor graphics, HudBox box, RunHud.Content content, int color) {
		int width = RunHud.width(this.font, content);
		int height = RunHud.height(this.font);
		graphics.fill(box.x - 1, box.y - 1, box.x + width + 1, box.y, color);
		graphics.fill(box.x - 1, box.y + height, box.x + width + 1, box.y + height + 1, color);
		graphics.fill(box.x - 1, box.y, box.x, box.y + height, color);
		graphics.fill(box.x + width, box.y, box.x + width + 1, box.y + height, color);
	}

	/** The box under the mouse. Boxes drawn later are on top, so look from the end. */
	private HudBox boxAt(int mouseX, int mouseY) {
		for (int i = GuiConfig.ALL.size() - 1; i >= 0; i--) {
			HudBox box = GuiConfig.ALL.get(i);
			if (RunHud.contains(box, RunHud.content(box, true), this.font, mouseX, mouseY)) {
				return box;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		HudBox hit = boxAt(lastMouseX, lastMouseY);
		if (hit != null) {
			if (event.button() == RIGHT_BUTTON) {
				hit.hidden = !hit.hidden;
				return true;
			}
			if (event.button() == LEFT_BUTTON) {
				dragging = hit;
				grabOffsetX = lastMouseX - hit.x;
				grabOffsetY = lastMouseY - hit.y;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		GuiConfig.save();
		super.onClose();
	}
}
