package com.ninjachicken784.advancednucruns.client;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.ninjachicken784.advancednucruns.ChestFinder;
import com.ninjachicken784.advancednucruns.DivanTracker;

/**
 * The Metal Detector chest finder of the Tools Tracker. It reads the "TREASURE: 7.3m" distance the
 * detector shows, asks {@link ChestFinder} where the chest is (after every reading, so it is fast),
 * and draws a box, a label and a line to it. Opening a chest starts the Tools Tracker timer and
 * clears the finder for the next chest.
 */
final class ChestFinderClient {
	private static final ChestFinder FINDER = new ChestFinder();
	private static final Pattern TREASURE = Pattern.compile("TREASURE:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*m");
	private static final int CENTER_CHECK_TICKS = 20;
	private static final int GOLD = ARGB.color(255, 255, 200, 0);
	private static final int GOLD_FILL = ARGB.color(80, 255, 200, 0);
	private static final int YELLOW = ARGB.color(255, 255, 255, 85);
	private static final GizmoStyle BEST_STYLE = GizmoStyle.strokeAndFill(GOLD, 3.0f, GOLD_FILL);
	private static final GizmoStyle MAYBE_STYLE = GizmoStyle.stroke(YELLOW, 2.0f);
	private static final int MAX_MAYBE = 6;

	private static volatile ChestFinder.Result result = ChestFinder.Result.NONE;
	private static boolean keeperHintShown;
	private static int ticks;
	private static int inventoryTicks;
	private static long lastOpenMillis;

	private ChestFinderClient() { }

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticks >= CENTER_CHECK_TICKS) {
				ticks = 0;
				if (HudVisibility.toolsOn() && !FINDER.hasCenter()) {
					findCenter(client);
				}
			}
			if (++inventoryTicks >= 10) {
				inventoryTicks = 0;
				if (HudVisibility.toolsOn()) {
					checkInventory(client);
				}
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset(true));
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && HudVisibility.toolsOn() && HudVisibility.inCrystalHollows()
				&& level.getBlockState(hit.getBlockPos()).is(Blocks.CHEST)) {
				chestOpened(hit.getBlockPos());
			}
			return InteractionResult.PASS;
		});
	}

	/** A set is counted the moment all four Scavenged tools are in the inventory. */
	private static void checkInventory(Minecraft client) {
		if (client.player == null) {
			return;
		}
		DivanTracker.Tool[] tools = DivanTracker.Tool.values();
		boolean[] has = new boolean[tools.length];
		var inventory = client.player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			var stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			String name = stack.getHoverName().getString();
			for (int i = 0; i < tools.length; i++) {
				if (name.contains(tools[i].itemName())) {
					has[i] = true;
				}
			}
		}
		if (AdvancedNucRunsClient.DIVAN.inventory(has, AdvancedNucRunsClient.nowMillis())) {
			TrackerStore.save();
			AdvancedNucRunsClient.chatNotice("Set complete! " + AdvancedNucRunsClient.DIVAN.sets() + " sets");
		}
	}

	private static void reset(boolean everything) {
		if (everything) {
			FINDER.resetAll();
			keeperHintShown = false;
		} else {
			FINDER.clearReadings();
		}
		result = ChestFinder.Result.NONE;
	}

	private static void chestOpened(BlockPos pos) {
		long now = AdvancedNucRunsClient.nowMillis();
		if (now - lastOpenMillis < 1500L) {
			return;
		}
		lastOpenMillis = now;
		AdvancedNucRunsClient.DIVAN.chestOpened(now);
		TrackerStore.save();
		reset(false);
	}

	/** The center of the Mines is a fixed offset from the Keeper of Diamond, Lapis, Emerald or Gold. */
	private static void findCenter(Minecraft client) {
		if (client.level == null) {
			return;
		}
		for (Entity entity : client.level.entitiesForRendering()) {
			Component name = entity.getCustomName();
			if (name == null) {
				continue;
			}
			String text = name.getString().replaceAll("§.", "");
			int at = text.indexOf("Keeper of ");
			if (at < 0) {
				continue;
			}
			int[] offset = switch (text.substring(at + "Keeper of ".length()).strip().toLowerCase(Locale.ROOT)) {
				case "diamond" -> new int[] {33, 0, 3};
				case "lapis" -> new int[] {-33, 0, -3};
				case "emerald" -> new int[] {-3, 0, 33};
				case "gold" -> new int[] {3, 0, -33};
				default -> null;
			};
			if (offset != null) {
				FINDER.setCenter((int) Math.floor(entity.getX()) + offset[0], (int) Math.floor(entity.getY()) + offset[1],
					(int) Math.floor(entity.getZ()) + offset[2]);
				AdvancedNucRunsClient.chatNotice("Chest finder ready.");
				return;
			}
		}
	}

	/** Reads a line of chat or of the action bar. Returns true if it was a detector distance. */
	static boolean onMessage(String message) {
		if (!HudVisibility.toolsOn() || !HudVisibility.inCrystalHollows()) {
			return false;
		}
		Matcher matcher = TREASURE.matcher(message.replaceAll("§.", ""));
		if (!matcher.find()) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return true;
		}
		if (!FINDER.hasCenter()) {
			findCenter(client);
		}
		if (!FINDER.hasCenter()) {
			if (!keeperHintShown) {
				keeperHintShown = true;
				AdvancedNucRunsClient.chatNotice("Chest finder: walk up to a Keeper in the Mines of Divan once, then it points at chests.");
			}
			return true;
		}
		double distance = Double.parseDouble(matcher.group(1));
		double eyeShift = client.player.getEyeHeight() - 1.62d;
		FINDER.addReading(client.player.getX(), client.player.getY() + eyeShift, client.player.getZ(), distance);
		result = FINDER.solve();
		return true;
	}

	static void render() {
		Minecraft client = Minecraft.getInstance();
		ChestFinder.Result current = result;
		if (client.player == null || current.best() == null || !HudVisibility.toolsOn()) {
			return;
		}
		Vec3 eye = client.player.getEyePosition();
		List<ChestFinder.Spot> spots = current.spots();
		for (int i = 0; i < Math.min(spots.size(), MAX_MAYBE); i++) {
			ChestFinder.Spot spot = spots.get(i);
			AABB box = new AABB(spot.x(), spot.y(), spot.z(), spot.x() + 1.0, spot.y() + 1.0, spot.z() + 1.0);
			boolean best = i == 0;
			Gizmos.cuboid(box, best ? BEST_STYLE : MAYBE_STYLE).setAlwaysOnTop();
			Vec3 center = new Vec3(spot.x() + 0.5, spot.y() + 0.5, spot.z() + 0.5);
			String label = (best ? (current.certain() ? "Treasure chest" : "Chest?") : "maybe") + " ("
				+ Math.round(eye.distanceTo(center)) + "m)";
			Gizmos.billboardText(label, new Vec3(spot.x() + 0.5, spot.y() + 1.6, spot.z() + 0.5),
				TextGizmo.Style.whiteAndCentered()).setAlwaysOnTop();
		}
		ChestFinder.Spot best = current.best();
		Vec3 from = new Vec3(eye.x, eye.y - 0.6, eye.z);
		Gizmos.line(from, new Vec3(best.x() + 0.5, best.y() + 0.5, best.z() + 0.5), GOLD, 2.0f).setAlwaysOnTop();
	}
}
