/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.LogoutSpotsHack;

@Mixin(EntityRenderDispatcher.class)
public class LogoutSpotsMixin
{
	@Inject(at = @At("TAIL"), method = "render")
	private void onRender(Entity entity, double x, double y, double z,
		float yaw, float tickDelta, MatrixStack matrices,
		VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci)
	{
		// Check if LogoutSpots is enabled
		LogoutSpotsHack logoutSpots =
			WurstClient.INSTANCE.getHax().logoutSpotsHack;
		if(!logoutSpots.isEnabled())
			return;
			
		// The actual rendering is handled in the LogoutSpotsHack class
		// This mixin is just a placeholder for future improvements
	}
}
