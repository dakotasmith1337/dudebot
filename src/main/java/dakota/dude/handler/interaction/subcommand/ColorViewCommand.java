package dakota.dude.handler.interaction.subcommand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dakota.dude.DudeUtility;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.interaction.model.CacheMap;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class ColorViewCommand extends SlashCommandHandler {
	
	private static CacheMap<List<String>> guildColorRoles = new CacheMap<>();

	public ColorViewCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}
	
	public static List<String> getGuildColorRoles(Long messageId) {
		return guildColorRoles.get(messageId);
	}

	/**
	 * Displays the list of color roles.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		Long guildId = event.getInteraction().getGuildId().get().asLong();
		List<String> colorRoles = DatabaseHandler.getColorRoles(guildId).stream().map(DudeUtility::getRoleMention).collect(Collectors.toList());
		if(colorRoles.isEmpty()) {
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
				EmbedCreateSpec.create().withTitle("Color Roles").withDescription("No color roles found.\nAdd some with /color add."))
			);
		}
		
		LinkedHashMap<String, List<? extends Object>> columns = new LinkedHashMap<>();
		columns.put("Role", colorRoles.size() > 25 ? colorRoles.subList(0, 25) : colorRoles);
		
		if(colorRoles.size() > 25) {
			return event.createFollowup().withEmbeds(DudeUtility.createListEmbed("Color", columns)).withComponents(DudeUtility.createButtonScrollRow("Color", 1))
					.doOnNext(message -> guildColorRoles.put(message.getId().asLong(), colorRoles));
		} else {
			return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(DudeUtility.createListEmbed("Color", columns)));
		}
	}
}
