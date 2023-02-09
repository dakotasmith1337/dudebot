package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.ReminderSettings;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class ReminderSettingsViewCommand extends SlashCommandHandler {

	public ReminderSettingsViewCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays a guild's settings for reminder channels.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		ReminderSettings settings = DatabaseHandler.getReminderSettings(event.getInteraction().getGuildId().get().asLong());
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(EmbedCreateSpec.create().withTitle("Reminder Settings").withFields(
				EmbedCreateFields.Field.of("Restrict Reminders", settings.getRestrictReminders().toString(), true),
				EmbedCreateFields.Field.of("Whitelist", settings.getWhitelist().toString(), true)
		)));
	}

}
