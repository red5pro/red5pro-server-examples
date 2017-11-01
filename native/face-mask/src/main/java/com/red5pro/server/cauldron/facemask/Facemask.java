package com.red5pro.server.cauldron.facemask;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.server.plugin.Red5Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.red5pro.override.IProStream;
import com.red5pro.override.cauldron.IProcess;
import com.red5pro.override.cauldron.MediaProcessor;
import com.red5pro.override.cauldron.MediaProcessorAware;
import com.red5pro.override.cauldron.ProcessConfiguration;
import com.red5pro.override.cauldron.ProcessConfiguration.ProcessTiming;
import com.red5pro.override.cauldron.brews.Ingredient;
import com.red5pro.override.cauldron.brews.Potion;
//com.red5pro.server.cauldron.facemask.Facemask



/**
 * This class loads supports libs and then loads module at startup.
 * <p>
 * 1) Plugin startup loads configs required.
 * 2) The deploy method loads support libs from config.
 * 3) MediaProcessorAware handlers receives module loader.
 * 4) After both configs and loader are present, plugin calls loader to load the module. 
 * <p>TODO mvn assembly plugin to write module-${config.file} and utilize LIbraryLoader for support binaries.
 * @author Andy Shaules
 *
 */
public class Facemask extends Red5Plugin implements MediaProcessorAware{

	private static Logger log = LoggerFactory.getLogger(Facemask.class);
	
	private FileSystemXmlApplicationContext configContext;  
	
	/**support libs loaded by JVM*/
	AtomicBoolean nativeDeployed = new AtomicBoolean();
	
	/** config present and jvm loaded deps*/
	AtomicBoolean allReady = new AtomicBoolean();
	
	/**config present.*/
	AtomicBoolean javaLoaded = new AtomicBoolean();
	
	/**the module loader.*/
	private IProcess loader;
	
	/**potion guid contained in loaded binary*/
	private long guid = 0;
	
	/**binary config*/
	private ModuleConfig configuration;
	
	/**process parameters for native code.*/
	private Brewery brewery;
	
	
	/**
	 * 1) We add ourself as a ProcessAwareListener
	 */
	public Facemask(){
		
		MediaProcessor.addProcessListener(this);
	}

	
	/**
	 * Our red5 plugin name
	 */
	public String getName(){
		return "Facemask";
	}

	
	/**
	 * Red5 Plugin Start is called. 
	 */
	public void doStart(){

		try {
			//look up our XML configuratiion
			configContext = new FileSystemXmlApplicationContext(new String[] { "/${red5.root}/plugins/native/facemask/module-facemask.xml" }, true);
			
			if(configContext!=null){
				//Get our basic configuration parameters for re-encoding. bandwidth and framerate
				configuration = (ModuleConfig) configContext.getBean("config");
				
				//Get our brewery for our Potion. 
				brewery = (Brewery)configContext.getBean("brew");
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		javaLoaded.set(true); 

		deploy();

	}
	
	/**
	 * 2) Deploy Native dependencies our C++ needs. 
	 * 
	 */
	public void deploy(){
		
		//No configuration..
		if(!javaLoaded.get()){
			log.warn("waiting for java");
			return;
		}
		
		//lets deploy binaries
		if(configuration!=null &&  nativeDeployed.compareAndSet(false, true)){	

			//First load support libs via system load lib call;
			for(String platform:configuration.getSupportLibs()){
				log.info("Loading {}", platform); 
				
				File f = new File(platform);

				try {//attempt to extract it.
					log.info("Loading {}  exists {}", f.getAbsolutePath(), f.exists()); 
					
					//Load OPEN CV
					System.load(f.getAbsolutePath()); 
				
				
				
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//done loading open CV
			allReady.set(true); 
		}
		
		
		//Try to Load the native module
		loadModule();

	}
	/**
	 * 3) Cauldron Library is Ready, We will load our video module with it.
	 */
	@Override
	public void cauldronLibStarted(IProcess loader) {		
		
		log.warn("cauldron Lib Started");
		
		//This is our video module Loader.
		this.loader=loader;

		
		deploy();


	}
	
	
	/**
	 * 4) If everything is ready, we will finally Load the native shared lib / module. 
	 */

	private void loadModule(){	
		
		if(loader!=null && allReady.get()){
			log.info("loading module"); 
			//construct expected module guid.
			String guidString = brewery.getPotion();
			if(guidString.length()<4){
				guidString=guid+"    ";//pad it out
			}
			int expected  =  guidString.charAt(3); 
			expected  = expected<<8 | guidString.charAt(2); 
			expected  = expected<<8 | guidString.charAt(1); 
			expected  = expected<<8 | guidString.charAt(0); 
				
			//load it up ..
			guid  = loader.loadLibrary(configuration.getModuleFile());
			//this brew for this binary?
			if(expected!=guid){
				log.error("wrong brew guid {}  {}", expected,guid);
				// wrong native plugin ?
			}
		}else{
			log.info("cannot loading module yet"); 
		}
	}



	/**
	 * 5) Stream Time! We configure a potion and set it to the stream.
	 * 
	 */
	

	@Override
	public void streamProcessorStart(IProStream stream) {	
		
		// we care about only streams named asd in any room or scope. 
		if(!stream.getPublishedName().equals("asd")){
			return;
		}
				
		log.warn("streamProcessorStart  {}", stream.getName());
		
		stream.setProcessorClass("com.red5pro.media.transform.codec.AVCProcessor"); 
		
		ProcessConfiguration config = new ProcessConfiguration();

		//Because our native lib returns an edited image...
		config.processReturn=ProcessReturnType.IMAGE.ordinal();
		
		//...we want to decode and re-encode,
		config.processType= ProcessType.ENCODE.ordinal();

		// and we want the stream to wait for remuxing.
		config.processTiming=ProcessTiming.WAIT.ordinal();
		//set it
		stream.setProcessConfiguration(config ); 
		
		//We want to use our MASK four-cc processor
		Potion potion = new Potion("MASK");
		//we have an image resource.
		potion.add(new Ingredient("mask_image", brewery.getIngredients().get("mask_image")));
		//we have a cascade sheet from openCV to load.
		potion.add(new Ingredient("cascade_sheet", brewery.getIngredients().get("cascade_sheet")));
		//we set the potion!
		stream.setPotion(potion);

	}
	/**
	 * 6) end of session.
	 */
	@Override
	public void streamProcessorStop(IProStream stream) {
		log.warn("streamProcessorStop {} ",stream.getName());

	}

	
	/**
	 * Oh NO! error!
	 */
	@Override
	public void streamProcessorError(IProStream stream, Exception error) {
		log.warn("streamProcessorError  {}  {}", stream.getBroadcastStreamPublishName(),error);

	}
	
	/**
	 * Red5 Plugin stopped.
	 */
	public void doStop(){
		MediaProcessor.removeProcessListener(this);
	}

}
