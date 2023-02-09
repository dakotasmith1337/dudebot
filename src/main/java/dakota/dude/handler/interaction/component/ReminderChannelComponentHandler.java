package dakota.dude.handler.interaction.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dakota.dude.DudeUtility;
import dakota.dude.handler.interaction.subcommand.ReminderChannelViewCommand;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionReplyEditSpec;
import reactor.core.publisher.Mono;

public class ReminderChannelComponentHandler extends ComponentHandler {
	
	private static final String objectName = "ReminderChannel";

	public ReminderChannelComponentHandler() {
		super(objectName);
	}

	/**
	 * Refreshes the reminder channel list message with a new page of reminder channels, based on the user clicking next or previous.
	 */
	@Override
	public Mono<Message> execute(ComponentInteractionEvent event) {
		List<String> reminderChannels = ReminderChannelViewCommand.getGuildReminderChannels(event.getInteraction().getMessageId().get().asLong());
		if(reminderChannels != null) {
			int totalPages = DudeUtility.getNumberOfPartitions(DudeUtility.COLOR_ROLE_PAGE_SIZE, reminderChannels);
			
			final int finalPage = DudeUtility.getFinalPageNumber(event.getCustomId(), totalPages);
			
			List<String> pageChannels = DudeUtility.getSubList(finalPage, DudeUtility.COLOR_ROLE_PAGE_SIZE, reminderChannels);
			
			Map<String, List<? extends Object>> columns = new LinkedHashMap<>();
			columns.put("Channel", pageChannels);
			return event.editReply(InteractionReplyEditSpec.create().withEmbeds(DudeUtility.createListEmbed(objectName, columns)).withComponents(DudeUtility.createButtonScrollRow(objectName, finalPage)));
		} else {
			return Mono.empty();
		}
	}

}
