package openraven.ravenxd.lunar;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Guards for features that need a full JDK. Lunar ships a JRE, so the script compiler may be missing; that must never break startup. */
public final class Safe {
    private Safe() {}

    /** Replaces ToolProvider.getSystemJavaCompiler() inside ScriptManager.<clinit>. */
    public static JavaCompiler compiler() {
        try { return ToolProvider.getSystemJavaCompiler(); }
        catch (Throwable t) { return null; }
    }

    /** Replaces calls to ScriptManager.reload(). */
    public static void reloadScripts() {
        try { openraven.ravenxd.script.ScriptManager.reload(); }
        catch (Throwable t) { System.err.println("[RavenXD] scripts unavailable on this runtime: " + t); }
    }

    public static void setAccessible(java.lang.reflect.Field f, boolean b) { if (f != null) f.setAccessible(b); }
    public static void setAccessible(java.lang.reflect.Method m, boolean b) { if (m != null) m.setAccessible(b); }
}
