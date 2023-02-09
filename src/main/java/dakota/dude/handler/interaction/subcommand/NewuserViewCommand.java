package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.UserJoinSettings;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class NewuserViewCommand extends SlashCommandHandler {

	public NewuserViewCommand(String name, String description, String longDescription, CommandType type, Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays a guild's settings for newly joining users.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		UserJoinSettings settings = DatabaseHandler.getUserJoinSettings(event.getInteraction().getGuildId().get().asLong());
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(EmbedCreateSpec.create().withTitle("Reminder Settings").withFields(
			EmbedCreateFields.Field.of("Enabled", settings.getEnabled().toString(), true),
			EmbedCreateFields.Field.of("Mention", settings.getMention().toString(), true),
			EmbedCreateFields.Field.of("Message", settings.getMessage() == null ? "<not yet set>" :
				settings.getMessage(), false),
			EmbedCreateFields.Field.of("Channel", settings.getChannelId() == null ? "<not yet set>" :
				DudeUtility.getChannelMention(settings.getChannelId()), false)
		)));
	}

}
