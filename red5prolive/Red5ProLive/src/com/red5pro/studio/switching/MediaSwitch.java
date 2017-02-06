package com.infrared5.red5pro.studio.switching;

import java.util.HashMap;
import java.util.Map;

import org.red5.codec.AACAudio;
import org.red5.codec.StreamCodecInfo;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IProviderService;
import org.slf4j.Logger;

public class MediaSwitch implements ISwitchStream{
	
	private static Logger log = Red5LoggerFactory.getLogger(MediaSwitch.class);

	private static Map<String,MediaSwitch> rooms = new HashMap<>();
	
	private Map<String,MediaSource>content = new HashMap<String,MediaSource>();
	
	private String currentOutput = null;
	
	private String previousOutput = null;
	
	private ClientBroadcastStream output;
	
	private boolean waitingForKey = true;

	private IScope scope;

	private long startTime=0;

	public MediaSwitch(IScope room){
		this.scope=room;
		rooms.put(room.getContextPath(), this);
	}
	public boolean isStarted(){
		return output!=null;
	}
	public void start(){
		//start stream.
		output=new ClientBroadcastStream();
		output.setRegisterJMX(false);
		this.startTime = System.currentTimeMillis();
		IStreamCapableConnection emptyClient = new EmptyClient(scope);
		IConnection current = Red5.getConnectionLocal();
		log.info("current {}  {}  ",current,scope); 
		Red5.setConnectionLocal(emptyClient);
		output.setConnection(emptyClient ); 
		output.setName("media"); 
		output.setPublishedName("media"); 
		output.setBroadcastStreamPublishName("media"); 
		
		
		IContext context = scope.getContext();
		
		IProviderService providerService = (IProviderService) context.getBean(IProviderService.BEAN_NAME);		
		
		if (providerService.registerBroadcastStream(scope, output.getPublishedName(), output)) {
			log.info("The stream {}, did register ",output.getPublishedName());
			//bsScope = (BroadcastScope) providerService.getLiveProviderInput(conn.getScope(), name, true);
			IBroadcastScope bsScope = scope.getBroadcastScope(output.getPublishedName());	
			bsScope.setClientBroadcastStream(output);
			StreamCodecInfo info= new StreamCodecInfo();
			info.setHasAudio(true);
			info.setAudioCodec(new AACAudio());
			output.setCodecInfo(info);
			output.setScope(scope);
			output.setStreamId(1); 
			output.start();
			output.startPublishing();
			log.info("publishing {}",output.getBroadcastStreamPublishName());  
		}
		Red5.setConnectionLocal(current);
	}

	private void closeSwitch() {
		
		if(output!=null){
			IProviderService providerService = (IProviderService) scope.getContext().getBean(IProviderService.BEAN_NAME);		
			providerService.unregisterBroadcastStream(scope, output.getPublishedName(),output); 
			output.close();
			output=null;
		}
		
	}
	public void add(MediaSource source){
		content.put(source.name,source);
		if(this.currentOutput==null){
			log.info("switching to  {}   ", source.name); 
			this.switchTo(source.name);
		}
	}
	public void add(String name, IBroadcastStream stream){
		MediaSource source = new MediaSource(stream, this); 
		log.info("adding {}   {}", name, stream.getPublishedName()); 
		if(stream.getPublishedName().equals(output.getPublishedName())){
			return;
		}
		content.put(name,source);
		if(this.currentOutput==null){
			log.info("switching to  {}   ", name); 
			this.switchTo(name);
		}
	}
	
	public boolean switchTo(String name){
		log.info("switch to {}" , name); 
		if(content.containsKey(name)){
			previousOutput=currentOutput;
			currentOutput=name;
			waitingForKey=true;
			return true;
		}else{
			log.info("switch not found {}" , name); 
		}
		return false; 
	}
	
	public void remove(String name){
		if(content.containsKey(name)){
			content.remove(name);
			if(name.equals(currentOutput)){
				currentOutput=null;
			}
			if(name.equals(previousOutput)){
				previousOutput=null;
			}
		}
	}
	@Override
	public void dispatch(MediaSource source, IStreamPacket arg1) {
		
		if(source.name.equals(currentOutput)){
			log.trace("dispatch {}",source.name);
			long started = startTime;
			long sourceStarted = source.creationTime;
			long offset = started - sourceStarted;
			long packetTime = arg1.getTimestamp() - offset;
			log.trace("{}  {}  ",arg1.getTimestamp() , packetTime );
			if(changeTimeStamp(arg1,packetTime, true)){
				//switch vid  on key
				if(waitingForKey){
					log.trace("waitingForKey {}",source.name);
				}else{
					log.trace("send vid {}",source.name);
					output.dispatchEvent((IEvent) arg1); 
				}
			}else{//audio or notify switch now
				log.trace("send aud {}",source.name);
				output.dispatchEvent((IEvent) arg1); 
			}
			
		}else if(waitingForKey && source.name.equals(previousOutput)){
			long started = startTime;
			long sourceStarted = source.creationTime;
			long offset = started - sourceStarted;
			long packetTime = arg1.getTimestamp() - offset;
			log.trace("{}  {}  ",arg1.getTimestamp() , packetTime );
			if(changeTimeStamp(arg1,packetTime, false)){
				//switch on key
				if(waitingForKey){//send vid until a key from new stream hits. 
					output.dispatchEvent((IEvent) arg1); 
				
				}else{
					//done
				}
			}else{
				//nothing
			}
		}else{
			log.trace("ignoring {}",source.name);
		}

	}
/**
 * 
 * @param packet the packet to change
 * @param toTime time to set in packet
 * @param isTarget this packet is from switch target
 * @return true if video data
 */

	public boolean changeTimeStamp( IStreamPacket arg1, long toTime,boolean isTarget){
		if(arg1 instanceof VideoData){
			VideoData vid = (VideoData)arg1;
			vid.setTimestamp((int) toTime);
			if(isTarget){
				if(vid.getFrameType()==FrameType.KEYFRAME){
					this.waitingForKey=false;
				}
			}
			return true;
		}else if(arg1 instanceof AudioData){
			AudioData aud = (AudioData)arg1;
			aud.setTimestamp((int) toTime);
		}else if(arg1 instanceof Notify){
			Notify not = (Notify)arg1;
			not.setTimestamp((int) toTime);
		}
		return false;
	}
	public static void close(String path){
		if(rooms.containsKey(path)){
			 MediaSwitch streamer = rooms.get(path);
			 streamer.closeSwitch();
		}
	}


	public static MediaSwitch getSwitch(String contextPath) {
		return rooms.get(contextPath);
		
	}
	public void stop() {
		
		close(scope.getContextPath());
	}

	
}
