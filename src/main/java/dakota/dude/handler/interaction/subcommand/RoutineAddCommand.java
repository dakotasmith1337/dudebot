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

public class RoutineAddCommand extends SlashCommandHandler {

	public RoutineAddCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Adds a new routine.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		ApplicationCommandInteractionOption operation = event.getOption("add").get();
		String trigger = operation.getOption("trigger").get().getValue().get().asString();
		String response = operation.getOption("response").get().getValue().get().asString();
		String findSetting = operation.getOption("findsetting").get().getValue().get().asString();
		boolean caseSensitive = operation.getOption("casesensitive").get().getValue().get().asBoolean();
		Mono<Role> filterRole = operation.getOption("filterrole").flatMap(option -> option.getValue()).map(value -> value.asRole()).orElse(null);
		Map<String, Routine> guildRoutines = EventHandler.getRoutines(guildId);
		
		if(guildRoutines.size() >= 25) {
			return event.createFollowup("The guild may not have more than 25 routines.");
		} else if(trigger.length() > 768 && response.length() > 768) {
			return event.createFollowup("Your trigger and response are too long, the maximum length for each is 768 characters.");
		} else if (trigger.length() > 768) {
			return event.createFollowup("That trigger is too long, the maximum length is 768 characters.");
		} else if (response.length() > 768) {
			return event.createFollowup("That response is too long, the maximum length is 768 characters.");
		} else if(Routine.containsValidation(trigger, guildRoutines.keySet())) {
			return event.createFollowup("An existing routine has a trigger that is either equal to this trigger, contains it, or is contained within it (this validation is *not* case sensitive).");
		}

		if(filterRole != null) {
			return filterRole.flatMap(role -> {
				Routine newRoutine = new Routine(trigger, response, RoutineFindSetting.valueOf(findSetting), caseSensitive, true, role.getId().asLong());
				DatabaseHandler.addRoutine(guildId, newRoutine);
				guildRoutines.put(trigger, newRoutine);
				return event.createFollowup("Routine successfully created.");
			});
		} else {
			Routine newRoutine = new Routine(trigger, response, RoutineFindSetting.valueOf(findSetting), caseSensitive, false, null);
			DatabaseHandler.addRoutine(guildId, newRoutine);
			guildRoutines.put(trigger, newRoutine);
			return event.createFollowup("Routine successfully created.");
		}
	}

}
