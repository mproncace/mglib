package net.amigocraft.mglib.api;

import org.bukkit.ChatColor;

/**
 * The Team Object
 *
 * @author codekrafter
 * @since 0.5.1
 */

public class Team {

	private String name = null;
	private String prefix = null;
	private ChatColor color = null;
	public static final Team DEFAULT = new Team("default", ChatColor.GRAY);
	
	public Team(String name, ChatColor color) {
		this.setName(name);
		this.setColor(color);
		this.setPrefix(this.getColor() + "[" + name + "] ");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public ChatColor getColor() {
		return color;
	}

	public void setColor(ChatColor color) {
		this.color = color;
	}

}
