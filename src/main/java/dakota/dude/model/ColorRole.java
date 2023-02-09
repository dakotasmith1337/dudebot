package dakota.dude.model;

import discord4j.rest.util.Color;

public class ColorRole {
	
	private String name;
	private Color color;

	public ColorRole(String name, Color color) {
		this.name = name;
		this.color = color;
	}

	/**
	 * Returns this role's name.
	 * @return this role's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set's this role's name.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns this role's color.
	 * @return this role's color
	 */
	public Color getColor() {
		return color;
	}
	
	/**
	 * Sets this role's color.
	 * @param color
	 */
	public void setColor(Color color) {
		this.color = color;
	}
}
