package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Lets the player drag the timer to a new spot. Closing the screen (Esc) saves the position.
 */
final class TimerMoveScreen extends Screen {
	private static final int DIM = 0x50000000;
	private static final int WHITE = 0xFFFFFFFF;

	private boolean dragging;
	private int grabOffsetX;
	private int grabOffsetY;
	private int lastMouseX;
	private int lastMouseY;

	TimerMoveScreen() {
		super(Component.literal("Move timer"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;

		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), DIM);

		if (dragging) {
			TimerHud.x = mouseX - grabOffsetX;
			TimerHud.y = mouseY - grabOffsetY;
		}

		TimerHud.draw(graphics, this.font, true);

		graphics.centeredText(this.font, "Drag the timer to move it. Press Esc to save.", graphics.guiWidth() / 2, 20, WHITE);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (TimerHud.contains(lastMouseX, lastMouseY, this.font)) {
			dragging = true;
			grabOffsetX = lastMouseX - TimerHud.x;
			grabOffsetY = lastMouseY - TimerHud.y;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		TimerHud.save();
		super.onClose();
	}
}
