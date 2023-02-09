package dakota.dude.model;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import discord4j.common.util.Snowflake;

public class Reminder {
	
	private Long primaryKey;
	
	private String event;
	private Instant time;
	private Boolean mention;
	private Boolean recurring;
	private Long frequencyAmount;
	private ChronoUnit frequency;
	private Long userId;
	private Long channelId;
	
	public Reminder(Long primaryKey, String event, Instant time, Boolean mention, Boolean recurring, Long frequencyAmount, ChronoUnit frequency, Long userId, Long channelId) {
		this.primaryKey = primaryKey;
		this.event = event;
		this.time = time;
		this.mention = mention;
		this.recurring = recurring;
		this.frequencyAmount = frequencyAmount;
		this.frequency = frequency;
		this.userId = userId;
		this.channelId = channelId;
	}
	
	/**
	 * Returns the primary key.
	 * @return The primary key
	 */
	public Long getPrimaryKey() {
		return primaryKey;
	}
	
	/**
	 * Sets the primary key.
	 * @param primaryKey The primary key
	 */
	public void setPrimaryKey(Long primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * Returns the reminder event.
	 * @return The reminder event
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Sets the reminder event.
	 * @param event The reminder event
	 */
	public void setEvent(String event) {
		this.event = event;
	}

	/**
	 * Returns the reminder time.
	 * @return The reminder time
	 */
	public Instant getTime() {
		return time;
	}

	/**
	 * Sets the reminder time.
	 * @param time The time of the event
	 */
	public void setTime(Instant time) {
		this.time = time;
	}

	/**
	 * Returns whether or not the user should be mentioned when the reminder is triggered.
	 * @return Whether the user should be mentioned
	 */
	public Boolean getMention() {
		return mention;
	}

	/**
	 * Sets whether or not the user should be mentioned when the reminder is triggered.
	 * @param mention Whether the user should be mentioned
	 */
	public void setMention(Boolean mention) {
		this.mention = mention;
	}

	/**
	 * Returns whether or not the reminder should recur continually.
	 * @return Whether the reminder should recur continually
	 */
	public Boolean getRecurring() {
		return recurring;
	}

	/**
	 * Sets whether or not the reminder should recur continually.
	 * @param recurring Whether the reminder should recur continually
	 */
	public void setRecurring(Boolean recurring) {
		this.recurring = recurring;
	}

	/**
	 * Returns the amount of the recurrence unit of the reminder.
	 * @return The amount of the recurrence unit of the reminder
	 */
	public Long getFrequencyAmount() {
		return frequencyAmount;
	}

	/**
	 * Sets the amount of the recurrence unit of the reminder.
	 * @param period The amount of the recurrence unit of the reminder
	 */
	public void setFrequencyAmount(Long frequencyAmount) {
		this.frequencyAmount = frequencyAmount;
	}

	/**
	 * Returns the frequency of the recurrence of the reminder.
	 * @return The frequency of the recurrence of the reminder
	 */
	public ChronoUnit getFrequency() {
		return frequency;
	}

	/**
	 * Sets the frequency of the recurrence of the reminder.
	 * @param period The frequency of the recurrence of the reminder
	 */
	public void setFrequency(ChronoUnit frequency) {
		this.frequency = frequency;
	}

	/**
	 * Returns the reminder user's ID.
	 * @return The reminder user's ID
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * Sets the reminder user's ID.
	 * @param userId The ID of the user to be reminded
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Returns the id of the channel to remind in.
	 * @return The id of the channel to remind in
	 */
	public long getChannelId() {
		return channelId;
	}

	/**
	 * Sets the reminder channel's ID.
	 * @param channelId The ID of the channel to remind in
	 */
	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}
	
	/**
	 * Convenience method to get the Snowflake object for the user's ID.
	 * @return The Snowflake object for the user's ID
	 */
	public Snowflake getUserSnowflake() {
		return Snowflake.of(userId);
	}
	
	/**
	 * Convenience method to get the Snowflake object for the channel's ID.
	 * @return The Snowflake object for the channel's ID
	 */
	public Snowflake getChannelSnowflake() {
		return Snowflake.of(channelId);
	}
	
	/**
	 * Gets the formatted timestamp for this Reminder's time.
	 * @return
	 */
	public String getTimeString() {
		//discord parses this with the user's timezone, meaning for the bot it will be in the jvm timezone
		return "<t:" + ZonedDateTime.ofInstant(time, ZoneId.systemDefault()).toEpochSecond() + ">, <t:" + ZonedDateTime.ofInstant(time, ZoneId.systemDefault()).toEpochSecond() + ":R>";
	}
	
	/**
	 * Calculates the trigger time for this reminder, given that offset is false and many of these values are nullable.
	 * @param timezone
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param am
	 * @param minute
	 * @return The trigger time for this reminder
	 * @throws DateTimeException
	 */
	public static Instant calculateTime(String timezone, Long year, Long month, Long day, Long hour, Boolean am, Long minute) throws DateTimeException {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
		
		//handle defaults; each null time unit should default to its current IRL value or the next, depending on the values of the higher precision units
		//e.g., if day is null, it should be today if the hour and minute are after now, otherwise it should be tomorrow
		if(minute == null) minute = 0L;
		if(hour == null) {
			if(minute > now.getMinute()) hour = (long) now.getHour();
			else hour = (long) now.plusHours(1).getHour();
			am = hour < 12;
		} else {
			//convert from 12-hour to 24-hour
			if(!am) hour += 12;
			else if(hour == 12) hour = 0L;
		}
		if(day == null) {
			if(hour > now.getHour()) day = (long) now.getDayOfMonth();
			else if(hour < now.getHour()) day = (long) now.plusDays(1).getDayOfMonth();
			else {
				if(minute > now.getMinute()) day = (long) now.getDayOfMonth();
				else day = (long) now.plusDays(1).getDayOfMonth();
			}
		}
		if(month == null) {
			if(day > now.getDayOfMonth()) month = (long) now.getMonthValue();
			else if(day < now.getDayOfMonth()) month = (long) now.plusMonths(1).getMonthValue();
			else {
				if(hour > now.getHour()) month = (long) now.getMonthValue();
				else if(hour < now.getHour()) month = (long) now.plusMonths(1).getMonthValue();
				else {
					if(minute > now.getMinute()) month = (long) now.getMonthValue();
					else month = (long) now.plusMonths(1).getMonthValue();
				}
			}
		}
		if(year == null) {
			if(month > now.getMonthValue()) year = (long) now.getYear();
			else if(month < now.getMonthValue()) year = (long) now.plusYears(1).getYear();
			else {
				if(day > now.getDayOfMonth()) year = (long) now.getYear();
				else if(day < now.getDayOfMonth()) year = (long) now.plusYears(1).getYear();
				else {
					if(hour > now.getHour()) year = (long) now.getYear();
					else if(hour < now.getHour()) year = (long) now.plusYears(1).getYear();
					else {
						if(minute > now.getMinute()) year = (long) now.getYear();
						else year = (long) now.plusYears(1).getYear();
					}
				}
			}
		}
		
		//long to int overflow safety is handled by the max_value and min_value restrictions on these arguments in the command definition for reminder
		return ZonedDateTime.of(Math.toIntExact(year), Math.toIntExact(month), Math.toIntExact(day), Math.toIntExact(hour), Math.toIntExact(minute), 0, 0, ZoneId.of(timezone)).toInstant();
	}
}