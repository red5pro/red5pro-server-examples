package com.red5pro.live;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;

public class Red5ProLive extends MultiThreadedApplicationAdapter {
	
	public void streamBroadcastStart(IBroadcastStream stream) {
		
		IConnection connection = Red5.getConnectionLocal();
		if (connection != null &&  stream != null) {
			System.out.println("Broadcast started for: " + stream.getPublishedName());
			connection.setAttribute("streamStart", System.currentTimeMillis());
			connection.setAttribute("streamName", stream.getPublishedName());
		}
		
	}

	public List<String> getLiveStreams() {
		
		Iterator<IClient> iter = scope.getClients().iterator();
		List<String> streams = new ArrayList<String>();
		
		THE_OUTER:while(iter.hasNext()) {
			
			IClient client = iter.next();
			Iterator<IConnection> cset = client.getConnections().iterator();
			
			THE_INNER:while(cset.hasNext()) {
				IConnection c = cset.next();
				if (c.hasAttribute("streamName")) {
					if (!c.isConnected()) {
						try {
						 c.close();
						 client.disconnect();
						}
						catch(Exception e) {
							
						}
						continue THE_OUTER;
					}
					
					if (streams.contains(c.getAttribute("streamName").toString())) {
						continue THE_INNER;
					}
					
					streams.add(c.getAttribute("streamName").toString());
				}
			}
		}

		
		return streams;
		
	}
	
}
