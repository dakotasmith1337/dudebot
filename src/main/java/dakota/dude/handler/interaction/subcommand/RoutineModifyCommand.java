package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.EventHandler;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Routine;
import dakota.dude.model.RoutineFindSetting;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

public class RoutineModifyCommand extends SlashCommandHandler {

	public RoutineModifyCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Updates any of the values for a routine.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("modify").get();
		String existingTrigger = operation.getOption("existingtrigger").flatMap(option -> option.getValue()).get().asString();
		Routine routine = EventHandler.getRoutines(guildId).get(existingTrigger);
		if(routine == null) {
			return event.createFollowup("No routine found with that trigger.");
		}
		
		String trigger = operation.getOption("trigger").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		String response = operation.getOption("response").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		String findSetting = operation.getOption("findsetting").flatMap(option -> option.getValue()).map(value -> value.asString()).orElse(null);
		Boolean caseSensitive = operation.getOption("casesensitive").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Boolean filter = operation.getOption("filter").flatMap(option -> option.getValue()).map(value -> value.asBoolean()).orElse(null);
		Mono<Role> filterRole = operation.getOption("filterrole").flatMap(option -> option.getValue()).map(value -> value.asRole()).orElse(null);
		
		if(trigger == null && response == null && findSetting == null && caseSensitive == null && filter == null && filterRole == null) {
			return event.createFollowup("You must provide a new value for at least one of the six properties (trigger, response, find setting, case sensitive, filter, filter role).");
		} else if(filter != null && filter && filterRole == null && routine.getFilterRoleId() == null) {
			return event.createFollowup("You must add a role to filter on before or in the same command as when you enable filtering.");
		}
		
		//complicated mono magic we have to do to implement 3 cases: -1 for no new role provided (keep the old role), 0 for @everyone (make the role null), or an actual new role id
		Mono<Long> newFilterRoleId = filterRole != null ? filterRole.flatMap(role -> {
				return role.getGuild().flatMap(guild -> {
					return guild.getEveryoneRole().map(everyone -> {
						if(role.getId().equals(everyone.getId())) {
							return 0L;
						} else {
							return role.getId().asLong();
						}
					});
				});
			})
			: Mono.just(-1L);
		
		return newFilterRoleId.flatMap(filterRoleId -> {
			synchronized(routine.lock) {
				if(filterRoleId == -1L) {
					//keep the same
				} else if(filterRoleId == 0L) {
					//unset
					routine.setFilterRoleId(null);
				} else {
					//set
					routine.setFilterRoleId(filterRoleId);
				}
				
				if(trigger != null) routine.setTrigger(trigger);
				if(response != null) routine.setResponse(response);
				if(findSetting != null) routine.setFindSetting(RoutineFindSetting.valueOf(findSetting));
				if(caseSensitive != null) routine.setCaseSensitive(caseSensitive);
				if(filter != null) routine.setFilter(filter);
				DatabaseHandler.modifyRoutine(guildId, existingTrigger, routine);
				return event.createFollowup("Routine successfully updated.");
			}
		});
	}

}
