package dakota.dude.handler.database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;

import dakota.dude.Dude;
import dakota.dude.model.Reminder;
import dakota.dude.model.ReminderSettings;
import dakota.dude.model.Routine;
import dakota.dude.model.RoutineFindSetting;
import dakota.dude.model.UserJoinSettings;

/**
 * This class defines the database operations each database vendor class needs to implement.
 * Since almost all queries will be the same, default implementations are provided for each operation.
 * If a particular vendor is incompatible with a certain implementation or offers a more optimal solution,
 * subclasses can override the query strings and/or methods to use an alternative implementation.
 *
 */
public abstract class DatabaseHandler {
	
	protected static final Logger logger = LogManager.getLogger();
	
	protected static HikariPool connectionPool;
	
	protected static ThreadLocal<Connection> connection = new ThreadLocal<Connection>();
	
	protected static String createScript = "";
	protected static String databaseName = null;
	
	protected static Map<String, String> queries = new HashMap<>();

	protected static final String ADD_GUILD_IF_NOT_EXISTS = "addGuildIfNotExists";
	protected static final String ADD_USER_JOIN_SETTING_IF_NOT_EXISTS = "addUserJoinSettingIfNotExists";
	protected static final String ADD_REMINDER_SETTING_IF_NOT_EXISTS = "addReminderSettingIfNotExists";
	protected static final String GET_USER_JOIN_SETTINGS = "getUserJoinSettings";
	protected static final String UPDATE_USER_JOIN_SETTINGS = "updateUserJoinSettings";
	protected static final String GET_REMINDER_SETTINGS = "getReminderSettings";
	protected static final String SET_REMINDER_SETTINGS = "setReminderSettings";
	protected static final String SET_RESTRICT_REMINDERS = "setRestrictReminders";
	protected static final String SET_WHITELIST = "setWhitelist";
	protected static final String ADD_ROUTINE = "addRoutine";
	protected static final String DELETE_ROUTINE = "deleteRoutine";
	protected static final String MODIFY_ROUTINE = "modifyRoutine";
	protected static final String GET_ROUTINES = "getRoutines";
	protected static final String ADD_COLOR_ROLE = "addColorRole";
	protected static final String DELETE_COLOR_ROLE = "deleteColorRole";
	protected static final String DELETE_COLOR_ROLES = "deleteColorRoles";
	protected static final String COLOR_ROLE_EXISTS = "colorRoleExists";
	protected static final String GET_COLOR_ROLES = "getColorRoles";
	protected static final String ADD_REMINDER_CHANNEL = "addReminderChannel";
	protected static final String DELETE_REMINDER_CHANNEL = "deleteReminderChannel";
	protected static final String GET_REMINDER_CHANNELS = "getReminderChannels";
	protected static final String ADD_REMINDER = "addReminder";
	protected static final String DELETE_REMINDER = "deleteReminder";
	protected static final String DELETE_REMINDERS = "deleteReminders";
	protected static final String MODIFY_REMINDER = "modifyReminder";
	protected static final String UPDATE_RECURRING_REMINDER_TIME = "updateRecurringReminderTime";
	protected static final String GET_REMINDER = "getReminder";
	protected static final String GET_REMINDERS = "getReminders";
	protected static final String GET_REMINDER_COUNT = "getReminderCount";
	protected static final String GET_NEXT_REMINDERS = "getNextReminders";
	
	static {
		queries.put(ADD_GUILD_IF_NOT_EXISTS, "INSERT INTO Guild (ID, Name) VALUES (?, ?) ON DUPLICATE KEY UPDATE ID=ID;");
		queries.put(ADD_USER_JOIN_SETTING_IF_NOT_EXISTS, "INSERT INTO UserJoinSetting (GuildID) VALUES (?) ON DUPLICATE KEY UPDATE ID=ID;");
		queries.put(ADD_REMINDER_SETTING_IF_NOT_EXISTS, "INSERT INTO ReminderSetting (GuildID) VALUES (?) ON DUPLICATE KEY UPDATE ID=ID;");
		queries.put(GET_USER_JOIN_SETTINGS, "SELECT Enabled, Mention, Message, ChannelID FROM UserJoinSetting WHERE GuildID = ?");
		queries.put(UPDATE_USER_JOIN_SETTINGS, "UPDATE UserJoinSetting SET Enabled = ?, Mention = ?, Message = ?, ChannelID = ? WHERE GuildID = ?");
		queries.put(GET_REMINDER_SETTINGS, "SELECT RestrictReminders, Whitelist FROM ReminderSetting WHERE GuildID = ?");
		queries.put(SET_REMINDER_SETTINGS, "UPDATE ReminderSetting SET RestrictReminders = ?, Whitelist = ? WHERE GuildID = ?");
		queries.put(SET_RESTRICT_REMINDERS, "UPDATE ReminderSetting SET RestrictReminders = ? WHERE GuildID = ?");
		queries.put(SET_WHITELIST, "UPDATE ReminderSetting SET Whitelist = ? WHERE GuildID = ?");
		queries.put(ADD_ROUTINE, "INSERT INTO BotRoutine (GuildID, UserTrigger, Response, FindSetting, CaseSensitive, Filter, FilterRoleId) VALUES (?, ?, ?, ?, ?, ?, ?)");
		queries.put(DELETE_ROUTINE, "DELETE FROM BotRoutine WHERE GuildID = ? AND UserTrigger = ?");
		queries.put(MODIFY_ROUTINE, "UPDATE BotRoutine SET UserTrigger = ?, Response = ?, FindSetting = ?, CaseSensitive = ?, Filter = ?, FilterRoleId = ? WHERE UserTrigger = ? AND GuildID = ?");
		queries.put(GET_ROUTINES, "SELECT UserTrigger, Response, FindSetting, CaseSensitive, Filter, FilterRoleId FROM BotRoutine WHERE GuildID = ? ORDER BY ID");
		queries.put(ADD_COLOR_ROLE, "INSERT INTO ColorRole (ID, GuildID) VALUES (?, ?)");
		queries.put(DELETE_COLOR_ROLE, "DELETE FROM ColorRole WHERE ID = ?");
		queries.put(DELETE_COLOR_ROLES, "DELETE FROM ColorRole WHERE ID IN (?)");
		queries.put(COLOR_ROLE_EXISTS, "SELECT ID FROM ColorRole WHERE ID = ? AND GuildID = ?");
		queries.put(GET_COLOR_ROLES, "SELECT ID FROM ColorRole WHERE GuildID = ? ORDER BY ID");
		queries.put(ADD_REMINDER_CHANNEL, "INSERT INTO ReminderChannel (ID, GuildID) VALUES (?, ?)");
		queries.put(DELETE_REMINDER_CHANNEL, "DELETE FROM ReminderChannel WHERE ID = ? AND GuildID = ?");
		queries.put(GET_REMINDER_CHANNELS, "SELECT ID FROM ReminderChannel WHERE GuildID = ?");
		queries.put(ADD_REMINDER, "INSERT INTO Reminder (EventString, EventTime, Mention, Recurring, FrequencyAmount, Frequency, UserID, ChannelID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		queries.put(DELETE_REMINDER, "DELETE FROM Reminder WHERE ID = ? AND UserId = ?");
		queries.put(DELETE_REMINDERS, "DELETE FROM Reminder WHERE ID IN (?)");
		queries.put(MODIFY_REMINDER, "UPDATE Reminder SET EventString = ?, EventTime = ?, Mention = ?, Recurring = ?, FrequencyAmount = ?, Frequency = ?, ChannelID = ? WHERE ID = ?");
		queries.put(UPDATE_RECURRING_REMINDER_TIME, "UPDATE Reminder SET EventTime = ? WHERE ID = ?");
		queries.put(GET_REMINDER, "SELECT ID, EventString, EventTime, Mention, Recurring, FrequencyAmount, Frequency, ChannelID FROM Reminder WHERE ID = ? AND UserID = ?");
		queries.put(GET_REMINDERS, "SELECT ID, EventString, EventTime FROM Reminder WHERE UserID = ? ORDER BY ID");
		queries.put(GET_REMINDER_COUNT, "SELECT COUNT(ID) FROM Reminder WHERE EventTime = ?");
		queries.put(GET_NEXT_REMINDERS, "SELECT ID, EventString, EventTime, Mention, Recurring, FrequencyAmount, Frequency, UserID, ChannelId FROM Reminder WHERE EventTime <= ? ORDER BY ID");
	}
	
	/**
	 * Initializes the connection pool
	 * @param config
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	protected static void createConnectionPool(HikariConfig config)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		connectionPool = new HikariPool(config);
		
		logger.info("Successfully initialized database connection pool.");
	}
	
	/**
	 * Shuts down the connection pool
	 */
	public static void disconnect() {
		try {
			connectionPool.shutdown();
		} catch (InterruptedException e) {
			logger.error("Database shutdown sequence interrupted: " + e);
		}
	}
	
	/**
	 * Initializes a connection to the database
	 * @return a database Connection
	 * @throws SQLException
	 */
	protected static Connection getConnection() throws SQLException {
		return connectionPool.getConnection();
	}
	
	/**
	 * Executes a query and returns the result set, typically used for SELECT statements.
	 * ALL CALLERS MUST USE releaseConnection() TO RELEASE THE CONNECTION FROM THIS METHOD ONCE FINISHED WITH THE ResultSet IT RETURNS!
	 * Unlike the other methods, since this method passes a ResultSet back to the caller, it cannot automatically release the database connection.
	 * @param <T> Any Object
	 * @param query The query to execute
	 * @param args The list of arguments, ordered from left to right, to populate into the above query
	 * @return the ResultSet returned by the given query
	 */
	@SafeVarargs
	protected static <T> ResultSet executeQuery(String query, T... args) {
		try {
			connection.set(getConnection());
			PreparedStatement statement = connection.get().prepareStatement(query);
			formatQuery(statement, args);
			
			StringBuilder logQuery = new StringBuilder("Executing query: ").append(query).append(" - with args ");
			for(T arg : args) {
				logQuery.append(arg).append(", ");
			}
			logQuery.delete(logQuery.length() - 2, logQuery.length());
			logger.debug(logQuery.toString());
			
			return statement.executeQuery();
		} catch(SQLException error) {
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Executes an update statement and returns the number of rows that were updated.
	 * @param <T> Any Object
	 * @param query The query to execute
	 * @param args The list of arguments, ordered from left to right, to populate into the above query
	 * @return the number of rows that were updated
	 */
	@SafeVarargs
	protected static <T> int executeUpdate(String query, T... args) {
		try(Connection connection = getConnection()) {
			PreparedStatement statement = connection.prepareStatement(query);
			formatQuery(statement, args);
			
			StringBuilder logQuery = new StringBuilder("Executing query: ").append(query).append(" - with args ");
			for(T arg : args) {
				logQuery.append(arg).append(", ");
			}
			logQuery.delete(logQuery.length() - 2, logQuery.length());
			logger.debug(logQuery.toString());
			
			return statement.executeUpdate();
		} catch(SQLException error) {
			throw new DudeDatabaseException(error);
		}
	}

	/**
	 * Executes an insert statement and returns the primary key of the object if inserted, null otherwise.
	 * @param <T> Any Object
	 * @param query The query to execute
	 * @param args The list of arguments, ordered from left to right, to populate into the above query
	 * @return the primary key of the inserted object, null otherwise
	 */
	@SafeVarargs
	protected static <T> Long executeInsert(String query, T... args) {
		try(Connection connection = getConnection()) {
			PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			formatQuery(statement, args);
			
			StringBuilder logQuery = new StringBuilder("Executing query: ").append(query).append(" - with args ");
			for(T arg : args) {
				logQuery.append(arg).append(", ");
			}
			logQuery.delete(logQuery.length() - 2, logQuery.length());
			logger.debug(logQuery.toString());
			
			statement.executeUpdate();
			ResultSet key = statement.getGeneratedKeys();
			return key.next() ? key.getLong(1) : null;
		} catch(SQLException error) {
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Releases this thread's database Connection. This method is automatically called by executeUpdate() and executeInsert(), but NOT
	 * by executeQuery() since the caller will get a ResultSet they need to work with beyond the scope of the method.
	 * @throws SQLException
	 */
	protected static void releaseConnection() throws SQLException {
		connection.get().close();
		connection.set(null);
	}
	
	/**
	 * Formats the parameters of a PreparedStatement with the given arguments, in the order they are provided.
	 * @param <T> An Integer, Long, String, or Boolean object
	 * @param statement A parameterized query whose parameters are currently unset
	 * @param args The arguments to parameterize the statement with
	 */
	@SafeVarargs
	public static <T> void formatQuery(PreparedStatement statement, T... args) {
		try {
			for(int i = 0; i < args.length; i++) {
				if(args[i] instanceof Integer) {
					statement.setInt(i + 1, (Integer) args[i]);
				} else if(args[i] instanceof Long) {
					statement.setLong(i + 1, (Long) args[i]);
				} else if(args[i] instanceof String) {
					statement.setString(i + 1, (String) args[i]);
				} else if(args[i] instanceof Boolean) {
					statement.setBoolean(i + 1, (Boolean) args[i]);
				} else if(args[i] == null) {
					statement.setNull(i + 1, java.sql.Types.NULL);
				} else {
					throw new DudeDatabaseException("Unsupported database argument type");
				}
			}
		} catch(SQLException error) {
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Convenience method to format a query with a where clause of type "WHERE X IN (?)", given a set of arguments.
	 * @param args The values for the IN clause.
	 */
	public static <T extends Object> String prepareInStatement(String query, Set<T> args) {
		StringBuilder valueList = new StringBuilder();
		Iterator<T> it = args.iterator();
		while(it.hasNext()) {
			T value = it.next();
			if(value instanceof String) {
				valueList.append("'");
			}
			valueList.append(value);
			if(value instanceof String) {
				valueList.append("'");
			}
			if(it.hasNext()) {
				valueList.append(", ");
			}
		}
		return query.replace("?", valueList.toString());
	}
	
	/**
	 * An annoying but necessary method to get a null result for a Long, as by default ResultSet just returns 0.
	 * @param rs
	 * @param index
	 * @return
	 * @throws SQLException
	 */
	private static Long getNullableLong(ResultSet rs, int index) throws SQLException {
		Long result = rs.getLong(index);
		if(rs.wasNull()) result = null;
		return result;
	}
	
	/**
	 * Programmatically creates a new database by running a creation SQL script.
	 * Used for example to setup a database for integration tests.
	 */
	public static void setupDatabase() {
		try(Connection connection = getConnection()) {
			//convert create script to String
			InputStream create = Dude.class.getClassLoader().getResourceAsStream(createScript);
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			result.write(create.readAllBytes());
			String[] scriptLines = result.toString("UTF-8").split(";");
			
			//execute script
			Statement script = connection.createStatement();
			for(String line : scriptLines) {
				script.addBatch(line);
			}
			script.executeBatch();
		} catch (IOException | SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Inserts a new guild if it doesn't exist already.
	 * @param guildId
	 */
	public static void addGuildIfNotExists(Long guildId, String guildName) {
		executeInsert(queries.get(ADD_GUILD_IF_NOT_EXISTS), guildId, guildName);
		executeInsert(queries.get(ADD_USER_JOIN_SETTING_IF_NOT_EXISTS), guildId);
		executeInsert(queries.get(ADD_REMINDER_SETTING_IF_NOT_EXISTS), guildId);
	}
	
	/**
	 * Retrieves the UserJoinSettings for a particular guild.
	 * @param guildId
	 * @return the guild's UserJoinSettings
	 */
	public static UserJoinSettings getUserJoinSettings(Long guildId) {
		try {
			ResultSet settings = executeQuery(queries.get(GET_USER_JOIN_SETTINGS), guildId);
			settings.next();
			UserJoinSettings userJoinSettings = new UserJoinSettings(settings.getBoolean(1), settings.getBoolean(2), settings.getString(3), getNullableLong(settings, 4));
			return userJoinSettings;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Updates a particular guild's UserJoinSettings.
	 * @param guildId
	 * @param settings
	 */
	public static void updateUserJoinSettings(Long guildId, UserJoinSettings settings) {
		executeUpdate(queries.get(UPDATE_USER_JOIN_SETTINGS), settings.getEnabled(), settings.getMention(), settings.getMessage(), settings.getChannelId(), guildId);
	}
	
	/**
	 * Get the ReminderSettings for a particular guild.
	 * @param guildId
	 * @return the guild's ReminderSettings
	 */
	public static ReminderSettings getReminderSettings(Long guildId) {
		try {
			ResultSet settings = executeQuery(queries.get(GET_REMINDER_SETTINGS), guildId);
			settings.next();
			ReminderSettings reminderSettings = new ReminderSettings(settings.getBoolean(1), settings.getBoolean(2));
			return reminderSettings;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Update the ReminderSettings for a particular guild. Should be used when the values of both
	 * reminder restriction and whitelist change.
	 * @param guildId
	 * @param settings
	 */
	public static void setReminderSettings(Long guildId, ReminderSettings settings) {
		executeUpdate(queries.get(SET_REMINDER_SETTINGS), settings.getRestrictReminders(), settings.getWhitelist(), guildId);
	}
	
	/**
	 * Update whether reminders are restricted for a particular guild.
	 * @param guildId
	 * @param restrict
	 */
	public static void setRestrictReminders(Long guildId, Boolean restrict) {
		executeUpdate(queries.get(SET_RESTRICT_REMINDERS), restrict, guildId);
	}
	
	/**
	 * Update whether reminder restrictions are whitelisted or blacklisted for a particular guild.
	 * @param guildId
	 * @param whitelist
	 */
	public static void setWhitelist(Long guildId, Boolean whitelist) {
		executeUpdate(queries.get(SET_WHITELIST), whitelist, guildId);
	}
	
	/**
	 * Adds a new routine for a particular guild.
	 * @param guildId
	 * @param routine
	 * @return the newly generated primary key of the routine
	 * @throws SQLException 
	 */
	public static Long addRoutine(Long guildId, Routine routine) {
		return executeInsert(queries.get(ADD_ROUTINE), guildId, routine.getTrigger(), routine.getResponse(), routine.getFindSetting().ordinal(), routine.getCaseSensitive(), routine.getFilter(), routine.getFilterRoleId());
	}
	
	/**
	 * Deletes a new routine for a particular guild.
	 * @param guildId
	 * @param trigger
	 */
	public static void deleteRoutine(Long guildId, String trigger) {
		executeUpdate(queries.get(DELETE_ROUTINE), guildId, trigger);
	}
	
	/**
	 * Updates a routine for a particular guild.
	 * @param guildId
	 * @param trigger
	 * @param newRoutine
	 */
	public static void modifyRoutine(Long guildId, String trigger, Routine newRoutine) {
		executeUpdate(queries.get(MODIFY_ROUTINE), newRoutine.getTrigger(), newRoutine.getResponse(), newRoutine.getFindSetting().ordinal(), newRoutine.getCaseSensitive(), newRoutine.getFilter(), newRoutine.getFilterRoleId(), trigger, guildId);
	}
	
	/**
	 * Gets all the routines for a particular guild. Returns a LinkedHashMap to maintain order.
	 * @param guildId
	 * @return a map of triggers to the routines they belong to
	 */
	public static LinkedHashMap<String, Routine> getRoutines(Long guildId) {
		try {
			ResultSet routines = executeQuery(queries.get(GET_ROUTINES), guildId);
			LinkedHashMap<String, Routine> routinesByTrigger = new LinkedHashMap<String, Routine>();
			while(routines.next()) {
				routinesByTrigger.put(routines.getString(1), new Routine(routines.getString(1), routines.getString(2), RoutineFindSetting.getById(routines.getInt(3)), routines.getBoolean(4), routines.getBoolean(5), getNullableLong(routines, 6)));
			}
			return routinesByTrigger;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Adds a color role for a particular guild.
	 * @param guildId
	 * @param roleId
	 * @return the primary key of the inserted color role
	 */
	public static Long addColorRole(Long guildId, Long roleId) {
		return executeInsert(queries.get(ADD_COLOR_ROLE), roleId, guildId);
	}
	
	/**
	 * Deletes a color role for a particular guild.
	 * @param roleId
	 */
	public static void deleteColorRole(Long roleId) {
		executeUpdate(queries.get(DELETE_COLOR_ROLE), roleId);
	}
	
	/**
	 * Deletes a set of color roles for a particular guild.
	 * Warning: this method and query provides no validation that the roles all belong to the same guild,
	 * this must be performed by the caller.
	 * @param roleIds
	 */
	public static void deleteColorRoles(Set<Long> roleIds) {
		//Statement should be free from SQL injection as the arguments are only longs and the method is only used by EventHandler
		try(Connection connection = getConnection()) {
			Statement statement = connection.createStatement();
			String query = prepareInStatement(queries.get(DELETE_COLOR_ROLES), roleIds);
			logger.debug("Executing query: " + query);
			statement.executeUpdate(query);
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Returns whether a particular guild contains a color role or not.
	 * @param guildId
	 * @param roleId
	 * @return True if the guild contains the color role, false otherwise
	 */
	public static boolean colorRoleExists(Long guildId, Long roleId) {
		try {
			return executeQuery(queries.get(COLOR_ROLE_EXISTS), roleId, guildId).next();
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Gets all of the color roles for a particular guild.
	 * @param guildId
	 * @return all of a guild's color roles
	 */
	public static List<Long> getColorRoles(Long guildId) {
		try {
			ResultSet colorRoleIds = executeQuery(queries.get(GET_COLOR_ROLES), guildId);
			List<Long> colorRoles = new ArrayList<Long>();
			while(colorRoleIds.next()) {
				colorRoles.add(colorRoleIds.getLong(1));
			}
			return colorRoles;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Adds a reminder channel into the restriction list for a particular guild.
	 * @param guildId
	 * @param channelId
	 * @return the primary key of the newly inserted reminder channel
	 */
	public static Long addReminderChannel(Long guildId, Long channelId) {
		return executeInsert(queries.get(ADD_REMINDER_CHANNEL), channelId, guildId);
	}
	
	/**
	 * Deletes a reminder channel from the restriction list for a particular guild.
	 * @param guildId
	 * @param channelId
	 * @return True if the reminder channel was deleted, false if it did not exist
	 */
	public static boolean deleteReminderChannel(Long guildId, Long channelId) {
		return executeUpdate(queries.get(DELETE_REMINDER_CHANNEL), channelId, guildId) > 0;
	}
	
	/**
	 * Gets all of the channels in the reminder restriction list for a particular guild.
	 * @param guildId
	 * @return all of a guild's restricted reminder channels
	 */
	public static List<Long> getReminderChannels(Long guildId) {
		try {
			ResultSet channelIds = executeQuery(queries.get(GET_REMINDER_CHANNELS), guildId);
			List<Long> channels = new LinkedList<Long>();
			while(channelIds.next()) {
				channels.add(channelIds.getLong(1));
			}
			return channels;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Adds a new reminder.
	 * The reminder's time is stored as a Long, in "seconds since the epoch" form.
	 * The Frequency and FrequencyAmount fields are left null for non-recurring reminders.
	 * @param reminder The desired Reminder to store
	 */
	public static Long addReminder(Reminder reminder) {
		return executeInsert(queries.get(ADD_REMINDER), reminder.getEvent(), reminder.getTime().getEpochSecond(), reminder.getMention(), reminder.getRecurring(), reminder.getFrequencyAmount(), reminder.getFrequency() != null ? reminder.getFrequency().toString().toUpperCase() : null, reminder.getUserId(), reminder.getChannelId());
	}
	
	/**
	 * Deletes a reminder for a particular user.
	 * @param reminderId
	 * @param userId
	 * @return True if the reminder existed, false otherwise
	 */
	public static boolean deleteReminder(Long reminderId, Long userId) {
		return executeUpdate(queries.get(DELETE_REMINDER), reminderId, userId) > 0;
	}

	/**
	 * Deletes a set of reminders.
	 * Warning: this method and query provides no validation that the reminders all belong to given users,
	 * this must be performed by the caller.
	 * @param reminderIds The reminders to delete.
	 */
	public static void deleteReminders(Set<Long> reminderIds) {
		//Statement should be free from SQL injection as the arguments are only longs and the method is only used by EventHandler
		try(Connection connection = getConnection()) {
			Statement statement = connection.createStatement();
			String query = prepareInStatement(queries.get(DELETE_REMINDERS), reminderIds);
			logger.debug("Executing query: " + query);
			statement.executeUpdate(query);
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		}
	}
	
	/**
	 * Updates a reminder.
	 * @param reminder
	 */
	public static void modifyReminder(Reminder reminder) {
		executeUpdate(queries.get(MODIFY_REMINDER), reminder.getEvent(), reminder.getTime().getEpochSecond(), reminder.getMention(), reminder.getRecurring(), reminder.getFrequencyAmount(), reminder.getFrequency() != null ? reminder.getFrequency().toString().toUpperCase() : null, reminder.getChannelId(), reminder.getPrimaryKey());
	}
	
	/**
	 * For a reminder that is set to recur, updates its time to a new time in the future (where an ordinary reminder would be deleted).
	 * @param reminder
	 */
	public static void updateRecurringReminder(Reminder reminder) {
		executeUpdate(queries.get(UPDATE_RECURRING_REMINDER_TIME), reminder.getTime().getEpochSecond(), reminder.getPrimaryKey());
	}
	
	/**
	 * Gets a given reminder.
	 * @param reminderId
	 * @param userId
	 * @return The reminder with the given ID for the given user
	 */
	public static Reminder getReminder(Long reminderId, Long userId) {
		try {
			ResultSet remindersSet = executeQuery(queries.get(GET_REMINDER), reminderId, userId);
			Reminder reminder = null;
			if(remindersSet.isBeforeFirst() && remindersSet.next()) {
				reminder = new Reminder(remindersSet.getLong(1), remindersSet.getString(2), Instant.ofEpochSecond(remindersSet.getLong(3)), remindersSet.getBoolean(4), remindersSet.getBoolean(5), getNullableLong(remindersSet, 6), remindersSet.getString(7) != null ? ChronoUnit.valueOf(remindersSet.getString(7)) : null, userId, remindersSet.getLong(8));
			}
			return reminder;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Gets all reminders for a given user.
	 * @param userId
	 * @return all the given user's reminders
	 */
	public static List<Reminder> getReminders(Long userId) {
		try {
			ResultSet remindersSet = executeQuery(queries.get(GET_REMINDERS), userId);
			List<Reminder> reminders = new ArrayList<>();
			while(remindersSet.next()) {
				reminders.add(new Reminder(remindersSet.getLong(1), remindersSet.getString(2), Instant.ofEpochSecond(remindersSet.getLong(3)), null, null, null, null, null, null));
			}
			return reminders;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Gets the number of reminders whose trigger time is a given moment, given in epoch second form.
	 * @param time
	 * @return the amount of reminders with the given trigger time
	 */
	public static long getReminderCount(Long time) {
		try {
			ResultSet reminderCount = executeQuery(queries.get(GET_REMINDER_COUNT), time);
			reminderCount.next();
			long count = reminderCount.getLong(1);
			return count;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
	
	/**
	 * Gets all reminders whose trigger time is on or before the given time, in epoch second form.
	 * @param time
	 * @return all reminders on or before the given trigger time
	 */
	public static List<Reminder> getNextReminders(Long time) {
		try {
			ResultSet nextReminders = executeQuery(queries.get(GET_NEXT_REMINDERS), time);
			List<Reminder> reminders = new ArrayList<>();
			while(nextReminders.next()) {
				reminders.add(new Reminder(nextReminders.getLong(1), nextReminders.getString(2), Instant.ofEpochSecond(nextReminders.getLong(3)), nextReminders.getBoolean(4), nextReminders.getBoolean(5), nextReminders.getLong(6) != 0 ? nextReminders.getLong(6) : null, nextReminders.getString(7) != null ? ChronoUnit.valueOf(nextReminders.getString(7)) : null, nextReminders.getLong(8), nextReminders.getLong(9)));
			}
			return reminders;
		} catch(SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		} finally {
			try {
				releaseConnection();
			} catch (SQLException error) {
				logger.error(error);
				throw new DudeDatabaseException(error);
			}
		}
	}
}
