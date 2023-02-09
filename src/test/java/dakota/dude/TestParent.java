package dakota.dude;

import org.junit.BeforeClass;

import dakota.dude.handler.database.MySQLDatabaseHandler;

public class TestParent {
	
	@BeforeClass
	public static void initializeDatabase() {
		//the tests won't have access to runtime args like Dude will, so we use a command-line property instead
		String decrypt = System.getProperty("dude.decrypt");
		Dude.initializeAndGetToken(decrypt != null ? new String[] { decrypt } : new String[0]);
		MySQLDatabaseHandler.setupDatabase();
	}
}
