package dakota.dude.handler.interaction.component;

import java.util.HashMap;
import java.util.Map;

import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public abstract class ComponentHandler {

	private static Map<String, ComponentHandler> componentHandlers = new HashMap<>();
	
	/**
	 * Places this new ComponentHandler into a map maintaining all ComponentHandler instances.
	 * @param key 
	 */
	protected ComponentHandler(String key) {
		componentHandlers.put(key, this);
	}
	
	/**
	 * Defines the actions a handler should take when its particular event is received.
	 * @param event
	 * @return the message to send in response if needed, an empty Mono otherwise
	 */
	public abstract Mono<Message> execute(ComponentInteractionEvent event);
	
	/**
	 * Gets a particular component handler class by its key.
	 * This method will filter anything past the first comma, inclusive, out of the key so that components whose custom IDs contain information
	 * such as page number can safely just pass the entire ID (e.g. "Color,2" will be read as "Color").
	 * @param key The portion of the component handler class's name which precedes "ComponentHandler"
	 * @return
	 */
	public static ComponentHandler getComponentHandler(String key) {
		//some components use ',' as a delimiter to add arguments, e.g. page number for embed scroll buttons, e.g. "Color,<next page number>"
		return componentHandlers.get(key.split(",")[0]);
	}
	
	static {
		new ColorComponentHandler();
		new ReminderChannelComponentHandler();
	}
}
