package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

/**
 * This class represents subcommands, i.e. SlashCommandObjects whose type is Command.
 * Note on some of this class's subclasses: I really hate that some of them violate camel case, but discord requires json object names to be all lower case
 * and trying to use proper camel case in spite of that causes problems with getting their constructors through reflection.
 *
 */
public abstract class SlashCommandHandler extends SlashCommandObject {

	protected static final Logger logger = LogManager.getLogger();
	
	public SlashCommandHandler(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Defines the actions a handler should take when its particular event is received.
	 * @param event
	 * @return the message to send in response if needed, an empty Mono otherwise
	 */
	public abstract Mono<Message> execute(ChatInputInteractionEvent event);
}
