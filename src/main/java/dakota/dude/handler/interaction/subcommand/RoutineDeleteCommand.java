package dakota.dude.handler.interaction.subcommand;

import java.util.Map;

import dakota.dude.handler.EventHandler;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Routine;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class RoutineDeleteCommand extends SlashCommandHandler {

	public RoutineDeleteCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Deletes a routine.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		Map<String, Routine> routines = EventHandler.getRoutines(guildId);
		if(routines.isEmpty()) {
			return event.createFollowup("There are no routines which can be deleted.");
		} else {
			String trigger = event.getOption("delete").get().getOption("trigger").get().getValue().get().asString();
			Routine routine = routines.get(trigger);
			if(routine != null) {
				synchronized(routine.lock) {
					DatabaseHandler.deleteRoutine(guildId, routine.getTrigger());
					EventHandler.getRoutines(guildId).remove(trigger);
					return event.createFollowup("Routine deleted.");
				}
			} else {
				return event.createFollowup("No routine with that trigger found.");
			}
		}
	}

}
