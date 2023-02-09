package dakota.dude.handler.interaction.toplevel;

import java.util.Map;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;


public class ColorCommand extends TopLevelSlashCommandHandler {
	
	public ColorCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> children, boolean global) {
		super(name, description, longDescription, type, children, global);
	}
}
