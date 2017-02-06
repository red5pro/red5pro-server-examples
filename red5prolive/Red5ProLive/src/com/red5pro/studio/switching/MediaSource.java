package com.infrared5.red5pro.studio.switching;

import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.message.Constants;

public class MediaSource implements IStreamListener {

	public String name;
	public ISwitchStream output;
	public long creationTime = 0;
	public long streamTime = 0;
	
	
	public MediaSource(IBroadcastStream stream,ISwitchStream output){
		if(stream!=null){
			this.name=stream.getPublishedName();
			this.creationTime=stream.getCreationTime();
		
			stream.addStreamListener(this);
		}
		this.output=output;
	}
	
	@Override
	public void packetReceived(IBroadcastStream arg0, IStreamPacket arg1) {
		if(arg1.getDataType()==Constants.TYPE_AUDIO_DATA){
			streamTime=arg1.getTimestamp();
		}
		
		if(output!=null){
			synchronized(output){
				output.dispatch(this,arg1);
			}
		}

	}

}
