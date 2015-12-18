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
		log.info("Red5 Pro File conversion Demo"); 
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
			final String path = fileScope.getContextPath();
			log.info("path {}",path); 
			
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
							//Where is this happening?
							String where=fileScope.getContextPath();
							//Remove leading slash
							if(where.indexOf("/")==0){
								where=where.substring(1);
							}
							//break it down to rooms and remove the separator.
							String[] paths = where.split("/");
							//grab the app name from the context.
							String appName = paths[0];
							log.info("paths length {}",paths.length);

							//Form the file input location with the app name and streams directory.
							String inputLocation= "PATH_TO_RED5_ROOT/webapps/"+appName+"/streams/";
							//Add the rooms path if there is one.
							for( int i=1;i<paths.length;i++){
								inputLocation=inputLocation+paths[i]+"/";
							}
							//Add the file name to complete the path.
							String filePath= inputLocation+name+".flv";//or use 'theFlv'
							//Form the output path.							
							String outputLocation = "PATH_TO_RED5_ROOT/webapps/"+appName+"/";
							//Here you could potentially recreate the context structure of the rooms that we parsed above.
							//add the room names back onto the path and call makdirs.
							//File checker = new File(outputLocation);
							//checker.mkdirs()
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
