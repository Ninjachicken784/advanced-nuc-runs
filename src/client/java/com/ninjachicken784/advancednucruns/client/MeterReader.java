package com.ninjachicken784.advancednucruns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.ninjachicken784.advancednucruns.MeterMenuReader;

/**
 * Copies the RNG meter whenever the in-game "Crystal Nucleus RNG Meter" menu is open: the stored
 * XP, the selected drop and how much XP it needs. The icon of the selected drop is kept for the
 * meter GUI.
 */
final class MeterReader {
	/**
	 * The icon of the selected drop, copied from the menu. Null until the menu has been opened once.
	 * Not created here: item stacks cannot be made while the mod is starting, before the game's items are ready.
	 */
	private static volatile ItemStack icon;
	/** Shown until then. Made the first time it is drawn, when the game is ready. */
	private static ItemStack placeholder;

	private MeterReader() { }

	static ItemStack icon() {
		ItemStack copied = icon;
		if (copied != null) {
			return copied;
		}
		if (placeholder == null) {
			placeholder = new ItemStack(Items.PAPER);
		}
		return placeholder;
	}

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(MeterReader::tick);
	}

	private static void tick(Minecraft client) {
		if (!(client.screen instanceof AbstractContainerScreen<?> screen)
			|| !MeterMenuReader.isMeterTitle(screen.getTitle().getString())) {
			return;
		}

		List<Slot> all = screen.getMenu().slots;
		// The last 36 slots are the player's own inventory, which is not part of the menu.
		int menuSlots = all.size() > 36 ? all.size() - 36 : all.size();

		List<MeterMenuReader.Slot> slots = new ArrayList<>();
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < menuSlots; i++) {
			ItemStack stack = all.get(i).getItem();
			if (stack.isEmpty()) {
				continue;
			}
			List<String> lines = new ArrayList<>();
			ItemLore lore = stack.get(DataComponents.LORE);
			if (lore != null) {
				for (Component line : lore.lines()) {
					lines.add(line.getString());
				}
			}
			slots.add(new MeterMenuReader.Slot(stack.getHoverName().getString(), lines));
			stacks.add(stack);
		}

		MeterMenuReader.Result result = MeterMenuReader.read(slots);
		if (result == null) {
			return;
		}
		String before = fingerprint();
		AdvancedNucRunsClient.METER.update(result.storedXp(), result.selectedName(), result.required(),
			result.selectionKnown());
		if (result.slotIndex() >= 0 && result.slotIndex() < stacks.size()) {
			icon = stacks.get(result.slotIndex()).copy();
		}
		// The menu is read every tick while it is open, so only save when something changed.
		if (!before.equals(fingerprint())) {
			TrackerStore.save();
		}
	}

	private static String fingerprint() {
		return AdvancedNucRunsClient.METER.storedXp() + "|" + AdvancedNucRunsClient.METER.selectedName()
			+ "|" + AdvancedNucRunsClient.METER.required() + "|" + AdvancedNucRunsClient.METER.hasData();
	}
}
