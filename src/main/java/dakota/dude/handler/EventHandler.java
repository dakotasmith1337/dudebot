package dakota.dude.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dakota.dude.Dude;
import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.component.ComponentHandler;
import dakota.dude.handler.interaction.toplevel.TopLevelSlashCommandHandler;
import dakota.dude.model.Routine;
import dakota.dude.model.UserJoinSettings;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;
import discord4j.common.util.Snowflake;

/**
 * This class is the entry point for all events that need to be handled.
 */
public class EventHandler {
	
	private static final Logger logger = LogManager.getLogger();
	
	//routines are frequently checked so we just cache them all in memory
	private static Map<Long, Map<String, Routine>> guildRoutines = new ConcurrentHashMap<Long, Map<String, Routine>>();
	
	public static Map<String, Routine> getRoutines(Long guildId) {
		return guildRoutines.get(guildId);
	}
	
	/**
	 * Register a guild and add its data to the cache. If it is new, add it to the database with default settings.
	 * @param event The event for the guild either being registered at startup or Dude initially joining it
	 */
	public static void handle(GuildCreateEvent event) {
		Long guildId = event.getGuild().getId().asLong();
		DatabaseHandler.addGuildIfNotExists(guildId, event.getGuild().getName());
		guildRoutines.put(guildId, DatabaseHandler.getRoutines(event.getGuild().getId().asLong()));
		//check if any color roles were deleted while the bot was offline
		Set<Snowflake> roleIds = event.getGuild().getRoleIds();
		Set<Long> rolesToDelete = new HashSet<Long>();
		for(Long colorRole : DatabaseHandler.getColorRoles(guildId)) {
			if(!roleIds.contains(Snowflake.of(colorRole))) {
				rolesToDelete.add(colorRole);
			}
		}
		if(!rolesToDelete.isEmpty()) DatabaseHandler.deleteColorRoles(rolesToDelete);
		DudeUtility.logWithGuildId(logger, guildId, DudeUtility.LoggerLevel.INFO, "Registered guild");
	}
	
	/**
	 * Deletes color roles from the database if they were manually deleted by a user.
	 * @param event The event of the role deletion
	 */
	public static void handle(RoleDeleteEvent event) {
		if(DatabaseHandler.getColorRoles(event.getGuildId().asLong()).contains(event.getRoleId().asLong())) {
			DatabaseHandler.deleteColorRole(event.getRoleId().asLong());
		}
	}
	
	/**
	 * Sends a message welcoming a user that has newly joined a server.
	 * @param event The user join event
	 */
	public static void handle(MemberJoinEvent event) {
		UserJoinSettings settings = DatabaseHandler.getUserJoinSettings(event.getGuildId().asLong());
		if(!settings.getEnabled() || settings.getChannelId() == null) {
			return;
		}
		if(settings.getMention()) {
			StringBuilder joinMessage = new StringBuilder(settings.getMessage());
			joinMessage.insert(0, " ");
			joinMessage.insert(0, event.getMember().getMention());
			settings.setMessage(joinMessage.toString());
		}
		Dude.getClient().getChannelById(Snowflake.of(settings.getChannelId())).cast(MessageChannel.class).subscribe(channel -> {
			channel.createMessage(settings.getMessage()).subscribe();
			DudeUtility.logWithGuildId(logger, event.getGuildId().asLong(), DudeUtility.LoggerLevel.INFO, "Sent new user join message for user with id " + event.getMember().getId().asLong());
		});
	}
	
	/**
	 * Checks whether each message triggered a routine.
	 * @param event
	 */
	public static void handle(MessageCreateEvent event) {
		//Skip unless the message is actually sent by a non-bot user AND has text content (e.g. skip images, announcements, etc.)
		if(!event.getMessage().getType().equals(Message.Type.DEFAULT)
				|| DudeUtility.isNullOrEmpty(event.getMessage().getContent())
				|| !event.getMessage().getAuthor().isPresent()
				|| event.getMessage().getAuthor().get().isBot()) {
			return;
		}
		
		//check routines
		Long guildId = event.getGuildId().map(snowflake -> snowflake.asLong()).orElse(null);
		if(guildId != null) {
			Map<String, Routine> routines = guildRoutines.get(guildId);
			if(routines == null || routines.isEmpty()) return;
			for(String trigger : routines.keySet()) {
				Routine routine = routines.get(trigger);
				//first, check if we filter; if we don't, continue, otherwise check if the sender has the role; finally, search for the trigger
				if((!routine.getFilter() || event.getMember().get().getRoleIds().contains(Snowflake.of(routine.getFilterRoleId()))) && routine.findTrigger(event.getMessage().getContent())) {
					event.getMessage().getChannel().flatMap(channel -> channel.createMessage(routine.getResponse())).subscribe();
				}
			}
		}
	}
	
	/**
	 * Executes the necessary command handler for a slash command.
	 * @param event
	 * @return The message to be returned, if necessary.
	 */
	public static Mono<Message> handle(ChatInputInteractionEvent event) {
		return TopLevelSlashCommandHandler.getTopLevelHandler(event.getCommandName()).executeActualCommand(event);
	}
	
	/**
	 * Executes the necessary component handler for a component interaction.
	 * @param event
	 * @return The message to be returned, if necessary.
	 */
	public static Mono<Message> handle(ComponentInteractionEvent event) {
		return ComponentHandler.getComponentHandler(event.getCustomId()).execute(event);
	}
}