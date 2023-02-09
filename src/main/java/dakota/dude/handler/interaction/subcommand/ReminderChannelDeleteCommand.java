package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

public class ReminderChannelDeleteCommand extends SlashCommandHandler {

	public ReminderChannelDeleteCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Deletes a channel from the reminder restriction list.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Mono<Channel> channelMono = event.getOption("channel").get().getOption("delete").get().getOption("channel").get().getValue().get().asChannel();
		
		return channelMono.flatMap(channel -> {
			if(DatabaseHandler.deleteReminderChannel(event.getInteraction().getGuildId().get().asLong(), channel.getId().asLong())) {
				return event.createFollowup("Channel successfully removed as a reminder channel.");
			} else {
				return event.createFollowup("This channel is already absent from the reminder channel list.");
			}
		});
	}

}
