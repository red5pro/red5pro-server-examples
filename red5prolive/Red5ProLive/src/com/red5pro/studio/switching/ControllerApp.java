package com.infrared5.red5pro.studio.switching;
import java.util.Map;

//com.infrared5.red5pro.studio.switching.ControllerApp
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.slf4j.Logger;

import com.infrared5.red5pro.studio.feed.AudioParser;
import com.infrared5.red5pro.studio.feed.ICYStream;
import com.infrared5.red5pro.studio.feed.IceCast;
import com.infrared5.red5pro.studio.feed.Receiver;

public class ControllerApp extends MultiThreadedApplicationAdapter {
	
	private static Logger log = Red5LoggerFactory.getLogger(ControllerApp.class);
	private IceCast iceCast;

	public boolean appStart(IScope scope){			
		super.appStart(scope);
	
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(30000);
					
					iceCast = new IceCast();
					
					iceCast.start("http://live-aacplus-64.kexp.org/kexp64.aac", scope);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}}).start();

		
		return true;
		
	}
	
	public boolean appConnect(IConnection conn, Object[] params){
		super.appConnect(conn, params);
		return true;
	}
	
	public boolean roomStart(IScope room){
		super.roomStart( room);
		log.info("room start"); 
		MediaSwitch switcher = new MediaSwitch(room);
		switcher.start();
		return true;
	}
	public void roomStop(IScope room){
		MediaSwitch.close(room.getContextPath()); 
		log.info("room stop"); 
		super.roomStop( room);
		
	}	
	public void streamBroadcastStart(IBroadcastStream stream){
		super.streamBroadcastStart(stream);
		
		log.info("stream Broadcast Start {}",stream.getPublishedName()); 
		if(stream.getPublishedName().equals("media")){
			return;
		}
		MediaSwitch switcher = MediaSwitch.getSwitch(stream.getScope().getContextPath());
		switcher.add(stream.getPublishedName(),stream);
		
	}
	
	public void streamBroadcastClose(IBroadcastStream stream){
		log.info("stream Broadcast Close "); 
		MediaSwitch switcher = MediaSwitch.getSwitch(stream.getScope().getContextPath());
		switcher.remove(stream.getPublishedName());
	}
	
	public void appDisconnect(IConnection conn){
		log.info("app Disconnect"); 
	}
}

