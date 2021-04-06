# Demo Mask
Live demo used to introduce the new feature. Plugin uses opencv objdetect to overlay png face mask


# Building with JAR

For all platforms, to build the jar plugin without compiling the native code:
```sh
mvn clean install
```
Jar in target folder installs to server plugins folder.


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
This demo uses native API version 2. This requires you to change the red5pro-activation.xml file property to use the AudioCapableProcessor:

```sh
<property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
```


# References

[NAR FAQ](https://github.com/maven-nar/nar-maven-plugin/wiki/)

[NAR Maven Plugin](http://maven-nar.github.io/index.html)




