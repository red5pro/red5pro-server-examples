package com.red5pro.server.cauldron.facemask;

import java.io.File;
import java.io.IOException;
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

/**
 * This class loads supports libs and then loads module at startup.
 * <p>
 * 1) Plugin startup loads configs required.
 * 2) The deploy method loads support libs from config or bundled JAR.
 * 3) MediaProcessorAware handlers receives module loader.
 * 4) After both configs and loader are present, plugin calls loader to load the module.
 *
 * @author Andy Shaules
 */
public class Facemask extends Red5Plugin implements MediaProcessorAware {

    private static Logger log = LoggerFactory.getLogger(Facemask.class);

    private FileSystemXmlApplicationContext configContext;

    /** Support libs loaded by JVM */
    AtomicBoolean nativeDeployed = new AtomicBoolean();

    /** Config present and JVM loaded deps */
    AtomicBoolean allReady = new AtomicBoolean();

    /** Config present */
    AtomicBoolean javaLoaded = new AtomicBoolean();

    /** The module loader */
    private IProcess loader;

    /** Potion guid contained in loaded binary */
    private long guid = 0;

    /** Binary config */
    private ModuleConfig configuration;

    /** Process parameters for native code */
    private Brewery brewery;

    /** True if using bundled natives from JAR */
    private boolean usingBundledNatives = false;

    /** Path to module file (may be overridden for bundled natives) */
    private String moduleFilePath;

    public Facemask() {
        MediaProcessor.addProcessListener(this);
    }

    public String getName() {
        return "Facemask";
    }

    public void doStart() {
        // Check for bundled natives first
        if (NativeLoader.hasBundledNatives()) {
            log.info("Bundled native libraries detected in JAR");
            try {
                moduleFilePath = NativeLoader.loadBundledNatives();
                usingBundledNatives = true;
                log.info("Bundled natives loaded, module path: {}", moduleFilePath);
            } catch (IOException e) {
                log.error("Failed to load bundled natives, falling back to config", e);
                usingBundledNatives = false;
            }
        }

        try {
            configContext = new FileSystemXmlApplicationContext(
                new String[] { "/${red5.root}/plugins/native/facemask/module-facemask.xml" }, true);
            if (configContext != null) {
                configuration = (ModuleConfig) configContext.getBean("config");
                brewery = (Brewery) configContext.getBean("brew");

                // If not using bundled natives, use config path
                if (!usingBundledNatives) {
                    moduleFilePath = configuration.getModuleFile();
                }
            }
        } catch (Exception e) {
            log.warn("Exception in doStart", e);
            // If config loading fails but we have bundled natives, create default brewery
            if (usingBundledNatives && brewery == null) {
                log.info("Using default brewery configuration for bundled mode");
                brewery = createDefaultBrewery();
            }
        }

        javaLoaded.set(true);
        deploy();
    }

    public void doStop() {
        MediaProcessor.removeProcessListener(this);
    }

    private Brewery createDefaultBrewery() {
        Brewery b = new Brewery();
        b.setPotion("MASK");
        return b;
    }

    private void loadModule() {
        if (loader != null && allReady.get()) {
            log.info("Loading module from: {}", moduleFilePath);

            // Construct expected module guid
            String guidString = brewery.getPotion();
            if (guidString.length() < 4) {
                guidString = guid + "    "; // pad it out
            }
            int expected = guidString.charAt(3);
            expected = expected << 8 | guidString.charAt(2);
            expected = expected << 8 | guidString.charAt(1);
            expected = expected << 8 | guidString.charAt(0);

            // Load it up
            guid = loader.loadLibrary(moduleFilePath);

            // This brew for this binary?
            if (expected != guid) {
                log.error("Wrong brew guid {} {}", expected, guid);
            }
        } else {
            log.info("Cannot load module yet - loader: {}, allReady: {}", loader, allReady.get());
        }
    }

    @Override
    public void cauldronLibStarted(IProcess loader) {
        log.info("Cauldron Lib Started");
        this.loader = loader;
        deploy();
    }

    @Override
    public void streamProcessorError(IProStream stream, Exception error) {
        log.warn("streamProcessorError {} {}", stream.getBroadcastStreamPublishName(), error);
    }

    @Override
    public void streamProcessorStart(IProStream stream) {
        log.info("streamProcessorStart {}", stream.getName());
        stream.setProcessorClass("com.red5pro.media.transform.codec.AudioCapableProcessor");

        ProcessConfiguration config = new ProcessConfiguration();
        config.processTiming = ProcessTiming.WAIT.ordinal();
        stream.setProcessConfiguration(config);
        config.processReturnType = ProcessReturn.IMAGE.ordinal();
        config.processType = ProcessType.ENCODE.ordinal();

        String guidString = brewery.getPotion();
        Potion potion = new Potion(guidString);

        if (brewery.getIngredients() != null) {
            for (Entry<String, Object> e : brewery.getIngredients().entrySet()) {
                potion.add(new Ingredient(e.getKey(), e.getValue()));
            }
        }
        stream.setPotion(potion);
    }

    @Override
    public void streamProcessorStop(IProStream stream) {
        log.info("streamProcessorStop {}", stream.getName());
    }

    public void deploy() {
        if (!javaLoaded.get()) {
            log.info("Waiting for java initialization");
            return;
        }

        if (nativeDeployed.compareAndSet(false, true)) {
            if (usingBundledNatives) {
                // Bundled natives already loaded by NativeLoader
                log.info("Using bundled native libraries (already loaded)");
                allReady.set(true);
            } else if (configuration != null && configuration.getSupportLibs() != null) {
                // Load support libs from config paths
                for (String platform : configuration.getSupportLibs()) {
                    log.info("Loading {}", platform);
                    File f = new File(platform);
                    try {
                        log.info("Loading {} exists {}", f.getAbsolutePath(), f.exists());
                        System.load(f.getAbsolutePath());
                    } catch (Exception e) {
                        log.warn("Exception loading native library", e);
                    }
                }
                allReady.set(true);
            } else {
                log.warn("No native library configuration available");
            }
        }

        loadModule();
    }
}
