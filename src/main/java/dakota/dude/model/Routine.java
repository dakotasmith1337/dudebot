package dakota.dude.model;

import java.util.Set;

public class Routine {
	
	private String trigger = "";
	private String response = "";
	private RoutineFindSetting findSetting = RoutineFindSetting.CONTAINS;
	private boolean caseSensitive = false;
	private boolean filter = false;
	private Long filterRoleId = 0L;
	
	public Object lock = new Object();
	
	/**
	 * Creates a message-response Routine.
	 * @param triggers The list of strings that trigger the execution of this routine.
	 * @param explicit False if the trigger should be found by contains() logic, true if it should be found by "word in a sentence" logic.
	 * @param filterUsers Automatically set to true if users is nonempty, false otherwise
	 * @param users The whitelist of users to filter against for the execution of this routine.
	 * @param tts Whether the response should be text to speech
	 * @param response The desired response of this routine.
	 */
	public Routine(String trigger, String response, RoutineFindSetting findSetting, boolean caseSensitive, boolean filter, Long filterRoleId) {
		this.trigger = trigger;
		this.response = response;
		this.findSetting = findSetting;
		this.caseSensitive = caseSensitive;
		this.filter = filter;
		this.filterRoleId = filterRoleId;
	}
	
	/**
	 * Gets the trigger for this routine.
	 * @return The trigger for this routine
	 */
	public String getTrigger() {
		return trigger;
	}
	
	/**
	 * Sets the trigger for this routine.
	 * @param trigger The trigger for this routine
	 */
	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}
	
	/**
	 * Gets the response for this routine.
	 * @return The response
	 */
	public String getResponse() {
		return response;
	}
	
	/**
	 * Sets the response for this routine.
	 * @param response The response
	 */
	public void setResponse(String response) {
		this.response = response;
	}
	
	/**
	 * Gets the method by which the trigger is found.
	 * @return The method by which the trigger is found
	 */
	public RoutineFindSetting getFindSetting() {
		return findSetting;
	}
		
	/**
	 * Sets the method by which the trigger is found.
	 * @param findSetting The method by which the trigger is found
	 */
	public void setFindSetting(RoutineFindSetting findSetting) {
		this.findSetting = findSetting;
	}
	
	/**
	 * Gets whether the trigger is case sensitive.
	 * @return Whether the trigger is case sensitive
	 */
	public Boolean getCaseSensitive() {
		return caseSensitive;
	}
	
	/**
	 * Sets whether the trigger is case sensitive.
	 * @param caseSensitive Whether the trigger is case sensitive
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	/**
	 * Sets whether this Routine filters on a user or role.
	 * @return Whether this Routine filters on a user or role
	 */
	public Boolean getFilter() {
		return filter;
	}
	
	/**
	 * Gets whether this Routine filters on a user or role.
	 * @param filterUsers Whether this Routine filters on a user or role
	 */
	public void setFilter(boolean filter) {
		this.filter = filter;
	}
	
	/**
	 * Gets the ID of the user or role to be whitelisted for the routine.
	 * @return The ID of the user or role to be whitelisted for the routine
	 */
	public Long getFilterRoleId() {
		return filterRoleId;
	}
	
	/**
	 * Sets the ID of the user or role to be whitelisted for the routine.
	 * @param filterObjectId The ID of the user or role to be whitelisted for the routine
	 */
	public void setFilterRoleId(Long filterRoleId) {
		this.filterRoleId = filterRoleId;
	}
	
	/**
	 * Returns whether the beginning of message is "phrase" followed by ' ', ',', '.', or '!'.
	 * @param message The message to check
	 * @param phrase The phrase to look for
	 * @return Whether the beginning of message is "phrase" followed by ' ', ',', '.', or '!'
	 */
	private boolean atBeginning(String message, String phrase) {
		if(message.length() <= phrase.length()) {
			return false;
		}
		if((message.substring(0,phrase.length()) == phrase) &&
				(message.charAt(phrase.length()) == ' '
			  || message.charAt(phrase.length()) == ','
			  || message.charAt(phrase.length()) == '.'
			  || message.charAt(phrase.length()) == '!')) {
			return true;
		}
		return false;
	}

	/**
	 * Returns whether message contains phrase preceded by space or newline, and followed by space or punctuation.
	 * @param message The message to check
	 * @param phrase The phrase to look for
	 * @return Whether message contains phrase preceded by space or newline, and followed by space or punctuation
	 */
	private boolean atMiddle(String message, String phrase) {
		if(message.contains(' ' + phrase + ' ')
		|| message.contains(' ' + phrase + ',')
		|| message.contains(' ' + phrase + '.')
		|| message.contains(' ' + phrase + '!')
		|| message.contains(' ' + phrase + '\n')
		|| message.contains('\n' + phrase + ' ')
		|| message.contains('\n' + phrase + '\n')
		|| message.contains('\n' + phrase + ',')
		|| message.contains('\n' + phrase + '.')
		|| message.contains('\n' + phrase + '!')) {
				return true;
		}
		return false;
	}

	/**
	 * Returns whether the end of message is " phrase", " phrase.", or " phrase!".
	 * @param message The message to check
	 * @param phrase The phrase to look for
	 * @return Whether the end of message is " phrase", " phrase.", or " phrase!"
	 */
	private boolean atEnd(String message, String phrase) {
		if(message.length() <= phrase.length() + 1) return false;
		String periodPhrase = phrase + '.';
		String exPhrase = phrase + '!';
		if((message.substring(message.length()-phrase.length(),message.length()-1).equalsIgnoreCase(phrase))
		|| (message.substring(message.length()-phrase.length()-1,message.length()-1).equalsIgnoreCase(periodPhrase))
		|| (message.substring(message.length()-phrase.length()-1,message.length()-1).equalsIgnoreCase(exPhrase))) {
			return true;
		}
		return false;
	}

	/**
	 * Returns whether message consists of phrase, begins with phrase, contains " phrase" followed by ' ', ',', '.', or '!', or ends with phrase followed by nothing, '.' or '!'.
	 * @param message The message to check
	 * @param phrase The phrase to look for
	 * @return Whether message consists of phrase, begins with phrase, contains " phrase" followed by ' ', ',', '.', or '!', or ends with phrase followed by nothing, '.' or '!'
	 */
	private boolean isWord(String message, String phrase) {
		if(!message.contains(phrase)) {
			return false;
		}
		return (message.equalsIgnoreCase(phrase) || atBeginning(message, phrase) || atMiddle(message, phrase) || atEnd(message, phrase));
	}
	
	/**
	 * Returns true if message contains the trigger using the appropriate logic based on the find setting.
	 * @param message The message to check
	 * @return True if message contains the trigger using the appropriate logic based on the find setting.
	 */
	public boolean findTrigger(String message) {
		String searchMessage = caseSensitive ? message : message.toLowerCase();
		String searchTrigger = caseSensitive ? trigger : trigger.toLowerCase();
		switch(findSetting) {
		case EXACT:
			return searchMessage.equals(searchTrigger);
		case WORD:
			return isWord(searchMessage, searchTrigger);
		case CONTAINS:
		default:
			return searchMessage.contains(searchTrigger);
		}
	}
	
	/**
	 * Returns true if 'trigger' is contained within, vice versa, or equal to the given existing triggers.
	 * @param trigger
	 * @param existingTriggers
	 * @return True if 'trigger' is contained within, vice versa, or equal to the given existing triggers
	 */
	public static boolean containsValidation(String trigger, Set<String> existingTriggers) {
		//the user could always change the caseSensitive setting later without changing the trigger, so we need to compare as strictly as possible,
		//so we compare every string insensitive to case
		trigger = trigger.toLowerCase();
		for(String existingTrigger : existingTriggers) {
			existingTrigger = existingTrigger.toLowerCase();
			if(trigger.length() > existingTrigger.length()) {
				if(trigger.contains(existingTrigger)) return true;
			} else if (trigger.length() < existingTrigger.length()) {
				if(existingTrigger.contains(trigger)) return true;
			} else {
				if(trigger.equals(existingTrigger)) return true;
			}
		}
		return false;
	}
}