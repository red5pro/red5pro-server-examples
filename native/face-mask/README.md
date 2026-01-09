# Face Mask Demo

A Red5 Pro server plugin demonstrating the Brew API (v2) for native video processing. Uses OpenCV's objdetect module for real-time face detection on live video streams.

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
