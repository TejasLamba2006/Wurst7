/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

@SearchTags({"logout spots", "LogoutESP", "logout esp", "LogoutTracker",
	"logout tracker"})
public final class LogoutSpotsHack extends Hack
	implements UpdateListener, RenderListener
{
	private final EnumSetting<BoxStyle> boxStyle = new EnumSetting<>(
		"Box style", "How to render the box at the logout position.",
		BoxStyle.values(), BoxStyle.OUTLINE);
	
	private final CheckboxSetting showName = new CheckboxSetting("Show name",
		"Shows the player's name above the logout spot.", true);
	
	private final CheckboxSetting showHealth =
		new CheckboxSetting("Show health",
			"Shows the player's last known health next to their name.", true);
	
	private final CheckboxSetting showDistance = new CheckboxSetting(
		"Show distance", "Shows your distance to the logout spot.", true);
	
	private final SliderSetting scale = new SliderSetting("Scale",
		"The scale of the text displayed above the logout spot.", 1, 0.5, 3,
		0.1, ValueDisplay.DECIMAL);
	
	private final ColorSetting boxColor = new ColorSetting("Box color",
		"The color of the box at the logout position.", new Color(255, 0, 0));
	
	private final ColorSetting textColor = new ColorSetting("Text color",
		"The color of the text displayed above the logout spot.",
		new Color(255, 255, 255));
	
	private final List<LogoutSpot> spots = new ArrayList<>();
	private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
	private final List<PlayerEntity> lastPlayers = new ArrayList<>();
	
	public LogoutSpotsHack()
	{
		super("LogoutSpots");
		setCategory(Category.RENDER);
		
		addSetting(boxStyle);
		addSetting(showName);
		addSetting(showHealth);
		addSetting(showDistance);
		addSetting(scale);
		addSetting(boxColor);
		addSetting(textColor);
	}
	
	@Override
	protected void onEnable()
	{
		spots.clear();
		lastPlayerList.clear();
		
		if(MC.getNetworkHandler() != null)
			lastPlayerList.addAll(MC.getNetworkHandler().getPlayerList());
		
		updateLastPlayers();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		spots.clear();
		lastPlayerList.clear();
		lastPlayers.clear();
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + spots.size() + "]";
	}
	
	private void updateLastPlayers()
	{
		lastPlayers.clear();
		
		if(MC.world == null)
			return;
		
		for(PlayerEntity player : MC.world.getPlayers())
			if(player != null && !player.isRemoved())
				lastPlayers.add(player);
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.getNetworkHandler() == null)
			return;
		
		// Check for players that have disconnected
		if(MC.getNetworkHandler().getPlayerList().size() != lastPlayerList
			.size())
		{
			for(PlayerListEntry entry : lastPlayerList)
			{
				if(MC.getNetworkHandler().getPlayerList().stream()
					.anyMatch(e -> e.getProfile().getId()
						.equals(entry.getProfile().getId())))
					continue;
				
				// Player has disconnected, find their entity
				for(PlayerEntity player : lastPlayers)
				{
					if(player.getUuid().equals(entry.getProfile().getId()))
					{
						// Add logout spot
						addLogoutSpot(player);
						break;
					}
				}
			}
			
			// Update the player list
			lastPlayerList.clear();
			lastPlayerList.addAll(MC.getNetworkHandler().getPlayerList());
			updateLastPlayers();
		}
	}
	
	private void addLogoutSpot(PlayerEntity player)
	{
		// Don't track the local player
		if(player == MC.player)
			return;
		
		// Remove any existing spot for this player
		spots.removeIf(spot -> spot.uuid.equals(player.getUuid()));
		
		// Add new spot
		spots.add(new LogoutSpot(player));
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// Don't render if no spots
		if(spots.isEmpty())
			return;
		
		// Render all spots
		for(LogoutSpot spot : spots)
			spot.render(matrixStack);
	}
	
	private class LogoutSpot
	{
		private final UUID uuid;
		private final String name;
		private final Vec3d pos;
		private final Box box;
		private final float health;
		private final float maxHealth;
		
		public LogoutSpot(PlayerEntity player)
		{
			this.uuid = player.getUuid();
			this.name = player.getName().getString();
			this.pos = player.getPos();
			this.box = player.getBoundingBox();
			this.health = player.getHealth();
			this.maxHealth = player.getMaxHealth();
		}
		
		public void render(MatrixStack matrixStack)
		{
			// Don't render if too far away
			if(MC.player.squaredDistanceTo(pos) > 16384) // 128 blocks squared
				return;
			
			// Render box
			renderBox(matrixStack);
			
			// Render nametag
			if(showName.isChecked() || showHealth.isChecked()
				|| showDistance.isChecked())
				renderNameTag(matrixStack);
		}
		
		private void renderBox(MatrixStack matrixStack)
		{
			switch(boxStyle.getSelected())
			{
				case OUTLINE:
				RenderUtils.drawOutlinedBox(matrixStack, box,
					boxColor.getColorI(), false);
				break;
				
				case FILLED:
				RenderUtils.drawSolidBox(matrixStack, box,
					boxColor.getColorI(0.25F), false);
				break;
				
				case BOTH:
				RenderUtils.drawOutlinedBox(matrixStack, box,
					boxColor.getColorI(), false);
				RenderUtils.drawSolidBox(matrixStack, box,
					boxColor.getColorI(0.25F), false);
				break;
			}
		}
		
		private void renderNameTag(MatrixStack matrixStack)
		{
			// Prepare text
			StringBuilder sb = new StringBuilder();
			
			if(showName.isChecked())
				sb.append(name);
			
			if(showHealth.isChecked())
			{
				if(sb.length() > 0)
					sb.append(" ");
				
				sb.append("§");
				
				// Health color
				float healthPercentage = health / maxHealth;
				if(healthPercentage <= 0.33)
					sb.append("c"); // Red
				else if(healthPercentage <= 0.66)
					sb.append("e"); // Yellow
				else
					sb.append("a"); // Green
					
				sb.append(Math.round(health));
			}
			
			if(showDistance.isChecked())
			{
				if(sb.length() > 0)
					sb.append(" ");
				
				sb.append("§7");
				sb.append(
					Math.round(Math.sqrt(MC.player.squaredDistanceTo(pos))));
				sb.append("m");
			}
			
			String text = sb.toString();
			if(text.isEmpty())
				return;
			
			// Get position for the nametag
			double height = box.maxY - box.minY;
			Vec3d tagPos = pos.add(0, height + 0.2, 0);
			
			// We need to use the EntityRenderer's method to render the text
			// properly
			// For now, we'll just render a line to indicate the position
			RenderUtils.drawLine(matrixStack, tagPos, tagPos.add(0, 0.3, 0),
				textColor.getColorI(), false);
			
			// We'll need to create a mixin to properly render the text
			// For now, just log the position to chat when close
			if(MC.player.squaredDistanceTo(pos) < 100
				&& MC.player.age % 100 == 0)
				ChatUtils.message("Logout spot: " + name + " at " + pos.getX()
					+ ", " + pos.getY() + ", " + pos.getZ());
		}
	}
	
	private enum BoxStyle
	{
		OUTLINE("Outline"),
		FILLED("Filled"),
		BOTH("Both");
		
		private final String name;
		
		private BoxStyle(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
