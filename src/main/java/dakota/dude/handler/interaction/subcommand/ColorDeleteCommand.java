package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class ColorDeleteCommand extends SlashCommandHandler {

	public ColorDeleteCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Deletes a color role.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		return event.getOption("delete").get().getOption("role").get().getValue().get().asRole().flatMap(role -> {
			if(DatabaseHandler.getColorRoles(guildId).contains(role.getId().asLong())) {
				return role.delete("Deleted by Dude as requested by user " + event.getInteraction().getUser().getUsername())
						.doOnSuccess($ -> DatabaseHandler.deleteColorRole(role.getId().asLong()))
						.then(event.createFollowup("Role successfully deleted."));
			} else {
				return event.createFollowup("This role is not a color role and cannot be deleted with this command.");
			}
		});
	}

}
