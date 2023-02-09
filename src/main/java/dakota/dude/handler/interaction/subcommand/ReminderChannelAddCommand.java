package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

public class ReminderChannelAddCommand extends SlashCommandHandler {

	public ReminderChannelAddCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Adds a channel to the reminder restriction list.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Mono<Channel> channelMono = event.getOption("channel").get().getOption("add").get().getOption("channel").get().getValue().get().asChannel();
		
		return channelMono.flatMap(channel -> {
			DatabaseHandler.addReminderChannel(event.getInteraction().getGuildId().get().asLong(), channel.getId().asLong());
			return event.createFollowup("Channel successfully added as a reminder channel.");
		});
	}

}
