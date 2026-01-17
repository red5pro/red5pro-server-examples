# Repository Guidelines

## Project Structure & Module Organization
This repository builds a Red5 Pro server plugin that couples Java glue code with a native OpenCV module.
- `src/main/java/com/red5pro/server/cauldron/facemask/`: Java plugin, loader, and configuration classes.
- `src/main/c++`: Native module implementation and local `make` build.
- `src/main/resources`: Spring config (`module-facemask.xml`), Haar cascade, and `mask.png`.
- `src/main/lib/amd64-Linux-gpp` and `src/main/include/amd64-Linux-gpp`: bundled OpenCV libs/headers for Linux.
- `docker/`: release build container and build script.
- `msvc/`: Visual Studio solution for Windows builds.
- `docs/`: demo output and images.
- `target/`: build artifacts (generated).

## Build, Test, and Development Commands
- `mvn clean package -Prelease`: release build; compiles OpenCV in Docker and produces a bundled `target/opencv-facemask.jar`.
- `mvn clean install`: development build; JAR without bundled natives.
- `./docker/build-opencv.sh`: native-only build to `target/facemask.so` and `src/main/lib/amd64-Linux-gpp/*.so`.
- `cd src/main/c++ && make`: local native build (requires OpenCV 4.13.0 installed).

## Coding Style & Naming Conventions
- Java uses 4-space indentation and standard Red5 package naming; follow existing class layout and logging style.
- C++ uses `.cpp`/`.h` pairs; keep functions and include ordering consistent with existing files.
- No formatter is enforced; avoid reformatting unrelated code.

## Testing Guidelines
There is no automated test suite in this module. Validate changes manually:
- Build a JAR, deploy to Red5 Pro, publish a stream, and confirm face boxes appear.
- Compare expected output in `docs/README.md`.
- Check server logs for native load order and Brew configuration issues.

## Commit & Pull Request Guidelines
- Recent history uses concise, sentence-case commit messages (no conventional prefixes); follow that pattern.
- PRs should describe the build profile used, deployment impact, and any config changes.
- If you change detection behavior or visuals, update `docs/README.md` and include a screenshot.

## Configuration & Deployment Notes
- `module-facemask.xml` must live under `${red5.root}/plugins/native/facemask/`.
- Support libraries load in order: core → imgproc → objdetect.
- Ensure `red5pro-activation.xml` uses `AudioCapableProcessor` for native processing.
