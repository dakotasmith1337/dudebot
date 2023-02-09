package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.ReminderSettings;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class ReminderSettingsModifyCommand extends SlashCommandHandler {

	public ReminderSettingsModifyCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Updates any of a guild's settings for reminder channels.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("settings").get().getOption("modify").get();
		Boolean restrictReminders = operation.getOption("restrictreminders").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Boolean whitelist = operation.getOption("whitelist").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		
		if(restrictReminders == null && whitelist == null) {
			return event.createFollowup("You must provide a value for either restrictreminders or whitelist.");
		}
		
		if(restrictReminders != null) {
			if(whitelist != null) {
				DatabaseHandler.setReminderSettings(guildId, new ReminderSettings(restrictReminders, whitelist));
			} else {
				DatabaseHandler.setRestrictReminders(guildId, restrictReminders);
			}
		} else {
			DatabaseHandler.setWhitelist(guildId, whitelist);
		}
		return event.createFollowup("Reminder settings successfully updated.");
	}

}
