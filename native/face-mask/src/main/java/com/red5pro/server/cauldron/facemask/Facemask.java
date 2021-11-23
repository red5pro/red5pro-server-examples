package com.red5pro.server.cauldron.facemask;

import java.io.File;
import java.util.Map.Entry;
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
import com.red5pro.override.cauldron.ProcessConfiguration.ProcessReturn;
import com.red5pro.override.cauldron.ProcessConfiguration.ProcessTiming;
import com.red5pro.override.cauldron.ProcessConfiguration.ProcessType;
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
	
	public Facemask(){
		MediaProcessor.addProcessListener(this);
	}

	public String getName(){
		return "Facemask";
	}

	public void doStart(){

		try {
//X:\red5pro\red5pro-server-develop.b3108-feature\red5pro-server-develop.b3108-feature\nativedevelop
			configContext = new FileSystemXmlApplicationContext(new String[] { "/${red5.root}/plugins/facemask/module-facemask.xml" }, true);
			if(configContext!=null){
				
				configuration = (ModuleConfig) configContext.getBean("config");
				
				brewery = (Brewery)configContext.getBean("brew");
			}
		} catch (Exception e) {
			log.warn("Exception in doStart", e);
		}


		javaLoaded.set(true); 

		deploy();

	}

	public void doStop(){
		MediaProcessor.removeProcessListener(this);
	}

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
			log.info("cannot loading module yet  {}   {}", loader, allReady.get()); 
		}
	}

	@Override
	public void cauldronLibStarted(IProcess loader) {		
		log.warn("Cauldron Lib Started");

		this.loader=loader;

		deploy();


	}

	@Override
	public void streamProcessorError(IProStream stream, Exception error) {
		log.warn("streamProcessorError  {}  {}", stream.getBroadcastStreamPublishName(),error);

	}

	@Override
	public void streamProcessorStart(IProStream stream) {	
		log.warn("streamProcessorStart  {}", stream.getName());
		stream.setProcessorClass("com.red5pro.media.transform.codec.AudioCapableProcessor"); 
		ProcessConfiguration config = new ProcessConfiguration();
		config.processTiming=ProcessTiming.WAIT.ordinal();
		stream.setProcessConfiguration(config );
		config.processReturnType = ProcessReturn.IMAGE.ordinal();
		config.processType = ProcessType.ENCODE.ordinal();
		
		String guidString = brewery.getPotion();
		Potion potion = new Potion(guidString);
		
		for(Entry<String, Object> e:brewery.getIngredients().entrySet()){
			 potion.add(new Ingredient(e.getKey(), e.getValue()));
		}		
		stream.setPotion(potion);

	}

	@Override
	public void streamProcessorStop(IProStream stream) {
		log.warn("streamProcessorStop {} ",stream.getName());

	}

	public void deploy(){

		if(!javaLoaded.get()){
			log.warn("waiting for java");
			return;
		}
		
		if(configuration!=null &&  nativeDeployed.compareAndSet(false, true)){	

		    if(configuration.getSupportLibs()!=null){
			//First load support libs via system load lib call;
    			for(String platform:configuration.getSupportLibs()){
    				log.info("Loading {}", platform); 
    				
    				File f = new File(platform);
    
    				try {//attempt to extract it.
    					log.info("Loading {}  exists {}", f.getAbsolutePath(), f.exists()); 
    					System.load(f.getAbsolutePath()); 
    				} catch (Exception e) {
    					log.warn("Exception in deploy", e);
    				}
    			}
		    }
			
			allReady.set(true); 
		}
		
		loadModule();

	}

}
