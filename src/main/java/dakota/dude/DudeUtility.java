package dakota.dude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;

public class DudeUtility {
	
	public static final String RIGHT_ARROW = "\u27A1";
	public static final String LEFT_ARROW = "\u2B05";
	
	public static final int COLOR_ROLE_PAGE_SIZE = 25;
	
	public enum LoggerLevel {
		ERROR, WARN, INFO, DEBUG, TRACE
	}
	
	/**
	 * Convenience method which prepends a log statement with the guild's ID.
	 * @param guildLogger
	 * @param guildId
	 * @param level
	 * @param message
	 */
	public static void logWithGuildId(Logger guildLogger, Long guildId, LoggerLevel level, String message) {
		StringBuilder logMessage = new StringBuilder();
		if(guildId != null) {
			logMessage.append("[GUILD ID: ");
			logMessage.append(guildId);
			logMessage.append("] ");
		}
		logMessage.append(message);
		switch (level) {
			case ERROR:
				guildLogger.error(logMessage.toString());
				break;
			case WARN:
				guildLogger.warn(logMessage.toString());
				break;
			case INFO:
				guildLogger.info(logMessage.toString());
				break;
			case DEBUG:
				guildLogger.debug(logMessage.toString());
				break;
			case TRACE:
			default:
				guildLogger.trace(logMessage.toString());
		}
	}
	
	/**
	 * Convenience method which prepends the log statement with the guild's ID.
	 * @param guildLogger
	 * @param guildId
	 * @param level
	 * @param message
	 * @param exception
	 */
	public static void logExceptionWithGuildId(Logger guildLogger, Long guildId, LoggerLevel level, String message, Throwable exception) {
		StringBuilder logMessage = new StringBuilder();
		if(guildId != null) {
			logMessage.append("[GUILD ID: ");
			logMessage.append(guildId);
			logMessage.append("] ");
		}
		logMessage.append(message);
		
		switch (level) {
			case ERROR:
				guildLogger.error(logMessage.toString(), exception);
				break;
			case WARN:
				guildLogger.warn(logMessage.toString(), exception);
				break;
			case INFO:
				guildLogger.info(logMessage.toString(), exception);
				break;
			case DEBUG:
				guildLogger.debug(logMessage.toString(), exception);
				break;
			case TRACE:
			default:
				guildLogger.trace(logMessage.toString(), exception);
		}
	}
	
	/**
	 * Gets the formatted channel mention from an ID.
	 * @param channelId
	 * @return
	 */
	public static String getChannelMention(Long channelId) {
		return "<#" + channelId + ">";
	}
	
	/**
	 * Gets the formatted role mention from an ID.
	 * @param roleId
	 * @return
	 */
	public static String getRoleMention(Long roleId) {
		return "<@&" + roleId + ">";
	}
	
	/**
	 * Returns true if a String is null or the empty string.
	 * @param string
	 * @return
	 */
	public static boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}
	
	/**
	 * Effectively .toUpperCase(), but only for the first character.
	 * @param string
	 * @return
	 */
	public static String capitalize(String string) {
		if(string.length() > 1) {
			return string.substring(0, 1).toUpperCase() + string.substring(1);
		} else {
			return string.toUpperCase();
		}
	}
	
	/**
	 * Returns the number of (not necessarily full) partitions given a list and partition size.
	 * For example, a list of 55 items and a partition size of 25 would return 3; 2 full partitions of 25 and one .
	 * @param <T>
	 * @param partitionSize
	 * @param list
	 * @return
	 */
	public static <T> int getNumberOfPartitions(int partitionSize, List<T> list) {
		return (list.size() - 1) / partitionSize + 1;
	}
	
	/**
	 * Given a typical ID from a Dude component, parses out the page (partition) number and returns it,
	 * performing wrapping if necessary based on the number of pages.
	 * For example, clicking the previous button on the first page will result in Dude passing a page number of 0,
	 * in which case this method returns the last page. Similarly, a click on next on the last page will result in
	 * total pages + 1, for which this method returns 1.
	 * @param customId
	 * @param totalPages
	 * @return
	 */
	public static int getFinalPageNumber(String customId, int totalPages) {
		int index = Integer.valueOf(customId.split(",")[1]);
		//handle wrap-around; next on last page goes to first page and previous on first page goes to last
		if(index > totalPages) {
			index = 1;
		} else if(index < 1){
			index = totalPages;
		}
		return index;
	}
	
	/**
	 * Gets a sublist from a list based on partition size and page (partition) number.
	 * For example, for a list of 55 items, a partition size of 25, and a page number of 2, the 26th through 50th elements are returned.
	 * @param <T>
	 * @param partitionNumber
	 * @param partitionSize
	 * @param list
	 * @return
	 */
	public static <T> List<T> getSubList(int partitionNumber, int partitionSize, List<T> list) {
		return list.subList(((partitionNumber - 1) * partitionSize),
				partitionNumber == getNumberOfPartitions(partitionSize, list) ? list.size() : partitionNumber * partitionSize);
	}
	
	/**
	 * Convenience method to create a row of buttons containing the previous arrow and next arrow buttons.
	 * @param object
	 * @param currentIndex
	 * @return
	 */
	public static ActionRow createButtonScrollRow(String object, int currentIndex) {
		return ActionRow.of(
				Button.secondary(object + "," + (currentIndex - 1), ReactionEmoji.unicode(DudeUtility.LEFT_ARROW)),
				Button.secondary(object + "," + (currentIndex + 1), ReactionEmoji.unicode(DudeUtility.RIGHT_ARROW)));
	}
	
	/**
	 * Creates an embed message for a set of columns of the format:
	 * [Object]s
	 * [If columns is empty, a description mentioning so]
	 * columns.key1                  columns.key2					...
	 * columns.get(key1).get(0)      columns.get(key2).get(0)		...
	 * columns.get(key1).get(1)      columns.get(key2).get(1)		...
	 * ...
	 * 
	 * Assumes a maximum of 25 items in each field with respect to length. Callers that need more should generally implement scroll buttons.
	 * @param object
	 * @param columns
	 * @return A formatted embed spec with the given fields
	 */
	public static EmbedCreateSpec createListEmbed(String object, Map<String, List<? extends Object>> columns) {
		List<Field> fields = new ArrayList<>();
		columns.forEach((header, data) -> {
			StringBuilder columnData = new StringBuilder();
			int maxChars = (1024 - data.size()) / data.size();
			for(Object item : data) {
				//if 39 chars or less, display it, otherwise display the first 36 plus "..."
				if(item.toString().length() <= maxChars) {
					columnData.append(item);
				} else {
					columnData.append(item.toString().substring(0, maxChars - 3));
					columnData.append("...");
				}
				columnData.append('\n');
			}
			//remove last newline
			columnData.deleteCharAt(columnData.length() - 1);
			fields.add(Field.of(header, columnData.toString(), true));
		});
		EmbedCreateSpec embed = EmbedCreateSpec.create().withTitle(object + "s").withFields(fields);
		if(columns.isEmpty()) {
			embed = embed.withDescription(!columns.isEmpty() ? null : "No " + object + "s found.\nAdd some with the appropriate add command.");
		}
		validateEmbed(embed);
		return embed;
	}
	
	/**
	 * Returns whether the embed violates any of the following length restrictions as set by Discord:
	 * title: 256 chars
	 * description: 4096 chars
	 * 25 fields
	 * field name: 256 chars
	 * field value: 1024 chars
	 * total: 6000 chars
	 * Due to the structure of the Discord4J class, it's not possible to validate the footer and author length.
	 * @param embed
	 * @throws IllegalArgumentException if the embed violates Discord restrictions
	 */
	private static void validateEmbed(EmbedCreateSpec embed) throws IllegalArgumentException {
		if(embed.titleOrElse("").length() > 256) {
			throw new IllegalArgumentException("Embed title exceeds 256 characters");
		}
		if(embed.descriptionOrElse("").length() > 4096) {
			throw new IllegalArgumentException("Embed description exceeds 4096 characters");
		}
		if(embed.fields().size() > 25) {
			throw new IllegalArgumentException("Embed field amount exceeds 25");
		}
		/*
		if(embed.footer().text().length() > 2048) {
			throw new IllegalArgumentException("Embed footer exceeds 2048 characters");
		}
		if(embed.author().toString().length() > 256) {
			throw new IllegalArgumentException("Embed author exceeds 256 characters");
		}
		*/
		
		int length = 0;
		length += embed.titleOrElse("").length();
		length += embed.descriptionOrElse("").length();
		for(Field field : embed.fields()) {
			if(field.name().length() > 256) {
				throw new IllegalArgumentException("The following field name exceeded 256 characters: " + field.name());
			}
			if(field.value().length() > 1024) {
				throw new IllegalArgumentException("The following field value exceeded 1024 characters: " + field.value());
			}
			length += field.name().length();
			length += field.value().length();
		}
		//length += embed.footer().text().length();
		//length += embed.author().toString().length();
		
		if(length > 6000) {
			throw new IllegalArgumentException("Total embed length exceeded 6000 characters");
		}
	}
}
