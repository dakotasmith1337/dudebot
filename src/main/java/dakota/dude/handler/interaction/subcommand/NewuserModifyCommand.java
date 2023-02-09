package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.UserJoinSettings;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

public class NewuserModifyCommand extends SlashCommandHandler {

	public NewuserModifyCommand(String name, String description, String longDescription, CommandType type, Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Updates any of a guild's settings for newly joining users.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("modify").get();
		Boolean enabled = operation.getOption("enabled").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Boolean mention = operation.getOption("mention").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		String message = operation.getOption("message").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		Mono<Channel> channel = operation.getOption("channel").flatMap(option -> option.getValue()).map(value -> value.asChannel()).orElse(null);
		
		if(enabled == null && mention == null && message == null && channel == null) {
			return event.createFollowup("You must provide a value for at least one of the four parameters: enabled, mention, message, and channel.");
		}
		
		UserJoinSettings settings = DatabaseHandler.getUserJoinSettings(guildId);
		
		if(enabled != null && enabled && !settings.getEnabled()) {
			if(settings.getChannelId() == null && channel == null) {
				return event.createFollowup("You must provide a channel in which the new user messages will be sent before enabling this feature.");
			} else if(settings.getMessage() == null && message == null) {
				return event.createFollowup("You must provide a message to send to new users before enabling this feature.");
			}
		}
		
		if(enabled != null) {
			settings.setEnabled(enabled);
		}
		
		if(mention != null) {
			settings.setMention(mention);
		}
		
		if(message != null) {
			settings.setMessage(message);
		}
		
		if(channel != null) {
			return channel.flatMap(newUserChannel -> {
				settings.setChannelId(newUserChannel.getId().asLong());
				DatabaseHandler.updateUserJoinSettings(guildId, settings);
				return event.createFollowup("New user join settings successfully updated.");
			});
		} else {
			DatabaseHandler.updateUserJoinSettings(guildId, settings);
			return event.createFollowup("New user join settings successfully updated.");
		}
	}

}
