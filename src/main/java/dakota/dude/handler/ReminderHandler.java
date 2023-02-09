package dakota.dude.handler;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dakota.dude.Dude;
import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.model.Reminder;
import dakota.dude.model.ReminderSettings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

public class ReminderHandler {
	
	private static final Logger logger = LogManager.getLogger();
	
	private static volatile boolean shutdown = false;
	
	/**
	 * Stops the reminder loop from continuing execution once it finishes its current batch.
	 */
	public static void shutdown() {
		shutdown = true;
	}
	
	/**
	 * Enters a loop and continuously handles sending reminder messages every minute until shutdown() is called.
	 */
	public static void handle() {
		LocalTime nextStartTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
		while(!shutdown) {
			try {
				nextStartTime = nextStartTime.plusMinutes(1);
				handleReminders();
			} catch(Exception error) {
				logger.error("Error processing reminders: ", error);
			} finally {
				//error or not, make sure we wait before trying again
				try {
					//if it didn't take at least a full minute to process, sleep until the start of the next minute
					LocalTime now = LocalTime.now();
					if(now.isBefore(nextStartTime)) {
						Thread.sleep(now.until(nextStartTime, ChronoUnit.MILLIS));
					}
				} catch(InterruptedException error) {
					logger.warn("Reminder thread was interrupted:\n", error);
				}
			}
		}
	}

	/**
	 * Get all reminders that are due or past due, send to the desired channel if possible or to the user's DMs otherwise,
	 * then delete the reminder if non-recurring, otherwise update the new time based on its recurrence frequency.
	 */
	private static void handleReminders() {
		List<Reminder> reminders = DatabaseHandler.getNextReminders(ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.MINUTES).toEpochSecond());
		Set<Long> remindersToDelete = new HashSet<>();
		reminders.parallelStream().forEach(reminder -> {
			Mono<PrivateChannel> userDm = Dude.getClient().getUserById(Snowflake.of(reminder.getUserId())).flatMap(user -> user.getPrivateChannel());
			
			//if the channel is a guild channel and either it's been deleted or no longer accepts reminders, use DM as a backup
			Dude.getClient().getChannelById(Snowflake.of(reminder.getChannelId())).filter(channel -> channel instanceof GuildMessageChannel).flatMap(channel -> {
				Long guildId = ((GuildMessageChannel) channel).getGuildId().asLong();
				//whitelist and channel not in list, or blacklist and channel in list
				ReminderSettings settings = DatabaseHandler.getReminderSettings(guildId);
				if(settings.getRestrictReminders() && (settings.getWhitelist() ^ DatabaseHandler.getReminderChannels(guildId).contains(channel.getId().asLong()))) {
					return userDm;
				} else {
					return Mono.just(channel);
				}
			}).switchIfEmpty(userDm).cast(MessageChannel.class).flatMap(channel -> channel.createMessage(reminder.getEvent())).subscribe($ -> {
				DudeUtility.logWithGuildId(logger, null, DudeUtility.LoggerLevel.DEBUG, "Reminder sent for user " + reminder.getUserId() + " in channel " + $.getChannelId().asLong());
			}, error -> {
				DudeUtility.logExceptionWithGuildId(logger, null, DudeUtility.LoggerLevel.ERROR, "Error sending reminder for user " + reminder.getUserId() + ":", error);
			});
			
			if(reminder.getRecurring()) {
				reminder.setTime(reminder.getTime().plus(reminder.getFrequencyAmount(), reminder.getFrequency()));
				DatabaseHandler.updateRecurringReminder(reminder);
			} else {
				remindersToDelete.add(reminder.getPrimaryKey());
			}
		});
		
		if(!remindersToDelete.isEmpty()) DatabaseHandler.deleteReminders(remindersToDelete);
	}
}
