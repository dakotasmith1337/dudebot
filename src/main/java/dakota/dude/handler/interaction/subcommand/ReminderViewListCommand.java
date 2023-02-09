package dakota.dude.handler.interaction.subcommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Reminder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class ReminderViewListCommand extends SlashCommandHandler {

	public ReminderViewListCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays the list of reminders for a user.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long userId = event.getInteraction().getUser().getId().asLong();
		
		List<Reminder> reminders = DatabaseHandler.getReminders(userId);
		
		if(reminders.isEmpty()) {
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
				EmbedCreateSpec.create().withTitle("Reminders").withDescription("No reminders found.\nAdd some with /reminder add."))
			);
		}
		
		//ensure the information stays ordered
		List<Long> ids = new ArrayList<>();
		List<String> times = new ArrayList<>();
		List<String> events = new ArrayList<>();
		for(Reminder reminder : reminders) {
			ids.add(reminder.getPrimaryKey());
			times.add(reminder.getTimeString());
			events.add(reminder.getEvent());
		}
		Map<String, List<? extends Object>> columns = new LinkedHashMap<>();
		columns.put("ID", ids);
		columns.put("Time", times);
		columns.put("Event", events);
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
			DudeUtility.createListEmbed("Reminder", columns)
		));
	}

}
