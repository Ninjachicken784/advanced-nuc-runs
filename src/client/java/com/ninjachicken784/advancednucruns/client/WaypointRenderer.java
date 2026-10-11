package com.ninjachicken784.advancednucruns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import com.ninjachicken784.advancednucruns.Waypoint;

/**
 * Draws every waypoint as a red box with a label. Both are marked "always on top", so they
 * show through blocks and terrain.
 */
final class WaypointRenderer {
	private static final int RED = ARGB.color(255, 255, 0, 0);
	private static final int RED_FILL = ARGB.color(70, 255, 0, 0);
	private static final float OUTLINE_WIDTH = 3.0f;
	private static final GizmoStyle BOX_STYLE = GizmoStyle.strokeAndFill(RED, OUTLINE_WIDTH, RED_FILL);

	private WaypointRenderer() { }

	static void register() {
		LevelRenderEvents.BEFORE_GIZMOS.register(WaypointRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		Vec3 eye = client.player.getEyePosition();

		for (Waypoint w : AdvancedNucRunsClient.WAYPOINTS.list()) {
			Gizmos.cuboid(new AABB(w.x(), w.y(), w.z(), w.x() + 1.0, w.y() + 1.0, w.z() + 1.0), BOX_STYLE)
				.setAlwaysOnTop();

			Vec3 center = new Vec3(w.x() + 0.5, w.y() + 0.5, w.z() + 0.5);
			long distance = Math.round(eye.distanceTo(center));
			String text = w.label() + " (" + distance + "m)";

			Gizmos.billboardText(text, new Vec3(w.x() + 0.5, w.y() + 1.6, w.z() + 0.5), TextGizmo.Style.whiteAndCentered())
				.setAlwaysOnTop();
		}
	}
}
