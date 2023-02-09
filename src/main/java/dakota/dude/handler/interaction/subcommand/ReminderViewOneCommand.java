package dakota.dude.handler.interaction.subcommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Reminder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class ReminderViewOneCommand extends SlashCommandHandler {

	public ReminderViewOneCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays detailed information for one reminder.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long userId = event.getInteraction().getUser().getId().asLong();
		Long id = event.getOption("view").get().getOption("one").get().getOption("id").get().getValue().get().asLong();
		
		Reminder reminder = DatabaseHandler.getReminder(id, userId);
		
		if(reminder == null) {
			return event.createFollowup("No reminder was found with that ID.");
		}
		
		List<Field> fields = new ArrayList<Field>();
		fields.add(EmbedCreateFields.Field.of("ID", reminder.getPrimaryKey().toString(), true));
		fields.add(EmbedCreateFields.Field.of("Time", reminder.getTimeString(), true));
		fields.add(EmbedCreateFields.Field.of("Event", reminder.getEvent().length() > 50 ? reminder.getEvent().substring(0, 50) : reminder.getEvent(), false));
		fields.add(EmbedCreateFields.Field.of("Channel", DudeUtility.getChannelMention(reminder.getChannelId()), false));
		fields.add(EmbedCreateFields.Field.of("Recurring", reminder.getRecurring().toString(), true));
		if(reminder.getRecurring()) {
			fields.add(EmbedCreateFields.Field.of("Frequency Amount", reminder.getFrequencyAmount().toString(), true));
			fields.add(EmbedCreateFields.Field.of("Frequency", reminder.getFrequency().toString(), true));
		}
		EmbedCreateSpec embed = EmbedCreateSpec.create().withTitle("Reminder").withFields(fields);
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(embed));
	}

}
