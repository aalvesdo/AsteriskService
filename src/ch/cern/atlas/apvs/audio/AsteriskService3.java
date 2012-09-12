package ch.cern.atlas.apvs.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.HangupAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.action.SipPeersAction;
import org.asteriskjava.manager.event.ManagerEvent;

import com.sun.tools.javac.util.List;


public class AsteriskService3 implements ManagerEventListener{

	private ManagerConnection managerConnection;
	private AsteriskServer asteriskServer;
	private ArrayList<SipUsers> usersList;
	
	private static final String CONTEXT ="internal";
	private static final int PRIORITY = 1;
	private static final int TIMEOUT = 20000;
	private static final String DESTINATION = "1000";

//*********************************************	
	// Constructor
	public AsteriskService3(){
		
		usersList = new ArrayList<SipUsers>();
		
		// Asterisk Connection Manager
		ManagerConnectionFactory factory = new ManagerConnectionFactory("pcatlaswpss02.cern.ch", "manager", "password");
		this.managerConnection = factory.createManagerConnection();
		
		// TODO Implement single connection with asterisk server 
		// Eases the communication with asterisk server
		asteriskServer = new DefaultAsteriskServer(managerConnection);
		
		// Event handler
		managerConnection.addEventListener(this);
	}
	
//*********************************************		
	// Auxiliar Functions
	
	// Online users usernames
	public ArrayList<String> listUsers(){
		ArrayList<String> usernameList = new ArrayList<String>();
		for(int i=0; i<usersList.size();i++){
			usernameList.add(usersList.get(i).getUsername());
		}
		return usernameList;
	}
	
	// Erase User
	public void eraseUser(String username){
		for(int i=0; i<usersList.size();i++){
			//TODO erase user
			if(usersList.get(i).getUsername().equals(username)){
				usersList.remove(i);
				break;
			}
		}
	}
	
	public String contentValue(String content){
		return content.substring(content.indexOf("'",0)+1,content.indexOf("'",content.indexOf("'",0)+1));
	}
	
	public int getIndexOfUsername(String typeAndUsername){
		for (int i=0; i<usersList.size() ;i++){
			if((usersList.get(i).getType() +"/"+ usersList.get(i).getUsername()).equals(typeAndUsername))
				return i;
		}
		return -1;
	}
	
	
//*********************************************
	
	// Users Register and Unregister
	public void peerStatusEvent(String evntContent) {
		
		
		String[] list = evntContent.replace(',','\n').split("\\n");
		//System.out.println("\n\n\n\n\n"+aux[1]+"\n\n\n\n\n");
		boolean read= false;
		SipUsers user = new SipUsers();
		
		for(int i=0 ; i<list.length; i++){
			if(list[i].contains("peer=")){
				String[] username=contentValue(list[i]).split("/");
				user.setUsername(username[1]);
					//System.out.println(user.getUsername()+",");
				read = true;
			}else{ 
				
				if(read==true){
					
					if(list[i].contains("channeltype"))
						user.setType(contentValue(list[i]));
					
					if(list[i].contains("peerstatus")){
						//Not Empty Users List
						if(!usersList.isEmpty()){
							if(contentValue(list[i]).equals("Registered")){
								if(listUsers().contains(user.getUsername())){
									// Do nothing when user is renew registration lease
									break;
								}
									
								else{
									// Registration for a new user
									user.setActiveCallChannel("");
									user.setActiveCallUsername("");
									usersList.add(user);
									break;
								}
							}else if(contentValue(list[i]).equals("Unregistered")){
								if(listUsers().contains(user.getUsername())){
									// Erase a registered user from "Online Users List" when someone leaves Asterisk
									eraseUser(user.getUsername());
									break;
								}else
									// Do nothing when a not registered user leaves Asterisk
									break;
							}
						}else{
							// Empty User List
							if(contentValue(list[i]).equals("Registered")){
								// Add user when no one is registered
								user.setActiveCallChannel("");
								user.setActiveCallUsername("");
								usersList.add(user);
								break;
							}	
						}
					}
				}
			}
		}	
	}
	
	//*********************************************
	
	// Call Event
	public void newChannelEvent(String channel){
			//System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		for (int i=0 ; i<list.length; i++){
				//System.out.println("ENTROU");
			if(list[i].contains("channel=")){
				channel=contentValue(list[i]);
				String[] aux = channel.split("-");
					//System.out.println(getIndexOfUsername(aux[0]));
				usersList.get(getIndexOfUsername(aux[0])).setActiveCallChannel(channel);
					//System.out.println(usersList.get(getIndexOfUsername(aux[0])).getActiveCallChannel());
				break;
			}			
		}								
	}
	
	
	//*********************************************
	
	// Hangup Call Event
	public void hangupEvent(String channel){
			//System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		for (int i=0 ; i<list.length; i++){
				//System.out.println("ENTROU");
			if(list[i].contains("channel=")){
				channel=contentValue(list[i]);
				String[] aux = channel.replace("/", "-").split("-");
				for(int u=0; u<usersList.size(); u++){
					if(aux[1].equals(usersList.get(u).getUsername())){
						usersList.get(u).setActiveCallChannel("");
						usersList.get(u).setActiveCallUsername("");
						break;
					}
				}
			}			
		}								
	}
	
	
	//*********************************************

	// Bridge of Call Channels
	
	public void bridgeEvent(String channel) {
			//System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		ArrayList<String> usersBridged = new ArrayList<String>();
		
		for (int i=0 ; i<list.length; i++){
				//System.out.println("ENTROU");
			if(list[i].contains("channel")){
				channel=contentValue(list[i]);
				String[] aux = channel.replace("/", "-").split("-");
				usersBridged.add(aux[1]);
			}	
		}
		
		//TODO Improve for loop with getIndexOfUsername fuction
		for (int u=0; u<usersList.size(); u++){
			if(usersBridged.contains(usersList.get(u).getUsername())){
				for (int b=0; b<usersBridged.size(); b++){
					if(usersBridged.get(b).equals(usersList.get(u).getUsername()))
						continue;
					else{
						if(usersList.get(u).getActiveCallUsername().isEmpty()){
							usersList.get(u).setActiveCallUsername(usersBridged.get(b));
								System.out.println("ENTROU");
						}else{
							usersList.get(u).setActiveCallUsername(usersList.get(u).getActiveCallUsername()+","+usersBridged.get(b));
								System.out.println("ENTROU2");
						}
					}
				}	
			}
			
		}
			
			//System.out.println(getIndexOfUsername(aux[0]));
			//usersList.get(getIndexOfUsername(aux[0])).setActiveCallChannel(channel);
			//System.out.println(usersList.get(getIndexOfUsername(aux[0])).getActiveCallChannel());			
	}
	
	//*********************************************
	
	// Establish Calls

	//TODO Resolve bug Anonymous call
	public void call(String callerOriginater, String callerDestination){
			//TODO Change the "internal" parameter and "timeout-20000" to CONSTANTS
		asteriskServer.originateToExtension(callerOriginater, CONTEXT, callerDestination, PRIORITY, TIMEOUT);		
	}
	
	// Establish Calls
	public void hangup(String channel) throws IllegalArgumentException, IllegalStateException, IOException, TimeoutException{
		HangupAction hangup = new HangupAction(channel);
		managerConnection.sendAction(hangup);
	}
	
	//*********************************************
	
	// Events Listener
	
	@Override
	//TODO Complete with events
	public void onManagerEvent(ManagerEvent event) {
		// TODO Improve deal with different events		
		System.out.println(event.toString());
		String[] eventContent = event.toString().split("\\[");		
    	
		/*//TODO To see if have to deal with this event
		 if(eventContent[0].contains("PeerEntryEvent"))
    		listOnlineUsers(eventContent[1]);
		*/
		
    	// NewChannelEvent    	
		if(eventContent[0].contains("NewChannelEvent"))
    		newChannelEvent(eventContent[1]);
    	
    	// BridgeEvent
		if(eventContent[0].contains("BridgeEvent"))
    		bridgeEvent(eventContent[1]);
    	
		// PeerStatusEvent
		if(eventContent[0].contains("PeerStatusEvent")){
			peerStatusEvent(eventContent[1]);
			//System.out.println(usersList.get(0).getUsername());
			System.out.println(usersList.size());
		}
		
		// HangupEvent
		if(eventContent[0].contains("HangupEvent")){
			hangupEvent(eventContent[1]);
				//System.out.println(usersList.get(0).getUsername());
			//System.out.println(usersList.size());
				}
    	
	}
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	// Test
    public static void main(String[] args) throws Exception
	{
    	AsteriskService3 manager = new AsteriskService3();			
        manager.managerConnection.login();
        //manager.periodicActions();
        	//Thread.sleep(1500000);
        //manager.printUsers();
        //manager.call(manager.usersList.get(1).getType()+"/"+manager.usersList.get(1).getUsername(), "1000");
        //manager.call("sip/1001", "1000"); //Originate caller has to include the channel type
        //manager.managerConnection.logoff();
        //Thread.sleep(20000);
        //System.out.println(manager.usersList.get(0).getActiveCallUsername());
        //Thread.sleep(5000);
        int menu = 0;
        while(true){
        	if(menu==0){
        		manager.mainMenu();
        		String a=manager.readCmd();
        		menu=Integer.parseInt(a);
        	}
        	else if(menu==1){
        				manager.usersMenu();
        				String a=manager.readCmd();
                		if(a.toUpperCase().equals("B"))
                			menu = 0;
        		 }
        		else if(menu==2){
        					manager.onlineCallMenu();
        					String a=manager.readCmd();
                    		if(a.toUpperCase().equals("B"))
                    			menu = 0;
        			 }
        			 else if(menu==3){
        				 		manager.callMenu();
        				 		String a=manager.readCmd();
                        		manager.call("SIP/"+a, DESTINATION);
                        		menu = 0;
        			 	  }
        			 	  else if(menu==4){
        			 		  		manager.hangupMenu();
        			 		  		String a=manager.readCmd();
        			 		  		for(int i=0; i<manager.usersList.size(); i++){
        			 		  			if(manager.usersList.get(i).getUsername().equals(a))
        			 		  				manager.hangup(manager.usersList.get(i).getActiveCallChannel());
        			 		  		}
                            		
        			 	  	   }
        	
        	
        	
        	
        	
        }        
        
	}
    
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    public void mainMenu(){
    	System.out.print("-------- Test Asterisk Service ----------\n"+
				  		 "|                                       |\n"+
				  		 "| Press 1 - List available users        |\n"+
				  		 "| Press 2 - List online calls           |\n"+
				  		 "|                                       |\n"+
				  		 "| Press 3 - Establish call              |\n"+
				  		 "| Press 4 - Hangup call                 |\n"+
				  		 "|                                       |\n"+
				  		 "|---------------------------------------|\n"+
    					 "Menu Option: ");
    }
    
    public void usersMenu(){
    	System.out.print("--------------- Users List --------------\n"+
    					 "|                                       |\n"+
    					 "  Users Online - "+ usersList.size()+"\n");
    					//for loop to list users with users list
    	if (usersList.isEmpty())
    		System.out.println("\n  No user is currently registered\n");
		for(int i =0; i<usersList.size(); i++)
			System.out.println("  "+usersList.get(i).getUsername()+"\n");
		     
		System.out.print("| Back to Main Menu - B                 |\n"+
						 "|---------------------------------------|\n"+
						 "Menu Option: ");
    }
    
    public void onlineCallMenu(){
    	System.out.print("--------------- Calls List --------------\n"+
    					 "|                                       |\n"+
    					 "  Calls Online - "+ numberOfCalls()+"\n");
    					//for loop to list users with users list
    	if (numberOfCalls()==0)
    		System.out.println("\n  No call is currently placed\n");
    	else{
    	   	for(int i=0; i < usersList.size(); i++){
    	   		if(!usersList.get(i).getActiveCallUsername().isEmpty())
    	   			System.out.println( usersList.get(i).getUsername() +" calling with "+ usersList.get(i).getActiveCallUsername());
    	    	}
    	}
		System.out.print("| Back to Main Menu - B                 |\n"+
						 "|---------------------------------------|\n"+
						 "Menu Option: ");
    }
    
    public void callMenu(){
    	System.out.print("------------------ Call -----------------\n"+
    					 "|                                       |\n");
    	if (usersList.isEmpty())
    		System.out.println("\n  No user is currently registered\n");
		for(int i =0; i<usersList.size(); i++)
			System.out.println("Call "+usersList.get(i).getUsername()+"\n");
		     
		System.out.print("| Back to Main Menu - B                 |\n"+
						 "|---------------------------------------|\n"+
						 "Menu Option: ");
    }

    public void hangupMenu(){
    	System.out.print("---------------- Hang Up ----------------\n"+
    					 "|                                       |\n");
    	if (numberOfCalls()==0)
    		System.out.println("\n  No call is currently placed\n");
    	else{
    	   	for(int i=0; i < usersList.size(); i++){
    	   		if(!usersList.get(i).getActiveCallUsername().isEmpty())
    	   			System.out.println("Hang up "+ usersList.get(i).getUsername());
    	    	}
    	}
		System.out.print("| Back to Main Menu - B                 |\n"+
						 "|---------------------------------------|\n"+
						 "Menu Option: ");
    }
    
    
    public int numberOfCalls(){
    	int rtn=0;
    	for(int i=0; i < usersList.size(); i++){
    		if(!usersList.get(i).getActiveCallUsername().isEmpty())
    			rtn=rtn+1;
    	}
    	return rtn/2;
    }
    
    
    public String readCmd() throws IOException
    {
        String cmd = "";
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));  
        cmd = br.readLine();
        return cmd;        		
    }
	
}
