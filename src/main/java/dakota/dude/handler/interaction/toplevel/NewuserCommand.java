package dakota.dude.handler.interaction.toplevel;

import java.util.Map;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;

public class NewuserCommand extends TopLevelSlashCommandHandler {

	public NewuserCommand(String name, String description, String longDescription, CommandType type, Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

}
