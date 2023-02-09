package dakota.dude.handler.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;

public class MySQLDatabaseHandler extends DatabaseHandler {
	
	static {
		createScript = "mysqlCreate.sql";
	}

	/**
	 * Initializes a MySQL database with optimal configurations.
	 * @param server
	 * @param databaseName
	 * @param username
	 * @param password
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws SQLException
	 */
	public static void initialize(String server, String databaseName, String username, String password)
				throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException,
				NoSuchMethodException, SecurityException, SQLException {
		HikariConfig config = new HikariConfig();
		StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://");
		jdbcUrl.append(server);
		jdbcUrl.append("/");
		//used in setupDatabase()
		DatabaseHandler.databaseName = databaseName;
		jdbcUrl.append(databaseName);
		
		config.setJdbcUrl(jdbcUrl.toString());
		config.setUsername(username);
		config.setPassword(password);
		//5 seconds short of the mysql timeout, 8 hours, as recommended by HikariCP
		config.setMaxLifetime(28799995);
		//2 * cores + # of drives, as recommended by HikariCP
		config.setMaximumPoolSize(2 * Runtime.getRuntime().availableProcessors() + 1);
		
		config.addDataSourceProperty("dataSource.cachePrepStmts", true);
		config.addDataSourceProperty("dataSource.prepStmtCacheSize", queries.size());
		config.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", 2048);
		config.addDataSourceProperty("dataSource.useServerPrepStmts", true);
		config.addDataSourceProperty("dataSource.useLocalSessionState", true);
		config.addDataSourceProperty("dataSource.useLocalTransactionState", true);
		config.addDataSourceProperty("dataSource.rewriteBatchedStatements", true);
		config.addDataSourceProperty("dataSource.cacheResultSetMetadata", true);
		config.addDataSourceProperty("dataSource.cacheServerConfiguration", true);
		config.addDataSourceProperty("dataSource.elideSetAutoCommits", true);
		config.addDataSourceProperty("dataSource.cacheServerConfiguration", true);
		config.addDataSourceProperty("dataSource.maintainTimeStats", false);
		config.addDataSourceProperty("dataSource.socketTimeout", 30);
		
		createConnectionPool(config);
	}
	
	/**
	 * Performs the necessary schema level drop and create statements, then executes the creation script to create a new database schema.
	 */
	public static void setupDatabase() {
		String drop = "DROP DATABASE IF EXISTS " + databaseName + ";";
		String create = "CREATE DATABASE " + databaseName + ";";
		String use = "USE " + databaseName + ";";

		try(Connection connection = getConnection()) {
			//execute script
			Statement script = connection.createStatement();
			script.execute(drop);
			script.execute(create);
			script.execute(use);
		} catch (SQLException error) {
			logger.error(error);
			throw new DudeDatabaseException(error);
		}
		
		DatabaseHandler.setupDatabase();
	}
}
