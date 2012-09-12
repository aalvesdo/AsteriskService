package ch.cern.atlas.apvs.audio;

import java.io.IOException;
import java.util.ArrayList;

import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.action.SipPeersAction;
import org.asteriskjava.manager.event.ManagerEvent;

import com.sun.tools.javac.util.List;


public class AsteriskService2 implements ManagerEventListener{

	private ManagerConnection managerConnection;
	private AsteriskServer asteriskServer;
	private ArrayList<SipUsers> usersList;

//*********************************************	
	// Constructor
	public AsteriskService2(){
		
		usersList = new ArrayList<SipUsers>();
		
		// Asterisk Connection Manager
		ManagerConnectionFactory factory = new ManagerConnectionFactory("pcatlaswpss02.cern.ch", "manager", "password");
		this.managerConnection = factory.createManagerConnection();
		
		// TODO Implement single connection with asterisk server 
		// Eases the communication with asterisk server
		asteriskServer = new DefaultAsteriskServer(managerConnection);
		
		//TODO AddEvent handler
		managerConnection.addEventListener(this);
	}
	
//*********************************************		
	// Auxiliar Functions
	
	// Online users usernames
	private ArrayList<String> listUsers(){
		ArrayList<String> usernameList = new ArrayList<String>();
		for(int i=0; i<usersList.size();i++){
			usernameList.add(usersList.get(i).getUsername());
		}
		return usernameList;
	}
	
	// Erase User
	private void eraseUser(String username){
		for(int i=0; i<usersList.size();i++){
			//TODO erase user
			if(usersList.get(i).getUsername().equals(username)){
				usersList.remove(i);
				break;
			}
		}
	}
	
	
//*********************************************
	// Event Methods Handling
	
	// Users Register and Unregister
	private void peerStatusEvent(String status) {
		// TODO Auto-generated method stub
		
		String[] list = status.replace(',','\n').split("\\n");
		//System.out.println("\n\n\n\n\n"+aux[1]+"\n\n\n\n\n");
		boolean read= false;
		SipUsers user = new SipUsers();
		
		for(int i=0 ; i<list.length; i++){
			if(list[i].contains("peer=")){
				String[] username=list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).split("/");
				user.setUsername(username[1]);
					//System.out.println(user.getUsername()+",");
				read = true;
			}else{ 
				
				if(read==true){
					
					if(list[i].contains("channeltype"))
						user.setType(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)));
					
					if(list[i].contains("peerstatus")){
						//Not Empty Users List
						if(!usersList.isEmpty()){
							if((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered"))){
								if(listUsers().contains(user.getUsername())){
									break;
								}
									
								else{
									user.setActiveCallChannel("");
									user.setActiveCallUsername("");
									usersList.add(user);
									break;
								}
							}else if((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Unregistered"))){
								if(listUsers().contains(user.getUsername())){
									eraseUser(user.getUsername());
									break;
								}else
									break;
							}
						}else{
							// Empty User List
							if(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered")){
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
	
	//DONE Part of the newChannel function
	public int getIndexOfUsername(String typeAndUsername){
		for (int i=0; i<usersList.size() ;i++){
			if((usersList.get(i).getType() +"/"+ usersList.get(i).getUsername()).equals(typeAndUsername))
				return i;
		}
		return -1;
	}
	
	//DONE
	public void newChannelEvent(String channel){
			//System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		for (int i=0 ; i<list.length; i++){
			//System.out.println("ENTROU");
			if(list[i].contains("channel=")){
				channel=list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1));
				String[] aux = channel.split("-");
					//System.out.println(getIndexOfUsername(aux[0]));
				usersList.get(getIndexOfUsername(aux[0])).setActiveCallChannel(channel);
					//System.out.println(usersList.get(getIndexOfUsername(aux[0])).getActiveCallChannel());
				break;
			}			
		}								
	}
	
	//DONE
	private void bridgeEvent(String channel) {
			//System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		ArrayList<String> usersBridged = new ArrayList<String>();
		
		for (int i=0 ; i<list.length; i++){
			//System.out.println("ENTROU");
			if(list[i].contains("channel")){
				channel=list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1));
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
						if(usersList.get(u).getActiveCallUsername()==null){
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

	// DONE
	//TODO Resolve bug Anonymous call
	public void call(String callerOriginater, String callerDestination){
		//TODO Change the "internal" parameter and "timeout-20000" to CONSTANTS
		asteriskServer.originateToExtension(callerOriginater, "internal", callerDestination, 1, 20000);		
	}
	
   /* public static void main(String[] args) throws Exception
	{
    	AsteriskService2 manager = new AsteriskService2();			
        manager.managerConnection.login();
        //manager.periodicActions();
        Thread.sleep(150000);
        //manager.printUsers();
        //manager.call(manager.usersList.get(1).getType()+"/"+manager.usersList.get(1).getUsername(), "1000");
        //manager.call("sip/1001", "1000"); //Originate caller has to include the channel type
        //manager.managerConnection.logoff();
        //Thread.sleep(20000);
        //System.out.println(manager.usersList.get(0).getActiveCallUsername());
        //Thread.sleep(5000);
        
	}*/

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
		
    	//TODO NewChannelEvent    	
		if(eventContent[0].contains("NewChannelEvent"))
    		newChannelEvent(eventContent[1]);
    	
    	//TODO BridgeEvent
		if(eventContent[0].contains("BridgeEvent"))
    		bridgeEvent(eventContent[1]);
    	
		//TODO PeerStatusEvent
		if(eventContent[0].contains("PeerStatusEvent")){
			peerStatusEvent(eventContent[1]);
			//System.out.println(usersList.get(0).getUsername());
			System.out.println(usersList.size());
			
		}
		
		//TODO UnlinkEvent
		
    	
		//TODO Register User Event --requires to analyse the event handling

    	
	}

	
	
}
