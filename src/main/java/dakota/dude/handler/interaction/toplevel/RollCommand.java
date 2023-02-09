package dakota.dude.handler.interaction.toplevel;

import java.util.Map;

import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class RollCommand extends TopLevelSlashCommandHandler {

	public RollCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> children, boolean global) {
		super(name, description, longDescription, type, children, global);
	}

	/**
	 * Returns a random number between two numbers, both inclusive.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		long min = event.getOption("min").get().getValue().get().asLong();
		long max = event.getOption("max").get().getValue().get().asLong();
		long random = (long) (Math.random() * (max + 1 - min) + min);
		return event.createFollowup("" + random);
	}
}
