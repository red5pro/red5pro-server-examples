package com.infrared5.red5pro.live;
//com.infrared5.red5pro.live.Red5ProLive
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.springframework.core.io.Resource;


/**
 * This example application adapter illustrates how to hook the stream end event in order to convert the flv to mp4..
 * @author Andy Shaules
 *
 */
public class Red5ProLive extends MultiThreadedApplicationAdapter {

	/**
	 * Application life-cycle begins here
	 */
	public boolean appStart(IScope scope){

		super.appStart(scope);		
		//Return false to prevent app from starting for any reason
		return true;
	}
	/**
	 * Here is the main hook. We create a thread to wait and allow the file writer to finish the flv write-out.
	 */
	public void streamBroadcastClose(IBroadcastStream stream){
		log.info("Stream closed {}", stream.getSaveFilename());
		
		if(stream.getSaveFilename()!=null){
			final String name = stream.getPublishedName();
			final String theFlv = stream.getSaveFilename();
			final IScope fileScope = stream.getScope();
			
			new Thread(new Runnable(){

				@Override
				public void run() {
					//let the server finish writing the file.					
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					} 
					
					//get the recorded file
					Resource res = fileScope.getResource(theFlv); 
					
					if(res!=null){
						
						try {//Get the path for command line.
							//the recorded file location
							String inputLocation= "PATH_TO_RED5_ROOT/webapps/live/streams/";
							String filePath= inputLocation+name+".flv";//or use 'theFlv'
							String outputLocation = "PATH_TO_RED5_ROOT/webapps/live/";
							log.info("Recorded file {}",filePath);
							String outputFile = outputLocation + name +".mp4";
							//delete any previous file created, or ffmpeg will complain.
							File checker = new File(outputFile);
							if(checker.exists()){
								checker.delete();
							}
							String ToolPath="ffmpeg";
							
							String[] commandFixed = {
									ToolPath,
									"-i",filePath,
									//additional arguments
									outputFile
							};
							//build the process
							Process p = Runtime.getRuntime().exec(commandFixed);
							//read the ffmpeg data or it will not finish the job.
							SyncPipe errors = new SyncPipe(p.getErrorStream());							
							SyncPipe infos = new SyncPipe(p.getInputStream());
							new Thread(errors).start();
							new Thread(infos).start();
							//let this thread wait for the process to exit.
							int returnCode = p.waitFor();
							//check that there is a file created afterwards.
							checker = new File(outputFile); 
							
							if(!checker.exists() || checker.length() == 0){								
								log.error("Error processing" + returnCode);
							}
							
							
						} catch (IOException e) {
						
							e.printStackTrace();
						} catch (InterruptedException e) {
							
							e.printStackTrace();
						}
					}					
					
				}}).start();			
		}	
	}

	static class SyncPipe implements Runnable
	{
		private final InputStream istrm;


		public SyncPipe(InputStream isg) throws IOException {
			istrm = isg;	
		}
		
		public void run() {
			try
			{

				int in = -1;
				while ((in = istrm.read()) != -1) {
					
				       System.out.print((char)in);  
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();			
			}
		}
	}
}
