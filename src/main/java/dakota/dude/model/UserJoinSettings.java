package dakota.dude.model;

public class UserJoinSettings {
	
	private Boolean enabled;
	private Boolean mention;
	private String message;
	private Long channelId;
	
	public UserJoinSettings(Boolean enabled, Boolean mention, String message, Long channelId) {
		this.enabled = enabled;
		this.mention = mention;
		this.message = message;
		this.channelId = channelId;
	}
	
	/**
	 * Returns whether the user join message function is enabled.
	 * @return whether the user join message function is enabled.
	 */
	public Boolean getEnabled() {
		return enabled;
	}
	
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	public Boolean getMention() {
		return mention;
	}
	
	public void setMention(Boolean mention) {
		this.mention = mention;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Long getChannelId() {
		return channelId;
	}
	
	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}
}
