package dakota.dude.handler.interaction.subcommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.EventHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.model.Routine;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class RoutineViewOneCommand extends SlashCommandHandler {

	public RoutineViewOneCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Displays detailed information for one routine.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		String trigger = event.getOption("view").get().getOption("one").get().getOption("trigger").get().getValue().get().asString();
		Routine routine = EventHandler.getRoutines(guildId).get(trigger);
		if(routine != null) {
			String filterRoleName = routine.getFilterRoleId() != null ? DudeUtility.getRoleMention(routine.getFilterRoleId()) : null;
			List<EmbedCreateFields.Field> fields = new ArrayList<>();
			fields.add(EmbedCreateFields.Field.of("Trigger", routine.getTrigger(), false));
			fields.add(EmbedCreateFields.Field.of("Response", routine.getResponse(), false));
			fields.add(EmbedCreateFields.Field.of("Find Setting", routine.getFindSetting().capitalized(), true));
			fields.add(EmbedCreateFields.Field.of("Case Sensitive", routine.getCaseSensitive().toString(), true));
			fields.add(EmbedCreateFields.Field.of("Filter", routine.getFilter().toString(), true));
			if(filterRoleName != null) {
				fields.add(EmbedCreateFields.Field.of("Filter Role", filterRoleName, true));
			}
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
				EmbedCreateSpec.create().withTitle("Routine").withFields(fields)));
		} else {
			return event.createFollowup("No routine found with that trigger.");
		}
	}

}
