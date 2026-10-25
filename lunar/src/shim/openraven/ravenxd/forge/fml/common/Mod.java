package openraven.ravenxd.forge.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Mod {
    String modid(); String name() default ""; String version() default ""; String acceptedMinecraftVersions() default "";
    boolean clientSideOnly() default false; boolean useMetadata() default false; String guiFactory() default "";
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface EventHandler {}
}
