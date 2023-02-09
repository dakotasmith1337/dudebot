package dakota.dude.handler.interaction.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.interaction.subcommand.ColorViewCommand;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionReplyEditSpec;
import reactor.core.publisher.Mono;

public class ColorComponentHandler extends ComponentHandler {
	
	private static final String objectName = "Color";

	public ColorComponentHandler() {
		super(objectName);
	}

	/**
	 * Refreshes the color role list message with a new page of color roles, based on the user clicking next or previous.
	 */
	@Override
	public Mono<Message> execute(ComponentInteractionEvent event) {
		List<String> colorRoles = ColorViewCommand.getGuildColorRoles(event.getInteraction().getMessageId().get().asLong());
		if(colorRoles != null) {
			int totalPages = DudeUtility.getNumberOfPartitions(DudeUtility.COLOR_ROLE_PAGE_SIZE, colorRoles);
			
			final int finalPage = DudeUtility.getFinalPageNumber(event.getCustomId(), totalPages);
			
			List<String> pageRoles = DudeUtility.getSubList(finalPage, DudeUtility.COLOR_ROLE_PAGE_SIZE, colorRoles);
			
			Map<String, List<? extends Object>> columns = new LinkedHashMap<>();
			columns.put("Role", pageRoles);
			return event.editReply(InteractionReplyEditSpec.create().withEmbeds(DudeUtility.createListEmbed(objectName, columns)).withComponents(DudeUtility.createButtonScrollRow(objectName, finalPage)));
		} else {
			return Mono.empty();
		}
	}

}
