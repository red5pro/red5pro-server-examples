# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Red5 Pro server plugin that provides real-time face detection and masking using OpenCV. It demonstrates the Red5 Pro "Brew" API (version 2) for native video processing. The plugin loads a native shared library (.so) that processes video frames via OpenCV's objdetect module to locate faces.

## Build Commands

### Release Build (Recommended)
```sh
mvn clean package -Prelease
```
Builds OpenCV 4.13.0 and facemask module in CentOS 7.9 Docker container, then packages everything into a single JAR with bundled natives.

Output: `target/opencv-facemask.jar` (~5.4 MB)

### Development Build
```sh
mvn clean install
```
Output: `target/opencv-facemask.jar` (without bundled natives)

### Native Build Only
```sh
./docker/build-opencv.sh
```
Output:
- `target/facemask.so`
- `src/main/lib/amd64-Linux-gpp/*.so`

### Local Native Build
```sh
cd src/main/c++
make
```
Output: `facemask.so` - requires OpenCV 4.13.0 libraries and JDK 11+.

## Architecture

### Java Components (`com.red5pro.server.cauldron.facemask`)

- **Facemask.java** - Main Red5 plugin entry point. Implements `Red5Plugin` and `MediaProcessorAware`. Handles loading support libraries and configuring stream processors.

- **NativeLoader.java** - Utility for loading bundled natives from JAR (`native/facemask/`). Extracts to temp directory and loads in dependency order.

- **ModuleConfig.java** - Spring bean for native binary paths: module file path and support library list.

- **Brewery.java** - Spring bean for "potion" configuration: potion name (GUID) and ingredients map passed to native code.

### Native Components (`src/main/c++`)

- **TestProcessor** (cauldron_test.cpp/h) - Implements `CVideoProcessModule2` interface:
  - `get_guid()` - Returns 'MASK' as 4-byte identifier
  - `open()` - Initializes processor with video dimensions and format (YV420P)
  - `apply()` - Receives key/value properties (e.g., cascade_sheet path)
  - `process()` - Per-frame processing: YUV->BGR conversion, face detection, draw rectangle, BGR->YUV
  - Uses OpenCV CascadeClassifier with Haar cascades for face detection

### Docker Build (`docker/`)

- **Dockerfile** - CentOS 7.9 based build environment with devtoolset-11 and OpenCV 4.13.0
- **build-opencv.sh** - Script to build and extract all native artifacts

### Configuration Files

- **module-facemask.xml** - Spring config loaded from `${red5.root}/plugins/native/facemask/`:
  - `config` bean: paths to native module and OpenCV support libs
  - `brew` bean: potion name "MASK" and ingredients including `cascade_sheet` path

- **haarcascade_frontalface_alt.xml** - OpenCV Haar cascade for frontal face detection

## Maven Profiles

- `release` - Full build with Docker native compilation and bundled JAR
- `linux` - Default profile for local development
- `osx` - macOS native build (NAR plugin)
- `win` - Windows native build (NAR plugin with MSVC)

## Server Configuration

Requires `red5pro-activation.xml` to use AudioCapableProcessor:
```xml
<property name="nativeLoader" value="com.red5pro.media.transform.codec.AudioCapableProcessor" />
```

## Key Dependencies

- OpenCV 4.13.0 (core, imgproc, objdetect)
- Red5 Pro Common 9.1.2
- Red5 Server 1.2.8
- Java 11+
- Docker (for release builds)
