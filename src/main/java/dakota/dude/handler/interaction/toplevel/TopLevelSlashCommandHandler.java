package dakota.dude.handler.interaction.toplevel;

import java.util.List;
import java.util.Map;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.handler.interaction.subcommand.SlashCommandHandler;

import java.util.HashMap;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

/**
 * The class that represents top level commands.
 *
 */
public abstract class TopLevelSlashCommandHandler extends SlashCommandHandler {
	
	protected static Map<String, TopLevelSlashCommandHandler> topLevelInteractions = new HashMap<String, TopLevelSlashCommandHandler>();
	
	/**
	 * Places this new TopLevelSlashCommandHandler into a map containing all TopLevelSlashCommandHandler instances.
	 * @param name
	 * @param description
	 * @param longDescription
	 * @param type
	 * @param options
	 * @param global
	 */
	public TopLevelSlashCommandHandler(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
		topLevelInteractions.put(name, this);
	}
	
	/**
	 * Top level commands with subcommands/groups, e.g. /routine, are never executed, so a default empty implementation is provided here
	 * to prevent needing to do so in each of those commands' handlers.
	 * The others, such as RollCommand, can just override this method again to provide their implementation.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		return Mono.empty();
	}
	
	/**
	 * Can be overridden if any initialization work is needed before the command is executed.
	 * This method will be executed just prior to receiving events from Discord.
	 */
	protected void initialize() {}
	
	/**
	 * Executes initialize() for every TopLevelSlashCommandHandler instance.
	 */
	public static void initializeAllTopLevelCommands() {
		for(TopLevelSlashCommandHandler command : topLevelInteractions.values()) {
			command.initialize();
		}
	}
	
	/**
	 * Gets a TopLevelSlashCommandHandler by its name.
	 * @param name
	 * @return the desired TopLevelSlashCommandHandler
	 */
	public static TopLevelSlashCommandHandler getTopLevelHandler(String name) {
		return topLevelInteractions.get(name);
	}
	
	/**
	 * Calls execute for either this handler or the subcommand handler as appropriate.
	 * @param event
	 * @return The message to be sent in response
	 */
	public Mono<Message> executeActualCommand(ChatInputInteractionEvent event) {
		List<ApplicationCommandInteractionOption> options = event.getOptions();
		if(!options.isEmpty()) {
			//a child InteractionObject is the one actually being executed (direct child if subcommand, grandchild if group)
			if(options.get(0).getType().equals(ApplicationCommandOption.Type.SUB_COMMAND)) {
				return ((SlashCommandHandler) getOptions().get(
							options.get(0).getName()
						)).execute(event);
			} else if (options.get(0).getType().equals(ApplicationCommandOption.Type.SUB_COMMAND_GROUP)) {
				//extremely ugly but basically get object's child by event child's name, then object child's child by event child's child's name
				return 	((SlashCommandHandler) getOptions().get(
							options.get(0).getName()
						).getOptions().get(
							options.get(0).getOptions().get(0).getName()
						)).execute(event);
			}
		}
		//no non-argument children, valid top level command
		return execute(event);
	}
}
