# Red5 RTSP Restreamer Demo
This demo shows how to use a built-in rtsp ip-camera client and re-stream the media from within the red5pro server.

For demo instructions, please follow the annotated file `src/main/java/com/red5pro/restreamer/Restreamer.java`

## Building
To build the demo from source, you'll need to have Java 8 and Maven 3 installed; in addition, you'll need to edit the `pom.xml` to point to your Red5 Pro Server installation directory to resolve library dependencies.
```xml
<dependency>
    <groupId>com.red5pro</groupId>
    <artifactId>red5pro-mega</artifactId>
    <!-- pay no attention to this version when system scope is used -->
    <version>5.4.0</version>
    <scope>system</scope>
    <systemPath>/usr/local/red5pro/lib/red5pro-5.4.0.31.jar</systemPath>
</dependency>
<dependency>
    <groupId>com.red5pro</groupId>
    <artifactId>red5pro-restreamer-plugin</artifactId>
    <!-- pay no attention to this version when system scope is used -->
    <version>5.4.0</version>
    <scope>system</scope>
    <systemPath>/usr/local/red5pro/plugins/red5pro-restreamer-plugin-5.4.0.31.jar</systemPath>
</dependency>
```
Update the `systemPath` to match your install path.

Once that's completed, build and package up as a Web Application Archive with the command `mvn`; you'll locate the `rtspingest.war` in the `target` directory.
