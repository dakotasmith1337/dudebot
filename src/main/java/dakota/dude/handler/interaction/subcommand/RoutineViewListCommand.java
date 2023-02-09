package dakota.dude.handler.interaction.subcommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.EventHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Routine;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class RoutineViewListCommand extends SlashCommandHandler {

	public RoutineViewListCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays the list of routines.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		Map<String, Routine> routinesToView = EventHandler.getRoutines(guildId);
		if(routinesToView.isEmpty()) {
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
				EmbedCreateSpec.create().withTitle("Routines").withDescription("No routines found.\nAdd some with /routine add."))
			);
		}
		//ensure the triggers and responses stay ordered
		List<String> triggers = new ArrayList<>();
		List<String> responses = new ArrayList<>();
		for(Map.Entry<String, Routine> entry : EventHandler.getRoutines(guildId).entrySet()) {
			triggers.add(entry.getKey());
			responses.add(entry.getValue().getResponse());
		}
		Map<String, List<? extends Object>> columns = new LinkedHashMap<>();
		columns.put("Trigger", triggers);
		columns.put("Response", responses);
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
			DudeUtility.createListEmbed("Routine", columns)
		));
	}

}
