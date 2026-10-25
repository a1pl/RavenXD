package openraven.ravenxd.lunar;

import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;

import openraven.ravenxd.ClickGuiMod;

/**
 * Startup entry for Lunar Client. On Forge the client starts through @Mod / FMLInitializationEvent;
 * Lunar has no FML, so a mixin at the tail of Minecraft.startGame calls this instead.
 */
public final class LunarBootstrap {
    private static boolean started;
    private LunarBootstrap() {}

    public static synchronized void init() {
        if (started) return;
        started = true;
        try { System.setProperty("ravenxd.loaded", "true"); } catch (Throwable ignored) {}
        try { registerResourcePack(); } catch (Throwable t) { System.err.println("[RavenXD] resource pack not registered (capes, N+ icons, waifus and motion blur will be unavailable): " + t); }
        // Same sequence as ClickGuiMod.init(FMLInitializationEvent), but each step is isolated so one
        // reflection miss on Lunar's runtime cannot leave the client half-initialised.
        final ClickGuiMod mod = new ClickGuiMod();
        int failed = 0;
        failed += step("modules", new Runnable() { public void run() { openraven.ravenxd.module.ModuleManager.init(); } });
        failed += step("script reflection", new Runnable() { public void run() { openraven.ravenxd.script.ScriptReflection.init(); } });
        failed += step("script events", new Runnable() { public void run() { openraven.ravenxd.script.ScriptEvents.bind(openraven.ravenxd.script.ScriptDelegated.class); } });
        failed += step("click gui", new Runnable() { public void run() { openraven.ravenxd.gui.ClickGui.instance = new openraven.ravenxd.gui.ClickGui(); } });
        failed += step("n+ gui", new Runnable() { public void run() { openraven.ravenxd.gui.nplus.NPlusGui.instance = new openraven.ravenxd.gui.nplus.NPlusGui(); } });
        failed += step("config", new Runnable() { public void run() { openraven.ravenxd.client.ClientConfig.load(); } });
        failed += step("event handlers", new Runnable() { public void run() {
            openraven.ravenxd.forge.common.MinecraftForge.EVENT_BUS.register(mod);
            openraven.ravenxd.forge.common.MinecraftForge.EVENT_BUS.register(new openraven.ravenxd.client.RenderDispatcher());
        } });
        failed += step("commands", new Runnable() { public void run() {
            openraven.ravenxd.forge.client.ClientCommandHandler.instance.func_71560_a(new openraven.ravenxd.client.KeystrokesGui.Command());
        } });
        failed += step("keystrokes", new Runnable() { public void run() {
            openraven.ravenxd.forge.common.MinecraftForge.EVENT_BUS.register(new openraven.ravenxd.client.Keystrokes());
            openraven.ravenxd.client.Keystrokes.instance = new openraven.ravenxd.client.Keystrokes();
            openraven.ravenxd.client.Keystrokes.load();
        } });
        System.out.println("[RavenXD] initialised on Lunar Client" + (failed == 0 ? "" : " (" + failed + " step(s) failed, see above)"));
    }

    private static int step(String name, Runnable r) {
        try { r.run(); return 0; }
        catch (Throwable t) { System.err.println("[RavenXD] init step '" + name + "' failed: " + t); t.printStackTrace(); return 1; }
    }

    /**
     * Forge auto-registers mod jars as resource packs; Lunar's agent does not, so expose assets/** ourselves:
     * capes (keystrokesmod), N+ icons and waifus (ravenxd) and the motion blur shader (minecraft). For the
     * minecraft domain this pack only answers for files that are inside this jar, so vanilla assets are untouched.
     */
    private static void registerResourcePack() throws Throwable {
        final Class<?> packType = Class.forName("net.minecraft.client.resources.IResourcePack");
        final Set<String> domains = new HashSet<String>(Arrays.asList("keystrokesmod", "clickgui", "ravenxd", "minecraft"));
        Object pack = Proxy.newProxyInstance(packType.getClassLoader(), new Class<?>[]{packType}, new InvocationHandler() {
            public Object invoke(Object proxy, Method m, Object[] a) throws Throwable {
                Class<?>[] p = m.getParameterTypes(); Class<?> r = m.getReturnType();
                if (p.length == 1 && r == InputStream.class) {
                    InputStream in = open(String.valueOf(a[0]));
                    if (in == null) throw new java.io.FileNotFoundException(String.valueOf(a[0]));
                    return in;
                }
                if (p.length == 1 && r == boolean.class) {
                    InputStream in = open(String.valueOf(a[0]));
                    if (in == null) return false;
                    in.close(); return true;
                }
                if (r == Set.class) return domains;
                if (r == String.class && p.length == 0) return "RavenXD";
                if (r == boolean.class) return false;
                if (r == int.class) return 0;
                return null;
            }
        });
        Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
        Object mc = null;
        for (String n : new String[]{"getMinecraft", "func_71410_x"}) { try { mc = mcClass.getMethod(n).invoke(null); break; } catch (Throwable ignored) {} }
        if (mc == null) return;
        // keep it across F3+T reloads
        for (String n : new String[]{"defaultResourcePacks", "field_110449_ao"}) {
            try { Field dp = mc.getClass().getDeclaredField(n); dp.setAccessible(true); ((List) dp.get(mc)).add(pack); break; } catch (Throwable ignored) {}
        }
        // and make it available right now without a full reload
        Object mgr = null;
        for (Field f : mc.getClass().getDeclaredFields()) {
            if (f.getType().getName().endsWith("IReloadableResourceManager")) { f.setAccessible(true); mgr = f.get(mc); break; }
        }
        if (mgr == null) return;
        for (Method m : mgr.getClass().getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 1 && p[0] == packType && m.getReturnType() == void.class) { m.invoke(mgr, pack); return; }
        }
    }

    /** "domain:path" -> /assets/domain/path inside this jar */
    private static InputStream open(String location) {
        int i = location.indexOf(':');
        String domain = i < 0 ? "minecraft" : location.substring(0, i);
        String path = i < 0 ? location : location.substring(i + 1);
        return LunarBootstrap.class.getResourceAsStream("/assets/" + domain + "/" + path);
    }
}
