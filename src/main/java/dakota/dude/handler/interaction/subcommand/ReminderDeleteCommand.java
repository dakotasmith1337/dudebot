package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class ReminderDeleteCommand extends SlashCommandHandler {

	public ReminderDeleteCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Deletes a reminder for a user.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long userId = event.getInteraction().getUser().getId().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("delete").get();
		Long reminderId = operation.getOption("id").get().getValue().get().asLong();
		if(DatabaseHandler.deleteReminder(reminderId, userId)) {
			return event.createFollowup("Reminder successfully removed.");
		} else {
			return event.createFollowup("No reminder exists with that id.");
		}
	}

}
