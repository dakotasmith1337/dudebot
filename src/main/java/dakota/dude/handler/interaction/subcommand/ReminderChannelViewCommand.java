package dakota.dude.handler.interaction.subcommand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CacheMap;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class ReminderChannelViewCommand extends SlashCommandHandler {
	
	private static CacheMap<List<String>> guildReminderChannels = new CacheMap<>();

	public ReminderChannelViewCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}
	
	public static List<String> getGuildReminderChannels(Long messageId) {
		return guildReminderChannels.get(messageId);
	}

	/**
	 * Displays the list of restricted reminder channels.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		List<String> channels = DatabaseHandler.getReminderChannels(event.getInteraction().getGuildId().get().asLong())
				.stream().map(DudeUtility::getChannelMention).collect(Collectors.toList());
		Map<String, List<? extends Object>> columns = new LinkedHashMap<>();
		columns.put("Channel", channels.size() > 25 ? channels.subList(0, 25) : channels);
		
		if(channels.size() > 25) {
			return event.createFollowup().withEmbeds(DudeUtility.createListEmbed("Reminder Channel", columns)).withComponents(DudeUtility.createButtonScrollRow("ReminderChannel", 1))
					.doOnNext(message -> guildReminderChannels.put(message.getId().asLong(), channels));
		} else {
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(DudeUtility.createListEmbed("ReminderChannel", columns)));
		}
	}
}
