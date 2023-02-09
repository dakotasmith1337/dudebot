package dakota.dude.model;

/**
 * EXACT - only trigger if the message consists of only the trigger
 * WORD - only trigger if the trigger is only surrounded by punctuation/whitespace
 * CONTAINS - trigger if the trigger is found anywhere in the string
 *
 */
public enum RoutineFindSetting {
	CONTAINS, WORD, EXACT;

	/**
	 * Returns a value of this enum given a ID with 0-based indexing.
	 * @param id
	 * @return the enum value with the given 0-based indexing ID
	 */
	public static RoutineFindSetting getById(int id) {
		switch(id) {
		case 0: return CONTAINS;
		case 1: return WORD;
		case 2: return EXACT;
		default: return null;
		}
	}
	
	/**
	 * Effectively .toLowerCase() for every character except the first.
	 * @return the capitalized version of this enum value
	 */
	public String capitalized() {
		return this.toString().substring(0, 1) + this.toString().substring(1).toLowerCase();
	}
}
