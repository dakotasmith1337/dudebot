package dakota.dude.model;

public class ReminderSettings {
	
	private Boolean restrictReminders;
	private Boolean whitelist;
	
	public ReminderSettings(Boolean restrictReminders, Boolean whitelist) {
		this.restrictReminders = restrictReminders;
		this.whitelist = whitelist;
	}
	
	/**
	 * Returns whether reminders should be restricted.
	 * @return whether reminders should be restricted
	 */
	public Boolean getRestrictReminders() {
		return restrictReminders;
	}
	
	/**
	 * Sets whether reminders should be restricted.
	 * @param restrictReminders
	 */
	public void setRestrictReminders(Boolean restrictReminders) {
		this.restrictReminders = restrictReminders;
	}
	
	/**
	 * Gets whether the restricted channel list denotes whitelisted or blacklisted reminders.
	 * @return True if reminders can only be sent in the channels in the restriction list, false if reminders may not be sent in them
	 */
	public Boolean getWhitelist() {
		return whitelist;
	}
	
	/**
	 * Sets whether the restricted channel list denotes whitelisted or blacklisted reminders.
	 * @param whitelist True if reminders can only be sent in the channels in the restriction list, false if reminders may not be sent in them
	 */
	public void setWhitelist(Boolean whitelist) {
		this.whitelist = whitelist;
	}
}
