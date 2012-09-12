package ch.cern.atlas.apvs.audio;

public class SipUsers {

	private String username;
	private String type;
	private String activeCallUsername;
	private String activeCallChannel;
	

//*********************************************	
	// Constructor
	public SipUsers(){
		//this.username = "";
		//this.type = "";
		//this.activeCallUsername = "";
		//this.activeCallChannel = "";
	}
	
	public SipUsers(String username, String type){
		this.username = username;
		this.type = type;
	}
	
	public SipUsers(String username, String type, String activeCallUsername, String activeCallChannel){
		this.username = username;
		this.type = type;
		this.activeCallUsername = activeCallUsername;
		this.activeCallChannel = activeCallChannel;
	}

//*********************************************	
	// Get Methods
	public String getUsername(){
		return this.username;
	}
	
	public String getType(){
		return this.type;
	}

	public String getActiveCallUsername(){
		return this.activeCallUsername;
	}
	
	public String getActiveCallChannel(){
		return this.activeCallChannel;
	}
	
//*********************************************	
	// Set Methods
	public void setUsername(String username){
		this.username = username;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	public void setActiveCallUsername(String activeCallUsername){
		this.activeCallUsername = activeCallUsername;
	}
	
	public void setActiveCallChannel(String activeCallChannel){
		this.activeCallChannel = activeCallChannel;
	}

}
