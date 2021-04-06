# Demo Mask
Live demo used to introduce the new feature. Plugin uses opencv objdetect to overlay png face mask


Download or build opencv developer components.  .libs and .dll/.so

# Building with JAR

For all platforms, to build the jar plugin without compiling the native code:
```sh
mvn clean install
```
The jar in target folder installs to server plugins folder.

Using this method allows you to compile the native code with the tools of your choice.


# Building with NAR
The example dynamic modules requires opencv.
Install includes and lib/dlls into proper AOL directory.

Replace `linux` with the target operating system in the commands below:
 * Linux = `linux`
 * Windows = `win`
 * MacOS / OSX = `osx`

To build for Linux, execute the following command using multiple profiles
```sh
mvn -P linux
```

# Server configuration
This demo uses native brew API version 2. This requires you to change the red5pro-activation.xml file property to use the AudioCapableProcessor:

```sh
<property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
```

Compile the jar and place it into the server plugins folder.

Create a directory named 'facemask' in the server plugins folder.

Put the three resource files into the facemask directory. mask.png and two xml files.

Edit module-facemask.xml file. 

Set moduleFile path to your compiled native code dll/so/dynlib  

This demo uses open CV. Either install the opencv components to your server's path or set them into the supportLibs property.  

```sh

		<property name="moduleFile" value="plugins/facemask/facemask.dll"/>
		<property name="supportLibs" >
		    <list >
 				<value>plugins/facemask/opencv_world320.dll</value>
		    </list>
		</property>

```
If you install libopenCV-dev with apt-get, you can remove the entry from the supportLibs property. If your openCV components are compiled as separate parts, the entries are loaded sequentially so any linker errors at runtime can be solved by correctly ordering the dependancies in the supportLibs list. 
 

# References

[NAR FAQ](https://github.com/maven-nar/nar-maven-plugin/wiki/)

[NAR Maven Plugin](http://maven-nar.github.io/index.html)




