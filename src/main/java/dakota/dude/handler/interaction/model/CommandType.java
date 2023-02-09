package dakota.dude.handler.interaction.model;

public enum CommandType {
	COMMAND,
	GROUP,
	STRING,
	INTEGER,
	BOOLEAN,
	USER,
	CHANNEL,
	ROLE,
	MENTIONABLE,
	NUMBER,
	ATTACHMENT;
	
	/**
	 * Returns the ID with 1-based indexing for a given value of this enum.
	 * @return
	 */
	public int getId() {
		return this.ordinal() + 1;
	}
	
	/**
	 * Returns a value of this enum given a ID with 1-based indexing.
	 * @param id
	 * @return
	 */
	public static CommandType getById(int id) {
		switch(id) {
			case 1: return COMMAND;
			case 2: return GROUP;
			case 3: return STRING;
			case 4: return INTEGER;
			case 5: return BOOLEAN;
			case 6: return USER;
			case 7: return CHANNEL;
			case 8: return ROLE;
			case 9: return MENTIONABLE;
			case 10: return NUMBER;
			case 11: return ATTACHMENT;
			default: return null;
		}
	}
}
