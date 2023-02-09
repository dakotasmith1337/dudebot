package dakota.dude.handler.interaction.toplevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class QuoteCommand extends TopLevelSlashCommandHandler {
	
	private static String[] quotes = null;

	public QuoteCommand(String name, String description, String longDescription, CommandType type, Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Returns a random quote.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		return event.createFollowup(quotes[(int) (Math.random() * quotes.length)]);
	}

	/**
	 * Retrieves the list of available quotes from a text file resource.
	 */
	@Override
	protected void initialize() {
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			result.write(QuoteCommand.class.getClassLoader().getResourceAsStream("quotes.txt").readAllBytes());
			quotes = result.toString("UTF-8").split(System.lineSeparator());
		} catch (IOException error) {
			logger.error(error);
			throw new RuntimeException(error);
		}
	}
}
