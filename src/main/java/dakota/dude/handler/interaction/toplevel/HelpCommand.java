package dakota.dude.handler.interaction.toplevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dakota.dude.DudeUtility;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class HelpCommand extends TopLevelSlashCommandHandler {
	
	private static String topLevelCommandInfo = null;
	
	public HelpCommand(String name, String description, String longDescription, CommandType type,
			Map<String, SlashCommandObject> children, boolean global) {
		super(name, description, longDescription, type, children, global);
	}

	/**
	 * Programmatically displays information about any command (top-level or otherwise) or parameter, given the path to that object.
	 * The base command displays general information about Dude and the list of top-level commands.
	 */
	@Override
	public Mono<Message> execute(ChatInputInteractionEvent event) {
		ApplicationCommandInteractionOption arg = event.getOption("command").orElse(null);
		
		String title;
		String description;
		List<Field> fields = new ArrayList<>();
		
		if(arg != null) {
			String path = arg.getValue().get().asString();
			List<String> commandPath = Arrays.asList(path.split(" "));
			SlashCommandObject object = topLevelInteractions.get(commandPath.get(0));
			if(object != null) {
				for(int i = 1; i < commandPath.size(); i++) {
					boolean isArgument = !object.getType().equals(CommandType.COMMAND) && !object.getType().equals(CommandType.GROUP);
					object = object.getOptions().get(commandPath.get(i));
					if(object == null) {
						return event.createFollowup(isArgument
								? commandPath.get(i-1) + " is an argument, it does not have any subcommands or arguments."
								: "The " + commandPath.get(i-1) + " command doesn't have a subcommand or argument named " + commandPath.get(i) + ".");
					}
				}
			} else {
				return event.createFollowup("There is no " + commandPath.get(0) + " command.");
			}
			
			title = "/" + path;
			description = object.getFullDescription();
			
			if(object.getType().equals(CommandType.COMMAND)){
				fields.add(Field.of("Usable in DMs", String.valueOf(isGlobal()), false));
			}
			
			StringBuilder subcommands = new StringBuilder();
			StringBuilder args = new StringBuilder();
			
			for(SlashCommandObject child : object.getOptions().values().stream().sorted().collect(Collectors.toList())) {
				if(child.getType().equals(CommandType.COMMAND) || child.getType().equals(CommandType.GROUP)) {
					appendCommandInfo(subcommands, child, true);
				} else {
					appendCommandInfo(args, child, true);
				}
			}
			fields.add(Field.of("Arguments", args.length() > 0 ? args.toString() : "N/A", false));
			fields.add(Field.of("Subcommands", subcommands.length() > 0 ? subcommands.toString() : "N/A", false));
			
		} else {
			title = getName();
			description = getFullDescription();
			fields.add(Field.of("Top Level Commands", topLevelCommandInfo, false));
		}
		
		return event.createFollowup(InteractionFollowupCreateSpec.create().withEmbeds(
				EmbedCreateSpec.create().withTitle(title).withDescription(description).withFields(fields)));
	}
	
	private static void appendCommandInfo(StringBuilder sb, SlashCommandObject child, boolean includeType) {
		sb.append("**");
		sb.append(child.getName());
		sb.append("**");
		if(includeType) {
			sb.append(" (");
			sb.append(DudeUtility.capitalize(child.getType().toString()));
			sb.append(")");
		}
		sb.append(": ");
		sb.append(child.getDescription());
		sb.append("\n");
	}
	
	@Override
	protected void initialize() {
		StringBuilder message = new StringBuilder();
		for(TopLevelSlashCommandHandler topLevel : topLevelInteractions.values().stream().sorted().collect(Collectors.toList())) {
			appendCommandInfo(message, topLevel, false);
		}
		topLevelCommandInfo = message.toString();
	}
}
