package openraven.ravenxd.forge;
import java.util.HashMap; import java.util.Map;
/** Stand-in for net.minecraft.launchwrapper.Launch (absent on Lunar). */
public class Launch {
    public static Map<String, Object> blackboard = new HashMap<String, Object>();
    public static LaunchClassLoader classLoader = new LaunchClassLoader(Launch.class.getClassLoader());
    static { blackboard.put("fml.deobfuscatedEnvironment", Boolean.FALSE); }
}
