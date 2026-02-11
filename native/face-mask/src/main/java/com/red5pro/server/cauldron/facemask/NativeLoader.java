package com.red5pro.server.cauldron.facemask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading native libraries bundled inside the JAR.
 * Extracts libraries to a temporary directory and loads them in the correct order.
 */
public class NativeLoader {

    private static final Logger log = LoggerFactory.getLogger(NativeLoader.class);

    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    private static Path tempDir;

    // Libraries in dependency order
    private static final String[] OPENCV_LIBS = {
        "libopencv_core.so",
        "libopencv_imgproc.so",
        "libopencv_objdetect.so"
    };

    private static final String FACEMASK_LIB = "facemask.so";

    private static final String NATIVE_PATH = "/native/facemask/";

    private NativeLoader() {
        // Utility class
    }

    /**
     * Check if bundled natives are available in the JAR.
     *
     * @return true if natives are bundled
     */
    public static boolean hasBundledNatives() {
        return NativeLoader.class.getResource(NATIVE_PATH + FACEMASK_LIB) != null;
    }

    /**
     * Load all bundled native libraries. Libraries are extracted to a temp
     * directory and loaded in dependency order. The temp directory is marked
     * for deletion on JVM exit.
     *
     * @return path to the extracted facemask.so
     * @throws IOException if extraction or loading fails
     */
    public static String loadBundledNatives() throws IOException {
        if (loaded.get()) {
            log.info("Native libraries already loaded");
            return getTempDir().resolve(FACEMASK_LIB).toString();
        }

        synchronized (NativeLoader.class) {
            if (loaded.get()) {
                return getTempDir().resolve(FACEMASK_LIB).toString();
            }

            log.info("Loading bundled native libraries from JAR");

            // Create temp directory
            tempDir = Files.createTempDirectory("facemask-natives-");
            tempDir.toFile().deleteOnExit();
            log.info("Extracting natives to: {}", tempDir);

            // Extract and load OpenCV libs in order
            for (String lib : OPENCV_LIBS) {
                extractAndLoad(lib);
            }

            // Extract facemask (loaded later by IProcess)
            extractLibrary(FACEMASK_LIB);

            loaded.set(true);
            log.info("Native libraries loaded successfully");

            return tempDir.resolve(FACEMASK_LIB).toString();
        }
    }

    /**
     * Get paths to extracted OpenCV support libraries.
     *
     * @return array of paths to OpenCV .so files
     */
    public static String[] getExtractedSupportLibs() {
        if (tempDir == null) {
            return new String[0];
        }
        String[] paths = new String[OPENCV_LIBS.length];
        for (int i = 0; i < OPENCV_LIBS.length; i++) {
            paths[i] = tempDir.resolve(OPENCV_LIBS[i]).toString();
        }
        return paths;
    }

    /**
     * Get path to extracted facemask module.
     *
     * @return path to facemask.so or null if not extracted
     */
    public static String getExtractedModulePath() {
        if (tempDir == null) {
            return null;
        }
        return tempDir.resolve(FACEMASK_LIB).toString();
    }

    private static Path getTempDir() {
        return tempDir;
    }

    private static void extractAndLoad(String libName) throws IOException {
        Path libPath = extractLibrary(libName);
        log.info("Loading library: {}", libPath);
        System.load(libPath.toString());
    }

    private static Path extractLibrary(String libName) throws IOException {
        String resourcePath = NATIVE_PATH + libName;
        Path targetPath = tempDir.resolve(libName);

        try (InputStream is = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Native library not found in JAR: " + resourcePath);
            }
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            // Mark for deletion on exit
            targetPath.toFile().deleteOnExit();
        }

        log.debug("Extracted {} to {}", libName, targetPath);
        return targetPath;
    }
}
