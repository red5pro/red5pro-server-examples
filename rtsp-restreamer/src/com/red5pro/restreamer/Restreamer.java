package com.red5pro.restreamer;
import java.io.IOException;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.ClientBroadcastStream;

import com.red5pro.server.stream.util.restreamer.ClientHandler;
import com.red5pro.server.stream.util.restreamer.ConnectorShell;
import com.red5pro.server.stream.util.restreamer.MediaPacket;
import com.red5pro.server.stream.util.restreamer.RTSPCameraClient;


/**
 * This example application adapter illustrates using the rtsp client.
 * @author Andy Shaules
 *
 */
public class Restreamer extends MultiThreadedApplicationAdapter {

	RTSPCameraClient test ;
	/**
	 * Application life-cycle begins here
	 */
	public boolean appStart(IScope scope){

		super.appStart(scope);

		// RTSP re-streamer usage
		// We have two threads, one which is pulling from the ip camera, 
		// and another which is pushing through the broadcast stream.
		// The RTSPCameraClient requires a ClientHandler to be useful.
		// We create a generic instance of the ClientHandler in-line.
		// Here is the first thread. 
		new Thread(new Runnable(){


			@Override
			public void run() {
				//This demo waits ten seconds after startup, and begins pulling from an ip camera.
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// imported from red5pro jar.				
				test = new RTSPCameraClient();
				// [host][path][stream name]
				// the test model used for this demo has a uri of:
				// rtsp://192.168.0.117/stream1
				// the demo camera has no path element in its uri.
				test.setHost("192.168.0.117");
				test.setContextPath("");
				test.setStreamName("stream1");
				// camera uses default rtsp port
				test.setPort(554); 
				
				// create the generic handler.
				test.setClient(new ClientHandler(){
					
					// server responded with 200 OK for play request after setup completed.
					public void playbackBegin(RTSPCameraClient arg0) {
						log.info("rtsp client playback begin");
						
					}
					// RTSPCameraClient exited normally after close.
					public void playbackEnd(RTSPCameraClient arg0) {
						log.info("rtsp client playback end");
						
					}
					// unexpected server closure or stream parse error.
					public void streamError(RTSPCameraClient arg0) {
						log.info("rtsp client stream Error");
						
					}
					// No such host or un-reachable host.
					public void unknownHostError(RTSPCameraClient arg0) {
						log.info("rtsp client unknown Host Error");
						
					}});
				
				// Here is the second thread which actually drives the RTSPCameraClient
				Thread thread = new Thread(test);
				// Before negotiation with the camera for media, 
				// we prepare the broadcast stream to make the media available to subscribers.
				// Using a generic factory method, 
				// we create a broadcast client facade and receive the would-be client's broadcast stream.
				ClientBroadcastStream broadcastStream = ConnectorShell.Connect(scope,"stream1");
				// startup internals
				broadcastStream.start();
				try {//lets force a test recording.
					broadcastStream.saveAs("stream1.flv", false);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
				//and commit to the broadcast! 
				broadcastStream.startPublishing();
			
				//start the rtsp client
				thread.start();
				
				//This demo will cut off the broadcast after 60000 milliseconds. 
				long startTime=System.currentTimeMillis();
				
				while (System.currentTimeMillis()-startTime < 60000) {					
					// LOOP
					// Here is the heart of the re-streaming.
					// We must poll the camera client for parsed media.
					// if there isn't any, give it a few moments to acquire more.
					MediaPacket p = test.getPackets().poll();
					if (p == null) {
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {					
							break;
						}
					} else {
						// Check what we have.
						// If you had a way to make a snapshot from a video frames,
						// here is where that would be done.
						// If you wanted to pause like DVR, 
						// here is where you would need to manipulate timestamps and queue up/down. 
						// The broadcastStream expects timestamps to increase in real time.
						if(p.frame instanceof VideoData){
							VideoData vd = (VideoData) p.frame;							
							broadcastStream.dispatchEvent(vd);
						}else if(p.frame instanceof AudioData){
							AudioData ad = (AudioData) p.frame;							
							broadcastStream.dispatchEvent(ad);
						}
						
					}
				}
				// We have run the 60000 milliseconds.
				test.stop();
				// Let this thread wait for the other to end.
				try {// up to a second of waiting.
					thread.join(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Dispose of the facade and stream.
				ConnectorShell.Close(broadcastStream); 
				
			//Start the Main thread of the RTSPCameraClient. 	
			}}).start();

		// In about 10 seconds, we should see a broadcast stream become live and persist for about a minute. 
		// there should be a recording left in the streams directory named 'stream1.flv'
		return true;
	}

}