package com.infrared5.red5pro.studio.feed;

import java.util.Map;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.IRTMPEvent;

import com.infrared5.red5pro.studio.switching.MediaSource;
import com.infrared5.red5pro.studio.switching.MediaSwitch;
/**
 * "http://live-aacplus-64.kexp.org/kexp64.aac"
 * @author Andy Shaules
 *
 */
public class IceCast {
	
	private MediaSwitch switcher;
	
	private MediaSource source;

	private ICYStream stream;

	public void start(String uri , IScope scope){
		switcher = new MediaSwitch(scope);
		
		source = new MediaSource(null,switcher);
		source.name="radio";
		source.creationTime=System.currentTimeMillis();
		switcher.add(source);
		
		
		Receiver wrecker = new Receiver(){
			
			AudioParser parser= new AudioParser(this);
			
			@Override
			public void onMetaData(Map<String, Object> meta) {
				System.out.println("onMetaData "+meta.toString());
				
			}

			@Override
			public void reset(String string, String string2) {
				System.out.println("reset " +string + " " +string2);
				if(string.equals("audio")&& string2.equals("aacp")){
					
				}
			}

			@Override
			public void onRawData(byte[] pushIt) {
				parser.onAACData(pushIt,  0);
			}

			@Override
			public void onDisconnected() {
				System.out.println("on Disconnected ");
				switcher.stop();
				
			}

			@Override
			public void dispatchEvent(IRTMPEvent audio) {				
				if(!switcher.isStarted()){
					switcher.start();
					source.creationTime=System.currentTimeMillis();
				}
				source.packetReceived(null, (IStreamPacket) audio);
			}
		};		
				
		stream = new ICYStream("http://live-aacplus-64.kexp.org/kexp64.aac",wrecker);
		
		stream.start();
	}
	
	public void stop(){
		
		if(stream!=null){
			stream.stop();
		}
		if(switcher!=null){
			switcher.stop();
		}
	}
}
