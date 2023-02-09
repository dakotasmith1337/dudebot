package dakota.dude.handler.interaction.subcommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class ColorSetCommand extends SlashCommandHandler {

	public ColorSetCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Gives the user that triggered this command the desired color role.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		Optional<ApplicationCommandInteractionOptionValue> colorRole = event.getOption("set").get().getOption("role").flatMap(option -> option.getValue());
		if(colorRole.isPresent()) {
			return colorRole.get().asRole().flatMap(role -> {
				if(DatabaseHandler.colorRoleExists(guildId, role.getId().asLong())) {
					Member member = event.getInteraction().getMember().get();
					List<Mono<Void>> operations = new ArrayList<>();
					
					//remove old role(s), if they exist
					for(Long colorRoleId : DatabaseHandler.getColorRoles(guildId)) {
						if(member.getRoleIds().contains(Snowflake.of(colorRoleId)) && !colorRoleId.equals(role.getId().asLong())) {
							operations.add(member.removeRole(Snowflake.of(colorRoleId), "Removed by Dude for color change request"));
						}
					}
					
					//add new role
					operations.add(member.addRole(role.getId(), "Added by Dude for color change request"));
					
					return Mono.when(operations).then(event.createFollowup("Role successfully applied."));
				} else {
					return event.createFollowup("This role is not a color role.");
				}
			});
		} else {
			//take no role selected to mean remove old role(s), if they exist
			List<Mono<Void>> operations = new ArrayList<>();
			for(Long colorRoleId : DatabaseHandler.getColorRoles(guildId)) {
				if(event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(colorRoleId))) {
					operations.add(event.getInteraction().getMember().get().removeRole(Snowflake.of(colorRoleId), "Removed by Dude for color change request"));
				}
			}
			return Mono.when(operations).then(event.createFollowup("Color role successfully removed."));
		}
	}

}
