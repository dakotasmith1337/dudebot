package dakota.dude.handler.interaction.subcommand;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dakota.dude.Dude;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class ColorAddCommand extends SlashCommandHandler {

	public ColorAddCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Adds a new color role.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		String name = event.getOption("add").get().getOption("name").get().getValue().get().asString();
		//cast safety handled by max_value in command definition
		int red = (int) event.getOption("add").get().getOption("red").get().getValue().get().asLong();
		int green = (int) event.getOption("add").get().getOption("green").get().getValue().get().asLong();
		int blue = (int) event.getOption("add").get().getOption("blue").get().getValue().get().asLong();
		
		Color color = Color.of(red, green, blue);
		
		return Dude.getClient().getGuildById(Snowflake.of(guildId)).flatMap(guild -> {
			//too many roles in the guild
			if(guild.getRoleIds().size() >= 250) {
				return event.createFollowup("There are too many roles in the guild, can't create another.");
			}
			
			return guild.getRoles().collectList().flatMap(roles -> {
				//map of guild role IDs to their names
				Map<Long, Role> guildRoles = roles.stream().collect(Collectors.toMap(role -> ((Role)role).getId().asLong(), role -> role));
				List<Long> colorRoleIds = DatabaseHandler.getColorRoles(guild.getId().asLong());
				for(Long colorRoleId : colorRoleIds) {
					//just more state maintenance for safety's sake
					if(!guildRoles.containsKey(colorRoleId)) {
						DatabaseHandler.deleteColorRole(colorRoleId);
						continue;
					}
					
					//name already taken
					if(guildRoles.get(colorRoleId).getName().equals(name)) {
						return event.createFollowup("A role already exists with this name.");
					}
					
					//color already taken
					if(guildRoles.get(colorRoleId).getColor().equals(color)) {
						StringBuilder alreadyExists = new StringBuilder("A color role named ");
						alreadyExists.append(guildRoles.get(colorRoleId).getName());
						alreadyExists.append(" already exists with this color.");
						return event.createFollowup(alreadyExists.toString());
					}
				}
				
				//finally, create it
				return guild.createRole(RoleCreateSpec.create().withColor(color).withName(name).withReason("Created by Dude as a color role")).flatMap(role -> {
					DatabaseHandler.addColorRole(guild.getId().asLong(), role.getId().asLong());
					return event.createFollowup("Role successfully created.");
				});
			});
		});
	}

}
