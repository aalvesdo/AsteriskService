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


public class AsteriskService implements ManagerEventListener{

	private ManagerConnection managerConnection;
	private AsteriskServer asteriskServer;
	private ArrayList<SipUsers> usersList;

//*********************************************	
	// Constructor
	public AsteriskService(){
		
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
	
	//DONE - to replace with the register events
	public void periodicActions() throws IllegalArgumentException, IllegalStateException, IOException, TimeoutException, InterruptedException{
		usersList.clear();
		managerConnection.sendAction(new SipPeersAction());
	}
	//DONE
	public void printUsers(){
		for(int i=0; i < usersList.size(); i++ )
			System.out.println(usersList.get(i).getType()+"\\"+usersList.get(i).getUsername());		
	}
	
	//DONE
	public void listOnlineUsers(String peer){
		String[] list = peer.replace(',','\n').split("\\n");
									//System.out.println("\n\n\n\n\n"+aux[1]+"\n\n\n\n\n");
		boolean read= false;
		SipUsers user = new SipUsers();
		for(int i=0 ; i<list.length; i++){
			if((list[i].contains("ipaddress")) && !(list[i].substring(10).contains("null")))
				read = true;
			else{
				if((list[i].contains("objectname")) && (read == true))
					user.setUsername(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)));
				else{
					if((list[i].contains("channeltype")) && (read == true)){
						user.setType(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)));
						usersList.add(user);
					}	
				}
			}
		}
	}
	
	private void peerStatusEvent(String status) {
		
		String[] list = status.replace(',','\n').split("\\n");
		//System.out.println("\n\n\n\n\n"+aux[1]+"\n\n\n\n\n");
		boolean read= false;
		SipUsers user = new SipUsers();
		for(int i=0 ; i<list.length; i++){
			if(list[i].contains("peer=")){
				String[] username=list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).split("/");
				user.setUsername(username[1]);
				System.out.println(user.getUsername()+",");
				read = true;
			}else{
				if(read==true){
					if(list[i].contains("channeltype"))
						user.setType(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)));
					
					if(list[i].contains("peerstatus")){
						System.out.println("Entrou Peer status");
						//o erro esta por aqui
						if(!usersList.isEmpty()){
							System.out.println("Nao Esta vazio");
							for(int u=0; u<usersList.size(); u++){
								System.out.println("Size: "+usersList.size());
								System.out.println("entrou");
								if((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered")) && (usersList.get(u).getUsername().equals(user.getUsername()))){
									// Do nothing
									System.out.println("Vai adicionar");
									break;
								}
								else{
									if((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Unregistered")) && (usersList.get(u).getUsername().equals(user.getUsername()))){
										// Erase user from userList;
										System.out.println("Vai remover");
										usersList.remove(u);
										break;
									}else{
										if((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered")) && ( u++ == usersList.size())){
											// Add user to list
											System.out.println("Vai adicionar ultima posicao");
											user.setActiveCallChannel("");
											user.setActiveCallUsername("");
											usersList.add(user);
											//break;
										}
										if( u++ == usersList.size()){
											System.out.println("Avaliacao primeira condicao "+(list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered")));
											System.out.println("u= "+u+" ; u+1= "+(u+1)+" ; size= "+usersList.size() +" ; condicao= "+(u+1 == usersList.size()));
											System.out.println("Avaliacao segunda condicao "+( (u+1) == usersList.size()));
											System.out.println("Nao entrou em nenhum "+((list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1)).equals("Registered")) && ( u++ == usersList.size())));}
									}
								}
							}	
						}else{
							System.out.println("Esta vazio");
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
	
	
	public int getIndexOfUsername(String typeAndUsername){
		for (int i=0; i<usersList.size() ;i++){
			if((usersList.get(i).getType() +"/"+ usersList.get(i).getUsername()).equals(typeAndUsername))
				return i;
		}
		return -1;
	}
	
	
	public void newChannelEvent(String channel){
		System.out.println(channel);
		String[] list = channel.replace(',','\n').split("\\n");
		for (int i=0 ; i<list.length; i++){
			//System.out.println("ENTROU");
			if(list[i].contains("channel=")){
				channel=list[i].substring(list[i].indexOf("'",0)+1,list[i].indexOf("'",list[i].indexOf("'",0)+1));
				String[] aux = channel.split("-");
				//System.out.println(getIndexOfUsername(aux[0]));
				usersList.get(getIndexOfUsername(aux[0])).setActiveCallChannel(channel);
				System.out.println(usersList.get(getIndexOfUsername(aux[0])).getActiveCallChannel());
				break;
			}			
		}								
	}
	
	private void bridgeEvent(String channel) {
		System.out.println(channel);
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
	
    /*public static void main(String[] args) throws Exception
	{
    	AsteriskService manager = new AsteriskService();			
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
    	
		if(eventContent[0].contains("PeerEntryEvent"))
    		listOnlineUsers(eventContent[1]);
		
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
