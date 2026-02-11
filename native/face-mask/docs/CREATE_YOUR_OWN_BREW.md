# Create Your Own Brew

Use this repository as a template for your own Red5 Pro Brew module. The goal is to keep the Java glue thin and push behavior into the native processor.

## Minimal Files to Copy/Rename

- Java: `src/main/java/com/red5pro/server/cauldron/facemask/`
  - `Facemask.java` (plugin entry point)
  - `NativeLoader.java` (bundled natives)
  - `ModuleConfig.java` (module + support libs)
  - `Brewery.java` (potion + ingredients)
- Native: `src/main/c++/cauldron_test.*` (rename to your module)
- Config: `src/main/resources/module-facemask.xml`
- Assets: any files referenced by ingredients (e.g., `haarcascade_frontalface_alt.xml`)

## Rename Checklist

1. Update package/class names in Java to your module name.
2. Update `pom.xml`:
   - `<artifactId>` and `<finalName>`
   - `Red5-Plugin-Main-Class` manifest entry
3. Rename the native output (`facemask.so`) and the `moduleFile` path in `module-facemask.xml`.
4. Set your potion GUID in `module-facemask.xml`:
   ```xml
   <property name="potion" value="MASK" />
   ```
5. Keep ingredient keys lower_snake_case to match native property names:
   ```xml
   <entry key="cascade_sheet" value="plugins/native/facemask/haarcascade_frontalface_alt.xml"/>
   ```

## Brew Contract (Java → Native)

- Java creates a `Potion` from the GUID and adds `Ingredient` entries.
- The native module reads these key/value pairs in `apply()` and uses them to configure processing.
- When you change ingredient keys, update both `module-facemask.xml` and the native code that reads them.

## Minimal "Hello Brew" Flow

1. Keep `Facemask.java` logic the same, but change the potion GUID and ingredient map.
2. In native `apply()`, log the incoming property keys to confirm wiring.
3. Build and deploy using the bundled JAR path:
   ```sh
   mvn clean package -Prelease
   cp target/opencv-facemask.jar /usr/local/red5pro/plugins/
   ```

## Common Pitfalls

- Mismatched potion GUID between Java config and native module.
- Wrong load order for support libs (core → imgproc → objdetect).
- Missing asset files referenced by ingredients.
