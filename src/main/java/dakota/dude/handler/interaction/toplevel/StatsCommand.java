package dakota.dude.handler.interaction.toplevel;

import java.util.Map;

import dakota.dude.Dude;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public class StatsCommand extends TopLevelSlashCommandHandler {

	public StatsCommand(String name, String description, String longDescription, CommandType type, Map<String, SlashCommandObject> options, boolean global) {
		super(name, description, longDescription, type, options, global);
	}

	/**
	 * Returns diagnostic information. Currently just displays the number of servers and shards.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		return Mono.zip(Dude.getClient().getGuilds().count(), Dude.getClient().getGatewayResources().getShardCoordinator().getConnectedCount(),
				(guildCount, shardCount) -> "Serving " + guildCount + " servers on " + shardCount + " shards.").flatMap(message -> event.createFollowup(message));
	}
}
