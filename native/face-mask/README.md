# Face Mask Demo

A Red5 Pro server plugin demonstrating the Brew API (v2) for native video processing. Uses OpenCV's objdetect module for real-time face detection on live video streams.

## Quickstart (Bundled JAR)

This path is the fewest moving parts and is the best way to validate the sample.

1. Build the release JAR:

   ```sh
   mvn clean package -Prelease
   ```

2. Copy the plugin and config files:

   ```sh
   cp target/opencv-facemask.jar /usr/local/red5pro/plugins/
   mkdir -p /usr/local/red5pro/plugins/native/facemask
   cp src/main/resources/module-facemask.xml /usr/local/red5pro/plugins/native/facemask/
   cp src/main/resources/haarcascade_frontalface_alt.xml /usr/local/red5pro/plugins/native/facemask/
   ```

3. Ensure Red5 Pro uses the native processor:

   ```xml
   <property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
   ```

4. Start Red5 Pro, publish a stream, and confirm rectangles appear around faces.

## Prerequisites

- JDK 11 or higher
- Maven 3.x
- Docker (for release builds)
- Red5 Pro Server

## Building

### Release Build (Recommended)

Build the complete JAR with all native libraries bundled:

```sh
mvn clean package -Prelease
```

This automatically:

1. Builds OpenCV 4.13.0 and facemask module in a CentOS 7.9 Docker container
2. Packages all natives inside the JAR

Output: `target/opencv-facemask.jar` (~5.4 MB)

The JAR extracts native libraries from `native/facemask/` to a temp directory at runtime. Deploy the single JAR - no separate `.so` files needed.

### Development Build

Build JAR only (requires natives built separately):

```sh
mvn clean install
```

Output: `target/opencv-facemask.jar` (without bundled natives)

### Native Build Only

Build native components without Maven:

```sh
./docker/build-opencv.sh
```

Output:

- `target/facemask.so`
- `src/main/lib/amd64-Linux-gpp/*.so`

### Local Native Build

If you have OpenCV 4.13.0 installed locally:

```sh
cd src/main/c++
make
```

### Helper Scripts

- `scripts/build-release.sh`: wrapper for `mvn clean package -Prelease`
- `scripts/build-native.sh`: wrapper for `./docker/build-opencv.sh`
- `scripts/deploy-local.sh`: deploys JAR + config to `$RED5_ROOT` (set `RED5_ROOT=/usr/local/red5pro`)

## Deployment

### Option A: Bundled JAR (Recommended)

Using the release build JAR with bundled natives:

1. Configure `red5pro-activation.xml`:

   ```xml
   <property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
   ```

2. Copy files:

   ```sh
   cp target/opencv-facemask.jar /usr/local/red5pro/plugins/
   mkdir -p /usr/local/red5pro/plugins/native/facemask
   cp src/main/resources/module-facemask.xml /usr/local/red5pro/plugins/native/facemask/
   cp src/main/resources/haarcascade_frontalface_alt.xml /usr/local/red5pro/plugins/native/facemask/
   ```

The bundled natives are extracted automatically at runtime.

### Option B: Separate Files

Using the development build with separate native files:

1. Configure `red5pro-activation.xml`:

   ```xml
   <property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
   ```

2. Copy files:

   ```sh
   cp target/opencv-facemask.jar /usr/local/red5pro/plugins/
   mkdir -p /usr/local/red5pro/plugins/native/facemask
   cp target/facemask.so /usr/local/red5pro/plugins/native/facemask/
   cp src/main/lib/amd64-Linux-gpp/libopencv_*.so /usr/local/red5pro/plugins/native/facemask/
   cp src/main/resources/module-facemask.xml /usr/local/red5pro/plugins/native/facemask/
   cp src/main/resources/haarcascade_frontalface_alt.xml /usr/local/red5pro/plugins/native/facemask/
   ```

3. Edit `plugins/native/facemask/module-facemask.xml` with correct paths:

   ```xml
   <bean name="config" class="com.red5pro.server.cauldron.facemask.ModuleConfig">
       <property name="moduleFile" value="/usr/local/red5pro/plugins/native/facemask/facemask.so"/>
       <property name="supportLibs">
           <list>
               <value>/usr/local/red5pro/plugins/native/facemask/libopencv_core.so</value>
               <value>/usr/local/red5pro/plugins/native/facemask/libopencv_imgproc.so</value>
               <value>/usr/local/red5pro/plugins/native/facemask/libopencv_objdetect.so</value>
           </list>
       </property>
   </bean>
   ```

**Note:** The `supportLibs` entries are loaded sequentially. Library order matters: core -> imgproc -> objdetect.

## Create Your Own Brew (Overview)

Start from this repo and adjust the following when creating a new Brew-based plugin:

- Java package/class names: `com.red5pro.server.cauldron.facemask.*`
- Potion GUID in `src/main/resources/module-facemask.xml` (`<property name="potion" value="MASK" />`)
- Ingredient keys in the same XML (lower_snake_case to match native side)
- Native module filename (`facemask.so`) and the module path in `module-facemask.xml`
- Maven artifact name and plugin main class in `pom.xml`

For a step-by-step walkthrough, see `docs/CREATE_YOUR_OWN_BREW.md`.

## Troubleshooting

- `Wrong brew guid`: potion GUID in `module-facemask.xml` does not match the native module.
- Native load errors: verify `.so` paths and load order (core -> imgproc -> objdetect).
- No processing in streams: confirm `red5pro-activation.xml` uses `AudioCapableProcessor`.
