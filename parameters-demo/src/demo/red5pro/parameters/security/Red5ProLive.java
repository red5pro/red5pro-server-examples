package demo.red5pro.parameters.security;
//demo.red5pro.parameters.security.Red5ProLive
import java.util.*;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.scope.IScope ;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.ISubscriberStream;

/**
 * This example application adapter illustrates how to validate a client id.
 * @author Andy Shaules
 *
 */
public class Red5ProLive extends MultiThreadedApplicationAdapter implements IStreamPlaybackSecurity, IStreamPublishSecurity{

	private String secret = "secret"; 
	/**
	 * Application life-cycle begins here
	 */
	public boolean appStart(IScope scope){

		super.appStart(scope);		
		//Step one: Add hooks to handle security.
		//To implement security handlers to prevent access to streams, shared objects, or publishing,
		//register an IStreamSecurity or ISharedObjectSecurity type.
		this.registerStreamPlaybackSecurity(this);
		this.registerStreamPublishSecurity(this);

		return true;
	}



	/**
	 * Called when a client connects to the application.	
	 */
	public boolean appConnect(IConnection conn, Object[] params){

		super.appConnect(conn, params); 

		//The app requires 5 parameters.
		//userId,stream, action, time, digest.
		//begin parameter section	
		if(params!=null && params.length>=5){
			String userId = String.valueOf(params[0]);
			String broadcastId = String.valueOf(params[1]);
			String type = String.valueOf(params[2]);
			String when = String.valueOf(params[3]);
			String digest = String.valueOf(params[4]);

			//how old is this token?
			Long timeWhen = Long.valueOf(when);

			long age = System.currentTimeMillis() - timeWhen;
			//Check the age of the token.
			if(age>30000){//30 seconds for example
				//REJECT CLIENT, OLD COOKIE!
				//return false; 
			}
			//Remake a valid digest with the provided params.
			String digestReformed = recreateDigest(userId,broadcastId,type,when);
			//if it is wrong, it has been altered.
			if(!digestReformed.equals(digest)){
				//REJECT CLIENT, Bad digest! 
				return false;
			}else{				
				//Lets set server-side session attributes.
				conn.setAttribute("userId", userId);
				conn.setAttribute("broadcastId", broadcastId);
				conn.setAttribute("type", type);
				conn.setAttribute("sessionAge", age);
			}


		}else{
			//REJECT CLIENT, NO PARAMS!
			return false;
		}
		//end parameter section	


		//to reject a client, call rejectClient();
		//The Thread local will not return from this call.
		//rejectClient();
		//NOTE: rejectClient(reason)	is not supported by mobile.	
		//you can also reject the client by returning false.
		return true;
	}
	private String recreateDigest(String userId, String broadcastId, String type, String when) {
		String toHash = userId + broadcastId + type + when + secret;
		String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(toHash);  
		log.info("created hash {}",sha256hex); 
		return sha256hex;
	}
	public String createDigest(String type, String broadcastId, String userId){
		long time=System.currentTimeMillis();
		String concated= userId + broadcastId + type + String.valueOf(time)+secret;
		String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(concated);
		return sha256hex+"-"+String.valueOf(time); 
	}


	/**
	 * Called when the client disconnects. This is where clean-up of client associated objects should occur.
	 */
	public void appDisconnect(IConnection conn){
		//If you have a stateful object, release it here.
		if(conn.hasAttribute("statefulObject")){
			@SuppressWarnings("unused")
			Object state = conn.getAttribute("statefulObject");
			//Do something with it.
			// state.release();
			conn.removeAttribute("statefulObject");
		}
	}
	/**
	 * If the client context uri is deeper than the top level application name,
	 * room join will be called for each sub-scope below it.
	 * proto://domain/app_name/room_name/...
	 */
	public boolean roomJoin(IClient client,IScope room){
		//Room Join events pass in the IClient rather than the IConnection.
		//To get the associated IConnection from the IClient, iterate the set.
		Iterator<IConnection> iterator = client.getConnections().iterator();
		while(iterator.hasNext()){
			IConnection theReference = iterator.next();
			if(theReference.hasAttribute("statefulObject")){
				@SuppressWarnings("unused")
				Object theState = theReference.getAttribute("statefulObject");
				//do Something with it.
				//theState.currentRoom = room.getName();
			}
		}

		//Another way to get the IConnection inside a method call is to use Red5 static methods.
		IConnection connection = Red5.getConnectionLocal();
		if(connection.hasAttribute("statefulObject")){
			@SuppressWarnings("unused")
			Object theState = connection.getAttribute("statefulObject");
			//do Something with it.
			//theState.currentRoom = room.getName();
		}
		return true;
	}
	/**
	 * Called when a client begins to publish media or data.
	 */
	public void streamBroadcastStart(IBroadcastStream stream){
		log.info("streamBroadcastStart "); 
		IConnection connection = Red5.getConnectionLocal(); 
		if(connection !=null &&  stream!=null){
			connection.setAttribute("streamStart", System.currentTimeMillis());
			connection.setAttribute("streamName", stream.getPublishedName());
		}
	}

	@SuppressWarnings("unused")
	public void streamSubscriberStart(ISubscriberStream stream){
		log.info("streamSubscriberStart "); 
		String streamName = stream.getBroadcastStreamPublishName();
		IConnection conn = Red5.getConnectionLocal();
		if(conn.hasAttribute("userId")){
			String userId=conn.getAttribute("userId").toString();
			String broadcastId=conn.getAttribute("broadcastId").toString();
			String type=conn.getAttribute("type").toString();
			String age=conn.getAttribute("sessionAge").toString();

			// broadcastId vs actual name

			//Should a client be rejected if choosing different stream than parameters authorized?
			//if(!streamName.equals(broadcastId)){//for demo simplicity sake, lets force it.
			//	conn.setAttribute("broadcastId", streamName);
			//}

			//lets reset age for stat accuracy

		}
		conn.setAttribute("broadcastId", streamName);
		conn.setAttribute("sessionAge",  System.currentTimeMillis()); 

	}


	public void streamSubscriberClose(ISubscriberStream stream){
		log.info("streamSubscriberClose "); 

		//this should match broadcastId
		String streamName = stream.getBroadcastStreamPublishName();


		IConnection conn = Red5.getConnectionLocal();
		if(conn.hasAttribute("userId")){
			String userId=conn.getAttribute("userId").toString();
			String broadcastId=conn.getAttribute("broadcastId").toString();
			String type=conn.getAttribute("type").toString();
			String age=conn.getAttribute("sessionAge").toString();		
			Long ageComputeable = Long.valueOf(age);
			long duration = System.currentTimeMillis() -ageComputeable;

			log.info("User id {}  watched broadcast {}  type:{} for {} seconds.",new Object[]{userId,broadcastId,type,duration/1000.0f});
		}else{
			String age=conn.getAttribute("sessionAge").toString();		
			Long ageComputeable = Long.valueOf(age);
			long duration = System.currentTimeMillis() -ageComputeable;

			log.info("unknown user watched broadcast {} for {} seconds.",new Object[]{streamName,duration/1000.0f});
		}
	}

	public void sendMessageToPublisher(Map<Object,Object> message){
		IConnection conn = Red5.getConnectionLocal();
		String thePublisher=null;
		if(conn.hasAttribute("broadcastId")){
			thePublisher = conn.getAttribute("broadcastId").toString(); 
		}else{//not a subscriber yet
			return;
		}

		Iterator<IConnection> inter = conn.getScope().getClientConnections().iterator();
		while(inter.hasNext()){
			IConnection client = inter.next();
			if(client.hasAttribute("streamName")){
				String theStream = client.getAttribute("streamName").toString(); 
				//this is a publisher. we set this above.
				if(theStream.equals(thePublisher) && client instanceof IServiceCapableConnection){
					((IServiceCapableConnection) client).invoke("subscriberMessage", new Object[]{message}); 
					log.info("sent message");
					return;				
				}
			}
		}

	}

	public void sendMessageToPublisher(String message){
		IConnection conn = Red5.getConnectionLocal();
		String thePublisher=null;
		if(conn.hasAttribute("broadcastId")){
			thePublisher = conn.getAttribute("broadcastId").toString(); 
		}else{//not a subscriber yet
			return;
		}

		Iterator<IConnection> inter = conn.getScope().getClientConnections().iterator();
		while(inter.hasNext()){
			IConnection client = inter.next();
			if(client.hasAttribute("streamName")){
				String theStream = client.getAttribute("streamName").toString(); 
				//this is a publisher. we set this above.
				if(theStream.equals(thePublisher) && client instanceof IServiceCapableConnection){
					((IServiceCapableConnection) client).invoke("subscriberMessage", new Object[]{message}); 
					log.info("sent message");
					return;				
				}
			}
		}

	}


	public List<String> getLiveStreams() {

		Iterator<IClient> iter = scope.getClients().iterator();
		List<String> streams = new ArrayList<String>();

		THE_OUTER:while(iter.hasNext()){
			IClient client = iter.next();
			Iterator<IConnection> cset = client.getConnections().iterator();
			THE_INNER:while(cset.hasNext()){
				IConnection c = cset.next();
				if(c.hasAttribute("streamName")){
					if(!c.isConnected()){
						try{
							c.close();
							client.disconnect();
						}catch(Exception e){

						}
						continue THE_OUTER;
					}

					if(streams.contains(c.getAttribute("streamName").toString()))
						continue THE_INNER;

					streams.add(c.getAttribute("streamName").toString());
				}
			}
		}

		return streams ;
	}
	/**
	 * Check parameters with desired action
	 */
	@Override
	public boolean isPublishAllowed(IScope arg0, String arg1, String arg2) {
		String name = (String) Red5.getConnectionLocal().getAttribute("broadcastId");
		String type = (String) Red5.getConnectionLocal().getAttribute("type");
		if("publish".equals(type) && arg1.equals(name))
			return true;

		return false;
	}
	/**
	 * Check parameters with desired action
	 */
	@Override
	public boolean isPlaybackAllowed(IScope arg0, String arg1, int arg2,int arg3, boolean arg4) {
		String name = (String) Red5.getConnectionLocal().getAttribute("broadcastId");
		String type = (String) Red5.getConnectionLocal().getAttribute("type");
		if("play".equals(type) && arg1.equals(name))
			return true;

		return false;
	}



	public String getSecret() {
		return secret;
	}



	public void setSecret(String secret) {
		this.secret = secret;
	}

}
