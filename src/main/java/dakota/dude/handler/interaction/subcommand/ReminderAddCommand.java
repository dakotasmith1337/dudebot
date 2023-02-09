package dakota.dude.handler.interaction.subcommand;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Reminder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class ReminderAddCommand extends SlashCommandHandler {

	public ReminderAddCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Adds a reminder for a user.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long userId = event.getInteraction().getUser().getId().asLong();
		Long channelId = event.getInteraction().getChannelId().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("add").get();
		String eventText = operation.getOption("event").get().getValue().get().asString();
		boolean mention = operation.getOption("mention").get().getValue().get().asBoolean();
		boolean recurring = operation.getOption("recurring").get().getValue().get().asBoolean();
		boolean offset = operation.getOption("offset").get().getValue().get().asBoolean();
		Long year = operation.getOption("year").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long month = operation.getOption("month").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long day = operation.getOption("day").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long hour = operation.getOption("hour").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Boolean hourIsAM = operation.getOption("hourisam").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Long minute = operation.getOption("minute").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		String timezone = operation.getOption("timezone").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		Long frequencyAmount = operation.getOption("frequencyamount").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		String frequency = operation.getOption("frequency").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		
		if(eventText.length() > 1900) {
			return event.createFollowup("Your event text was too long. It can't be longer than 1900 characters.");
		}
		
		Instant eventTime;
		
		if(offset) {
			if(year == null && month == null && day == null && hour == null && minute == null) {
				return event.createFollowup("You chose an amount of time ('offset' == true) but all of the time fields (year, month, day, hour, minute) were null.");
			}
			
			try {
				eventTime = ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(minute != null ? minute : 0).plusHours(hour != null ? hour : 0).plusDays(day != null ? day : 0)
						.plusMonths(month != null ? month : 0).plusYears(year != null ? year : 0).truncatedTo(ChronoUnit.MINUTES).toInstant();
			} catch(DateTimeException e) {
				logger.debug(e);
				return event.createFollowup("One or more of your time fields was too large; dates that far in the future aren't supported.");
			}
		} else {
			if(timezone == null) {
				return event.createFollowup("You chose a specific point in time (offset == false) but did not choose a timezone.");
			}
			
			if(hour != null && hourIsAM == null) {
				return event.createFollowup("You chose a specific amount of time (offset == false) and specified the hour, but did not choose a.m. or p.m.");
			}
			
			try {
				eventTime = Reminder.calculateTime(timezone, year, month, day, hour, hourIsAM, minute);
			} catch (DateTimeException e) {
				logger.debug(e);
				return event.createFollowup("One or more of your time fields was invalid: year cannot be greater than 999999999, month must be 1-12, day must be 1-28/30/31 depending on the month, hour must be 1-12, and minute must be 0-59.");
			}
			if(eventTime.isBefore(ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(1).truncatedTo(ChronoUnit.MINUTES).toInstant())) {
				return event.createFollowup("The event time must be in the future.");
			}
		}
		
		if(recurring && (frequencyAmount == null || frequency == null)) {
			return event.createFollowup("You chose for the reminder to be recurring but did not choose how often ('frequency' and/or 'frequencyamount' were null).");
		}
		
		List<Reminder> currentReminders = DatabaseHandler.getReminders(userId);
		if(currentReminders.size() > 2) {
			return event.createFollowup("You have already hit the limit of 3 reminders.");
		}
		
		
		for(Reminder reminder : currentReminders) {
			if(ChronoUnit.MINUTES.between(eventTime, reminder.getTime()) == 0L) {
				return event.createFollowup("You already have a reminder (ID: " + reminder.getPrimaryKey() + ") that is scheduled for this time.");
			}
		}
		
		StringBuilder text = new StringBuilder(eventText);
		if(mention) {
			text.insert(0, " ");
			text.insert(0, event.getInteraction().getUser().getMention());
		}
		
		Reminder newReminder = new Reminder(null, text.toString(), eventTime, mention, recurring, frequencyAmount, 
				frequency != null ? ChronoUnit.valueOf(frequency) : null, userId, channelId);
		
		DatabaseHandler.addReminder(newReminder);
		long existingReminders = DatabaseHandler.getReminderCount(eventTime.getEpochSecond()) - 1;
		String amount = existingReminders != 1 ? "are " + existingReminders + " other reminders" : "is 1 other reminder";
		return event.createFollowup("Reminder successfully created. There " + amount + " already in this time slot globally.");
	}
}
