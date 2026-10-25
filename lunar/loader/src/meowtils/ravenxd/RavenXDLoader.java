package meowtils.ravenxd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import wtf.tatp.meowtils.Meowtils;
import wtf.tatp.meowtils.extension.Extension;

/**
 * Adds Raven XD to the Meowtils Lunar agent. Lunar has no Forge, so Raven XD cannot be loaded like a
 * normal extension: it needs its mixins applied at startup. This writes it into the agent's
 * agent-mods.json (the file passed after -javaagent:lunar-agent.jar=), and the next launch loads it.
 */
public final class RavenXDLoader extends Extension {
    static final String JAR = "RavenXD.jar";
    static final String BUNDLED = "meowtils/ravenxd/" + JAR;

    public RavenXDLoader() {
        super("Raven XD", "qloha");
        this.info("Loads the Raven XD client on Lunar.\nPress Load, then fully restart the game.");
        this.button("Load Raven XD", 1.0f, new Runnable() {
            public void run() {
                load();
            }
        });
        this.button("Unload Raven XD", 1.0f, new Runnable() {
            public void run() {
                unload();
            }
        });
    }

    static void say(String s) {
        Meowtils.addMessage(s);
        Meowtils.info("[Raven XD] " + s.replaceAll("§.", ""));
    }

    static void load() {
        try {
            if (!Meowtils.isLunar()) {
                say("§cThe Raven XD extension is only for Lunar. On Forge, put RavenXD-3.1.jar in your mods folder instead.");
                return;
            }
            File config = AgentConfig.find();
            if (config == null) {
                say("§cCould not find the Meowtils Lunar agent. Launch with the -javaagent argument from the Meowtils installer.");
                return;
            }
            File jar = installJar();
            if (jar == null) {
                say("§cRaven XD jar not found. Put " + JAR + " in " + Meowtils.MEOWTILS_DIR.getAbsolutePath());
                return;
            }
            List<String[]> mods = AgentConfig.read(config);
            AgentConfig.removeRavenXD(mods, jar);
            mods.add(new String[]{jar.getAbsolutePath().replace('\\', '/'), AgentConfig.MIXIN, AgentConfig.PROPERTY});
            AgentConfig.write(config, mods);
            if (Boolean.getBoolean(AgentConfig.PROPERTY)) {
                say("§aRaven XD is already loaded. Restart the game to use an updated jar.");
            } else {
                say("§aRaven XD added to the Lunar agent. Fully restart the game to load it.");
            }
        } catch (Throwable t) {
            say("§cFailed to load Raven XD: " + t);
            t.printStackTrace();
        }
    }

    static void unload() {
        try {
            File config = AgentConfig.find();
            if (config == null) {
                say("§cCould not find the Meowtils Lunar agent config.");
                return;
            }
            List<String[]> mods = AgentConfig.read(config);
            int before = mods.size();
            AgentConfig.removeRavenXD(mods, new File(Meowtils.MEOWTILS_DIR, JAR));
            if (mods.size() == before) {
                say("Raven XD is not in the Lunar agent config.");
                return;
            }
            AgentConfig.write(config, mods);
            say("§aRaven XD removed from the Lunar agent. It stays active until you restart.");
        } catch (Throwable t) {
            say("§cFailed to unload Raven XD: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Puts RavenXD.jar in the Meowtils folder: the copy bundled in this extension wins, otherwise a jar the
     * user placed there is used. Lunar keeps the jar open while Raven XD runs, so replacing it can fail
     * until a restart; the existing copy is used in that case.
     */
    static File installJar() throws IOException {
        File target = new File(Meowtils.MEOWTILS_DIR, JAR);
        File self = ownExtensionFile();
        if (self != null) {
            try (JarFile jf = new JarFile(self)) {
                JarEntry e = jf.getJarEntry(BUNDLED);
                if (e != null && (!target.isFile() || target.length() != e.getSize() || !sameBytes(jf, e, target))) {
                    File tmp = new File(Meowtils.MEOWTILS_DIR, JAR + ".tmp");
                    Meowtils.MEOWTILS_DIR.mkdirs();
                    try (InputStream in = jf.getInputStream(e); OutputStream out = new FileOutputStream(tmp)) {
                        copy(in, out);
                    }
                    try {
                        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException busy) {
                        tmp.delete();
                        if (!target.isFile()) throw busy;
                        say("§eCould not replace " + JAR + " while it is in use; restart and press Load again to update it.");
                    }
                }
            }
        }
        return target.isFile() ? target : null;
    }

    /** Extension classes on Lunar cannot read their own resources, so find this .meowtils file on disk. */
    static File ownExtensionFile() {
        File[] files = Meowtils.EXTENSION_DIR.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.getName().endsWith(".meowtils")) continue;
            try (JarFile jf = new JarFile(f)) {
                if (jf.getJarEntry(BUNDLED) != null) return f;
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    static boolean sameBytes(JarFile jf, JarEntry e, File f) throws IOException {
        try (InputStream a = jf.getInputStream(e); InputStream b = Files.newInputStream(f.toPath())) {
            byte[] x = new byte[8192], y = new byte[8192];
            while (true) {
                int n = readFully(a, x), m = readFully(b, y);
                if (n != m) return false;
                if (n <= 0) return true;
                for (int i = 0; i < n; i++) if (x[i] != y[i]) return false;
            }
        }
    }

    static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0, n;
        while (total < buf.length && (n = in.read(buf, total, buf.length - total)) > 0) total += n;
        return total;
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

}
