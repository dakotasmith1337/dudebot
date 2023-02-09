package dakota.dude.handler.interaction.model;

import java.util.Map;

/**
 * The class that represents all non-command objects (type != 1)
 *
 */
public class SlashCommandObject implements Comparable<SlashCommandObject> {
	
	private final String name;
	private final String description;
	private final String longDescription;
	private final CommandType type;
	private final Map<String, SlashCommandObject> options;
	private final boolean global;
	
	public SlashCommandObject(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		this.name = name;
		this.description = description;
		this.longDescription = longDescription;
		this.type = type;
		this.options = options;
		this.global = global;
	}
	
	/**
	 * A comparison method which sorts ascending by command type ascending, then ascending alphabetically.
	 */
	public int compareTo(SlashCommandObject that) {
		//sort by CommandType then alphabetical
		int compare = this.getType().ordinal() - that.getType().ordinal();
		return compare != 0 ? compare : this.getName().compareTo(that.getName());
	}
	
	/**
	 * Gets this slash command object's name.
	 * @return this slash command object's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Return the object's description. Discord imposes a 100 character limit on this field, so if more text is needed,
	 * this field contains a short summary and the full text is put into the long description field; otherwise that field will be null.
	 * @return This object's description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the long description field, which will be null if the full text fits in the regular "description" field's 100 character limit.
	 * NB: Most callers will want getFullDescription() instead to get the fullest description possible (long description if not null, description otherwise).
	 * @return This object's lengthy description, if necessary
	 */
	public String getLongDescription() {
		return longDescription;
	}
	
	/**
	 * Gets this slash command object's type.
	 * @return this slash command object's type
	 */
	public CommandType getType() {
		return type;
	}
	
	/**
	 * Gets this slash command object's children, mapped by their name.
	 * @return this slash command object's children
	 */
	public Map<String, SlashCommandObject> getOptions() {
		return options;
	}
	
	/**
	 * Gets whether this slash command object is usable in DMs.
	 * @return whether this slash command object is usable in DMs
	 */
	public boolean isGlobal() {
		return global;
	}
	
	/**
	 * Returns the fullest description possible of this object. See getDescription() and getLongDescription().
	 * @return This object's lengthy description if available, regular description otherwise
	 */
	public String getFullDescription() {
		return longDescription != null ? longDescription : description;
	}
}
