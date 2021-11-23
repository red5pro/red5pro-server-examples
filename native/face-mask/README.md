# Demo Mask
Live demo used to introduce the brew feature. Plugin uses opencv objdetect to locate a face


# Building the JAR

For all platforms, to build the jar plugin:
```sh
mvn clean install
```
The jar will be found in the target folder, and installs to server plugins directory.


# Building the native code

Install build tools 

```sh
apt-get update apt install build-essential cmake git pkg-config libgtk-3-dev  libavcodec-dev libavformat-dev libswscale-dev libv4l-dev  libxvidcore-dev libx264-dev libjpeg-dev libpng-dev libtiff-dev  gfortran openexr libatlas-base-dev python3-dev python3-numpy  libtbb2 libtbb-dev libdc1394-22-dev libopenexr-dev  libgstreamer-plugins-base1.0-dev libgstreamer1.0-dev
```

Navigate into the c++ folder and call 'make' 

Requires openJDK 11 or higher. 

# Server configuration
This demo uses native brew API version 2. This requires you to change the red5pro-activation.xml file property to use the AudioCapableProcessor:

```sh
<property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
```

Compile the jar and place it into the server plugins folder.

Create a directory named 'facemask' in the server plugins folder.

Put the two resource xml files into the facemask directory.

Edit module-facemask.xml file as required. 

Set moduleFile path to your compiled native code facemask-4.2.0.so binary  

Edit as required the the supportLibs list of openCV components.  

```sh

    <bean name="config" class="com.red5pro.server.cauldron.facemask.ModuleConfig" >	
		<property name="moduleFile" value="/usr/local/red5pro/plugins/facemask/facemask-4.2.0.so"/>
		<property name="supportLibs" >
		    <list >
				<value>/usr/local/red5pro/plugins/facemask/libopencv_core.so</value>
				<value>/usr/local/red5pro/plugins/facemask/libopencv_imgproc.so</value>
				<value>/usr/local/red5pro/plugins/facemask/libopencv_objdetect.so</value>
		    </list>
		</property>
	</bean>

```
The Support entries are loaded sequentially so any linker errors at runtime can be solved by correctly ordering the dependancies in the supportLibs list as above. 
 





