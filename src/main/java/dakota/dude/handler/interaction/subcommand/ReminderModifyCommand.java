package dakota.dude.handler.interaction.subcommand;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Reminder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

public class ReminderModifyCommand extends SlashCommandHandler {

	public ReminderModifyCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Updates any of the values for a user's reminder.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long userId = event.getInteraction().getUser().getId().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("modify").get();
		Long id = operation.getOption("id").get().getValue().get().asLong();
		String eventText = operation.getOption("event").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		Boolean mention = operation.getOption("mention").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Boolean recurring = operation.getOption("recurring").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Boolean offset = operation.getOption("offset").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Long year = operation.getOption("year").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long month = operation.getOption("month").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long day = operation.getOption("day").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Long hour = operation.getOption("hour").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Boolean hourIsAM = operation.getOption("hourisam").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Long minute = operation.getOption("minute").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		String timezone = operation.getOption("timezone").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		String frequency = operation.getOption("frequency").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		Long frequencyAmount = operation.getOption("frequencyamount").flatMap(option -> option.getValue()).map(value -> value.asLong()).orElse(null);
		Mono<Channel> channel = operation.getOption("channel").flatMap(option -> option.getValue()).map(value -> value.asChannel()).orElse(null);
		
		if(eventText == null && mention == null && recurring == null && year == null && month == null && day == null && hour == null && minute == null
				&& frequency == null && frequencyAmount == null && channel == null) {
			return event.createFollowup("Please provide a value for at least one modifiable field (note: the offset, hourIsAM, and timezone components"
					+ " are not directly modifiable, they are only used in conjunction with other time parameters).");
		}
		
		Reminder reminder = DatabaseHandler.getReminder(id, userId);
		if(reminder == null) {
			return event.createFollowup("No reminder was found with that ID. Use /reminder view list to your list of reminders and their IDs.");
		}
		
		if(eventText != null) {
			//add the user mention to the string now, so we don't have to look up the user for their name later
			if(mention != null ? mention : reminder.getMention()) {
				reminder.setEvent(event.getInteraction().getUser().getMention() + " " + eventText);
			} else {
				reminder.setEvent(eventText);
			}
			
			if(mention != null) reminder.setMention(mention);
		} else if(mention != null) {
			//if only mention changed, add/remove the mention to/from the event string
			if(mention && !reminder.getMention()) {
				reminder.setEvent(event.getInteraction().getUser().getMention() + " " + reminder.getEvent());
			} else if(!mention && reminder.getMention()) {
				reminder.setEvent(reminder.getEvent().substring(event.getInteraction().getUser().getMention().length() + 1));
			}
			reminder.setMention(mention);
		}
		
		final boolean timeChanged = year != null || month != null || day != null || hour != null || minute != null;
		if(timeChanged) {
			if(offset == null) {
				return event.createFollowup("You must provide a value for offset when modifying the time.");
			}
			
			if(offset) {
				try {
					reminder.setTime(ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(minute != null ? minute : 0).plusHours(hour != null ? hour : 0).plusDays(day != null ? day : 0)
							.plusMonths(month != null ? month : 0).plusYears(year != null ? year : 0).truncatedTo(ChronoUnit.MINUTES).toInstant());
				} catch(DateTimeException e) {
					return event.createFollowup("One or more of your time fields was too large; dates that far in the future aren't supported.");
				}
			} else {
				if(timezone == null) {
					return event.createFollowup("You chose a specific point in time (offset == false) but did not choose a timezone.");
				}
				
				if(hour != null && hourIsAM == null) {
					return event.createFollowup("You chose a specific amount of time (offset == false) and specified the hour, but did not choose a.m. or p.m.");
				}
			}
			
			try {
				reminder.setTime(Reminder.calculateTime(timezone, year, month, day, hour, hourIsAM, minute));
			} catch(DateTimeException e) {
				return event.createFollowup("One or more of your time fields was invalid: year cannot be greater than 999999999, month must be 1-12, day must be 1-28/30/31 depending on the month, hour must be 1-12, and minute must be 0-59.");
			}
		} else {
			if(offset != null) {
				return event.createFollowup("The offset component cannot be modified directly, it is only used in conjunction with the rest of the time parameters.");
			}
			if(hourIsAM != null) {
				return event.createFollowup("The a.m./p.m. component cannot be modified directly, it is only used in conjunction with the hour parameter.");
			}
			if(timezone != null) {
				return event.createFollowup("The timezone component cannot be modified directly, it is only used in conjunction with the rest of the time parameters.");
			}
		}
		
		if(recurring != null && (recurring ^ reminder.getRecurring())) {
			if(recurring) {
				if(frequency == null || frequencyAmount == null) {
					return event.createFollowup("You must provide values for frequency and frequency amount if you are setting the reminder to be recurring.");
				}
			} else {
				reminder.setFrequency(null);
				reminder.setFrequencyAmount(null);
			}
			reminder.setRecurring(recurring);
		}
		
		if(reminder.getRecurring() && frequency != null) {
			reminder.setFrequency(ChronoUnit.valueOf(frequency));
		}
		
		if(reminder.getRecurring() && frequencyAmount != null) {
			reminder.setFrequencyAmount(frequencyAmount);
		}
		
		if(channel != null) {
			return channel.flatMap(reminderChannel -> {
				reminder.setChannelId(reminderChannel.getId().asLong());
				
				DatabaseHandler.modifyReminder(reminder);
				if(timeChanged) {
					return event.createFollowup("Reminder successfully updated. There are " + (DatabaseHandler.getReminderCount(reminder.getTime().getEpochSecond()) + 1) + " other reminders already in this time slot.");
				} else {
					return event.createFollowup("Reminder successfully updated.");
				}
			});
		} else {
			DatabaseHandler.modifyReminder(reminder);
			if(timeChanged) {
				return event.createFollowup("Reminder successfully updated. There are " + (DatabaseHandler.getReminderCount(reminder.getTime().getEpochSecond()) + 1) + " other reminders already in this time slot.");
			} else {
				return event.createFollowup("Reminder successfully updated.");
			}
		}
	}

}
