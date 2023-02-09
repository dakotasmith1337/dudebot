package dakota.dude.handler.interaction.subcommand;

import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import dakota.dude.TestParent;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.model.Reminder;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;

/**
 * Data:
 * User ID: 1
 * Channel ID: 1
 *
 */
public class ReminderAddCommandTest extends TestParent {
	
	private static final String STANDARD_TEXT = "test event";
	private static final Boolean STANDARD_MENTION = false;
	private static final Boolean STANDARD_RECURRING = false;
	private static final Boolean STANDARD_OFFSET = false;
	private static final String STANDARD_TIMEZONE = "America/Chicago";
	private static final String SUCCESS_MESSAGE = "Reminder successfully created. There are 0 other reminders already in this time slot.";
	
	@Test
	public void testBaseLineReminder() {
		ChatInputInteractionEvent event = Mockito.mock(ChatInputInteractionEvent.class);
		
		Interaction interaction = Mockito.mock(Interaction.class);
		Mockito.when(event.getInteraction()).thenReturn(interaction);
		Mockito.when(interaction.getChannelId()).thenReturn(Snowflake.of(1L));
		
		User user = Mockito.mock(User.class);
		Mockito.when(interaction.getUser()).thenReturn(user);
		Mockito.when(user.getId()).thenReturn(Snowflake.of(1L));
		
		ApplicationCommandInteractionOption operation = Mockito.mock(ApplicationCommandInteractionOption.class);
		Mockito.when(event.getOption("add")).thenReturn(Optional.of(operation));
		
		//argument related mocks
		ApplicationCommandInteractionOption eventOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption mentionOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption recurringOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption offsetOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption yearOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption monthOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption dayOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption hourOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption amOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption minuteOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption timezoneOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption frequencyAmountOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		ApplicationCommandInteractionOption frequencyOption = Mockito.mock(ApplicationCommandInteractionOption.class);
		
		Mockito.when(operation.getOption("event")).thenReturn(Optional.of(eventOption));
		Mockito.when(operation.getOption("mention")).thenReturn(Optional.of(mentionOption));
		Mockito.when(operation.getOption("recurring")).thenReturn(Optional.of(recurringOption));
		Mockito.when(operation.getOption("offset")).thenReturn(Optional.of(offsetOption));
		Mockito.when(operation.getOption("year")).thenReturn(Optional.of(yearOption));
		Mockito.when(operation.getOption("month")).thenReturn(Optional.of(monthOption));
		Mockito.when(operation.getOption("day")).thenReturn(Optional.of(dayOption));
		Mockito.when(operation.getOption("hour")).thenReturn(Optional.of(hourOption));
		Mockito.when(operation.getOption("am")).thenReturn(Optional.of(amOption));
		Mockito.when(operation.getOption("minute")).thenReturn(Optional.of(minuteOption));
		Mockito.when(operation.getOption("timezone")).thenReturn(Optional.of(timezoneOption));
		Mockito.when(operation.getOption("frequencyAmount")).thenReturn(Optional.of(frequencyAmountOption));
		Mockito.when(operation.getOption("frequency")).thenReturn(Optional.of(frequencyOption));
		
		Mockito.when(eventOption.getValue()).thenReturn(Optional.of(new ApplicationCommandInteractionOptionValue(null, null, 3, STANDARD_TEXT)));
		Mockito.when(mentionOption.getValue()).thenReturn(Optional.of(new ApplicationCommandInteractionOptionValue(null, null, 5, STANDARD_MENTION.toString())));
		Mockito.when(recurringOption.getValue()).thenReturn(Optional.of(new ApplicationCommandInteractionOptionValue(null, null, 5, STANDARD_RECURRING.toString())));
		Mockito.when(offsetOption.getValue()).thenReturn(Optional.of(new ApplicationCommandInteractionOptionValue(null, null, 5, STANDARD_OFFSET.toString())));
		Mockito.when(yearOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(monthOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(dayOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(hourOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(amOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(minuteOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(timezoneOption.getValue()).thenReturn(Optional.of(new ApplicationCommandInteractionOptionValue(null, null, 3, STANDARD_TIMEZONE)));
		Mockito.when(frequencyAmountOption.getValue()).thenReturn(Optional.ofNullable(null));
		Mockito.when(frequencyOption.getValue()).thenReturn(Optional.ofNullable(null));
		
		ReminderAddCommand command = new ReminderAddCommand(null, null, null, null, null, true);
		command.execute(event);
		Mockito.verify(event).createFollowup(SUCCESS_MESSAGE);
		
		//minor workaround to get the reminder, since we don't have access to the primary key since the call to addReminder() is inside the command
		List<Reminder> reminders = DatabaseHandler.getReminders(1L);
		assertTrue("Error adding baseline reminder to database", !reminders.isEmpty());
		Reminder reminder = DatabaseHandler.getReminder(reminders.get(0).getPrimaryKey(), 1L);
		assertTrue("Reminder event text is incorrect", reminder.getEvent().equals(STANDARD_TEXT));
		assertTrue("Reminder mention is incorrect", !reminder.getMention());
		assertTrue("Reminder recurring is incorrect", !reminder.getRecurring());
		assertTrue("Reminder time is incorrect", reminder.getTime().equals(ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant()));
		assertTrue("Reminder frequency amount not null", reminder.getFrequencyAmount() == null);
		assertTrue("Reminder frequency is not null", reminder.getFrequency() == null);
		assertTrue("Reminder user ID is incorrect", reminder.getUserId() == 1L);
		assertTrue("Reminder channel ID is incorrect", reminder.getChannelId() == 1L);
	}
}
