package dakota.dude;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dakota.dude.handler.EventHandler;
import dakota.dude.handler.ReminderHandler;
import dakota.dude.handler.database.DatabaseHandler;
import dakota.dude.handler.database.MySQLDatabaseHandler;
import dakota.dude.handler.interaction.model.CommandType;
import dakota.dude.handler.interaction.model.SlashCommandObject;
import dakota.dude.handler.interaction.subcommand.SlashCommandHandler;
import dakota.dude.handler.interaction.toplevel.TopLevelSlashCommandHandler;
import discord4j.common.JacksonResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import reactor.core.publisher.Mono;

public class Dude {
	
	private static final Logger logger = LogManager.getLogger();
	
	private static GatewayDiscordClient client;
	
	private static DatabaseHandler databaseHandler = new MySQLDatabaseHandler();
	
	private static final String interactionTopLevelPackage = "dakota.dude.handler.interaction.toplevel.";
	private static final String interactionSubCommandPackage = "dakota.dude.handler.interaction.subcommand.";
	
	private static Long errorChannelId;
	private static PrivateChannel errorChannel;
	private static boolean production;
	private static Long testGuildId;
	
	public static GatewayDiscordClient getClient() {
		return client;
	}
	
	public static DatabaseHandler getDatabaseHandler() {
		return databaseHandler;
	}
	
	/**
	 * Perform all the initialization work and retrieve the client token.
	 * @return clientKey The bot's client token
	 */
	static String initializeAndGetToken(String[] args) {
		//get the decryption string
		String decrypt = System.getenv("DUDE_DECRYPT");
		if(args.length < 1 && (decrypt == null || decrypt.equals("")) ) {
			String error = "No decryptor password specified, either through command line or environment variable. Launch with a password argument or set the DUDE_DECRYPT environment variable.";
			logger.error(error);
			throw new RuntimeException(error);
		}
		//prioritize the launch argument over the env variable
		if(args.length > 0) {
			decrypt = args[0];
		}
		
		//retrieve client and database connection info from properties
		StandardPBEStringEncryptor decryptor = new StandardPBEStringEncryptor();
		decryptor.setPassword(decrypt);
		Properties properties = new EncryptableProperties(decryptor);
		
		String clientKey = null;
		try {
			properties.load(Dude.class.getClassLoader().getResourceAsStream("dude.properties"));
			clientKey = properties.getProperty("dude.key");
			if(clientKey == null) {
				throw new MissingResourceException("The property 'dude.key' was not found in dude.properties", "Dude", "dude.key");
			}
		} catch(Exception error) {
			logger.error(error);
			throw new RuntimeException(error);
		}
		
		//initialize database connection, cease execution on failure
		try {
			String databaseServer = properties.getProperty("database.server");
			String databaseName = properties.getProperty("database.name");
			String databaseUsername = properties.getProperty("database.username");
			String databasePassword = properties.getProperty("database.password");
			if(DudeUtility.isNullOrEmpty(databaseServer) || DudeUtility.isNullOrEmpty(databaseUsername) || DudeUtility.isNullOrEmpty(databasePassword)) {
				throw new MissingResourceException("One of the 'database.*' properties, aside from database.name, is blank in dude.properties", "Dude", "database.*");
			}
			//Oracle doesn't use database names, so only throw a warning in this case
			if(DudeUtility.isNullOrEmpty(databaseName)) {
				logger.warn("database.name property is null! If the target database isn't Oracle, this will likely cause an error");
			}
			
			MySQLDatabaseHandler.initialize(databaseServer, databaseName, databaseUsername, databasePassword);
		} catch(Exception error) {
			throw new RuntimeException(error);
		}
		
		//initialize error-reporting channel, if specified
		try {
			String errorChannel = properties.getProperty("error.channel");
			logger.debug("errorChannel: " + errorChannel);
			if(DudeUtility.isNullOrEmpty(errorChannel)) {
				logger.warn("Error channel not specified. Errors will only be logged");
			} else {
				errorChannelId = Long.valueOf(errorChannel);
			}
		} catch(NumberFormatException e) {
			logger.warn("Value provided for error.channel did not parse as a Long, errors will only be logged");
		}
		
		//determine whether this is dev or prod, and hence whether to deploy commands to the test guild or globally
		production = Boolean.parseBoolean(properties.getProperty("production.environment"));
		logger.info("Production environment: " + production);
		
		try {
			String testGuild = properties.getProperty("test.guild");
			logger.debug("testGuild: " + testGuild);
			if(!production && DudeUtility.isNullOrEmpty(testGuild)) {
				throw new MissingResourceException("production.environment was false and test.guild was not provided, commands cannot be published", "Dude", "test.guild");
			} else {
				testGuildId = Long.valueOf(testGuild);
			}
		} catch(NumberFormatException e) {
			logger.warn("Value provided for test guild ID did not parse as a Long, commands will not be published");
		}
		
		return clientKey;
	}
	
	/**
	 * Connects to Discord and runs until given signal to exit by application or system.
	 * @throws IOException 
	 */
	private static void connect(String clientKey) {
		//connect the client and route events to event handlers
		DiscordClient discordClient = DiscordClientBuilder.create(clientKey).build();
		discordClient.gateway().setInitialPresence(status -> ClientPresence.online(
				production ? ClientActivity.listening("some Creedence") : ClientActivity.watching("Dude bowl")
		)).setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.DIRECT_MESSAGES)).withGateway(gateway -> {
			client = gateway;
			
			//initialize PM channel for automatic error reporting
			if(errorChannelId != null) {
				client.getChannelById(Snowflake.of(errorChannelId)).subscribe(channel -> {
					if(channel instanceof PrivateChannel) {
						errorChannel = (PrivateChannel) channel;
						logger.info("Initialized author PM channel");
					} else {
						logger.warn("Supplied error PM channel couldn't be cast to PrivateChannel, type is {}", channel.getClass().getSimpleName());
					}
				}, error -> {
					logger.error("Error retrieving the error PM channel: {}", error);
				});
			}
			
			//publish commands
			try {
				if(production) registerCommands(client.getRestClient(), null);
				else registerCommands(client.getRestClient(), testGuildId);
				logger.info("Finished registering commands");
			} catch(Exception error) {
				logger.error("Error while publishing commands: ", error);
				//consider this error unrecoverable and exit
				shutdown();
				return Mono.empty();
			}
			
			TopLevelSlashCommandHandler.initializeAllTopLevelCommands();
			
			EventDispatcher dispatcher = client.getEventDispatcher();
			Mono<Void> onGuildJoin = dispatcher.on(GuildCreateEvent.class)
				.doOnNext(event -> {
					try {
						EventHandler.handle(event);
					} catch(Exception error) {
						logHandlerError(event, event.getGuild().getId().asLong(), error);
					}
				})
				.then();
			
			Mono<Void> onRoleDelete = dispatcher.on(RoleDeleteEvent.class)
				.doOnNext(event -> {
					try {
						EventHandler.handle(event);
					} catch(Exception error) {
						logHandlerError(event, event.getGuildId().asLong(), error);
					}
				})
				.then();
			
			Mono<Void> onMemberJoin = dispatcher.on(MemberJoinEvent.class)
				.doOnNext(event -> {
					try {
						EventHandler.handle(event);
					} catch(Exception error) {
						logHandlerError(event, event.getGuildId().asLong(), error);
					}
				})
				.then();
			
			Mono<Void> onMessageCreate = dispatcher.on(MessageCreateEvent.class)
				.doOnNext(event -> {
					try {
						EventHandler.handle(event);
					} catch(Exception error) {
						logHandlerError(event, event.getGuildId().map(id -> id.asLong()).orElse(null), error);
					}
				})
				.then();
			
			Mono<Void> onCommandInteraction = dispatcher.on(ChatInputInteractionEvent.class)
				.doOnNext(event -> {
					try {
						event.deferReply().then(EventHandler.handle(event)).doOnError(error -> logger.error(error)).subscribe();
					} catch(Exception error) {
						logHandlerError(event, event.getInteraction().getGuildId().map(id -> id.asLong()).orElse(null), error);
					}
				})
				.then();
			
			Mono<Void> onComponentInteraction = dispatcher.on(ComponentInteractionEvent.class)
				.doOnNext(event -> {
					try {
						event.deferEdit().then(EventHandler.handle(event)).subscribe();
					} catch(Exception error) {
						logHandlerError(event, event.getInteraction().getGuildId().map(id -> id.asLong()).orElse(null), error);
					}
				})
				.then();
			
			System.out.println("Successfully connected the Discord client.");
			logger.info("Successfully connected the Discord client.");
			return Mono.when(Arrays.asList(onGuildJoin, onRoleDelete, onMemberJoin, onMessageCreate, onCommandInteraction, onComponentInteraction));
		}).subscribe();
		
		//from here on out, process reminders, but stop once given a shutdown signal
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("Signal given to shut down");
				shutdown();
			}
		});
		
		//don't move on until the discord connection is initialized
		while(client == null) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException error) {
				logger.warn("Reminder thread was interrupted:\n", error);
			}
		}
		
		//loop handling reminders until shutdown
		ReminderHandler.handle();
		
		logger.info("Disconnecting from the database");
		MySQLDatabaseHandler.disconnect();
		
		logger.info("Logging out");
		client.logout().block();
		
		logger.info("Logged out, exiting");
	}
	
	/**
	 * Generic top-level error logger. If enabled, also sends a Discord DM to the user designated as the author.
	 * @param event
	 * @param guildId
	 * @param error
	 */
	private static void logHandlerError(Event event, Long guildId, Exception error) {
		StringBuilder errorReport = new StringBuilder(error.getClass().getName());
		errorReport.append(" exception handling a ");
		errorReport.append(event.getClass().getSimpleName());
	
		if(guildId != null) {
			DudeUtility.logExceptionWithGuildId(logger, guildId, DudeUtility.LoggerLevel.ERROR, errorReport.toString(), error);
		} else {
			logger.error(errorReport.toString(), error);
		}
		
		if(errorChannel != null) {
			errorChannel.createMessage(errorReport.toString()).subscribe($ -> {
				logger.info("Successfully reported an error to author");
			}, e -> {
				logger.error("Couldn't DM author an error message", e);
			});
		}
	}

	/**
	 * For a given list of commands, reads them from JSON files into D4J command requests then publishes them to Discord.
	 * Source is <a href="https://github.com/Discord4J/example-projects/blob/master/gradle-simple-bot/src/main/java/com/novamaday/d4j/gradle/simplebot/GlobalCommandRegistrar.java">here</a>.
	 * 
	 * @param fileNames
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	private static void registerCommands(RestClient restClient, Long guildId) throws IOException, URISyntaxException {
		// Confirm that the commands folder exists
		URL url = DudeUtility.class.getClassLoader().getResource("commands/");
		Objects.requireNonNull(url, "the commands resource folder could not be found");
		List<File> files = Files.walk(Paths.get(url.toURI())).filter(Files::isRegularFile).map(file -> file.toFile()).collect(Collectors.toList());

		//Get all the files inside this folder and parse the contents of the files as a list of commands
		ObjectMapper mapper = JacksonResources.create().getObjectMapper();
		List<ApplicationCommandRequest> commands = new ArrayList<>();
		for (File file : files) {
			String json = null;
			try (InputStream fileAsStream = new FileInputStream(file)) {
				if(fileAsStream != null) {
					try (InputStreamReader inputStreamReader = new InputStreamReader(fileAsStream);
						 BufferedReader reader = new BufferedReader(inputStreamReader)) {
						json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
					}
				}
			}
			
			ApplicationCommandRequest request = mapper.readValue(json, ApplicationCommandRequest.class);
			commands.add(request);
			
			//each top level command is effectively the root node of a tree of subcommands and arguments
			initializeInteractionObjectAndChildren(JsonParser.parseString(json).getAsJsonObject(), "", false);
			
			logger.info("Loaded json info for file: " + file.getName());
		}
		
		//publish the command requests either globally or to a test server
		if(production) {
			restClient.getApplicationId().flatMapMany(applicationId -> restClient.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, commands))
				.doOnNext(cmd -> logger.debug("Successfully registered global command " + cmd.name()))
				.doOnError(e -> logger.error("Failed to register global commands", e))
				.subscribe();
		} else {
			restClient.getApplicationId().flatMapMany(applicationId -> restClient.getApplicationService().bulkOverwriteGuildApplicationCommand(applicationId, guildId, commands))
			.doOnNext(cmd -> logger.debug("Successfully registered guild command " + cmd.name()))
			.doOnError(e -> logger.error("Failed to register guild commands", e))
			.subscribe();
		}
	}
	
	/**
	 * Deserializes an object and its children from command JSON files into a SlashCommandObject or appropriate subclass.
	 * @param object
	 * @param parentName
	 * @param parentGlobal
	 * @return the object as a SlashCommandObject, SlashCommandHandler, or TopLevelSlashCommandHandler
	 */
	private static SlashCommandObject initializeInteractionObjectAndChildren(JsonObject object, String parentName, boolean parentGlobal) {
		//deserialize fields
		CommandType type = object.get("type") != null ? CommandType.getById(object.get("type").getAsInt()) : null;
		String name = object.get("name").getAsString();
		String description = object.get("description").getAsString();
		String longDescription = object.get("long_description") != null ? object.get("long_description").getAsString() : null;
		JsonArray options = object.get("options") != null ? object.get("options").getAsJsonArray() : null;
		
		//default to the top level command's setting; useful granularity for partially DM-usable commands like reminder
		//without forcing us to supply the value to every single object in the json model
		JsonElement isGlobal = object.get("dm_permission");
		boolean global;
		if(isGlobal == null) {
			global = parentGlobal;
		} else {
			global = isGlobal.getAsBoolean();
		}
		
		Map<String, SlashCommandObject> children = new HashMap<>();
		//recursively deserialize child objects
		if(options != null) {
			for(JsonElement option : options) {
				SlashCommandObject interactionObject = initializeInteractionObjectAndChildren(option.getAsJsonObject(), parentName + DudeUtility.capitalize(name), global);
				children.put(interactionObject.getName(), interactionObject);
			}
		}
		
		//commands have specific classes, so use reflection to get and construct those;
		//otherwise for groups and arguments the base InteractionObject is sufficient
		if(type == null || type.equals(CommandType.COMMAND)) {
			String className = (type == null ? interactionTopLevelPackage : interactionSubCommandPackage)
					+ parentName + DudeUtility.capitalize(name)
					+ "Command";
			try {
				return (SlashCommandHandler) Class.forName(className)
					.getConstructor(String.class, String.class, String.class, CommandType.class, Map.class, Boolean.TYPE)
					.newInstance(name, description, longDescription, CommandType.COMMAND, children, global);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException error) {
				//technically recoverable if we ignore the exception since the command will just be unused,
				//but we'd always rather know and fix it immediately, so just break
				throw new RuntimeException(error);
			}
		} else {
			return new SlashCommandObject(name, description, longDescription, type, children, global);
		}
	}
	
	/**
	 * Abstracts whatever actions are necessary to ensure that Dude begins the shutdown process.
	 */
	private static void shutdown() {
		ReminderHandler.shutdown();
	}
	
	/**
	 * Initializes the bot, connects, then becomes the reminder processing thread until given a shutdown signal.
	 * @param args Command line args
	 */
	public static void main(String[] args) {
		connect(initializeAndGetToken(args));
	}
}