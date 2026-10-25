package meowtils.ravenxd;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Reads and edits the Meowtils Lunar agent's agent-mods.json. Entries are {jar, mixin, property}. */
final class AgentConfig {
    static final String JAR = "RavenXD.jar";
    static final String MIXIN = "mixins.ravenxd.json";
    static final String PROPERTY = "ravenxd.agent.injected";

    private AgentConfig() {
    }

    /** The config file given to the Meowtils agent: -javaagent:<lunar-agent.jar>=<agent-mods.json>. */
    static File find() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-javaagent:")) continue;
            int eq = arg.indexOf('=', "-javaagent:".length());
            if (eq < 0) continue;
            File f = new File(unquote(arg.substring(eq + 1)));
            if (f.isFile() && f.getName().endsWith(".json")) return f;
        }
        return null;
    }

    static String unquote(String s) {
        s = s.trim();
        return s.length() > 1 && s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
    }

    static void removeRavenXD(List<String[]> mods, File jar) {
        String ours = jar.getAbsolutePath().replace('\\', '/');
        for (int i = mods.size() - 1; i >= 0; i--) {
            String[] m = mods.get(i);
            String path = m[0].replace("\\\\", "/").replace('\\', '/');
            if (MIXIN.equals(m[1]) || path.equalsIgnoreCase(ours) || path.endsWith("/" + JAR)) mods.remove(i);
        }
    }

    /** Same loose format the agent itself reads: objects with "jar", "mixin" and "property". */
    static List<String[]> read(File config) throws IOException {
        String json = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
        List<String[]> mods = new ArrayList<String[]>();
        String[] blocks = json.split("\\{");
        for (int i = 1; i < blocks.length; i++) {
            String jar = value(blocks[i], "jar");
            if (jar != null) mods.add(new String[]{jar, value(blocks[i], "mixin"), value(blocks[i], "property")});
        }
        return mods;
    }

    static String value(String block, String key) {
        int k = block.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int colon = block.indexOf(':', k);
        int start = colon < 0 ? -1 : block.indexOf('"', colon + 1);
        int end = start < 0 ? -1 : block.indexOf('"', start + 1);
        return end < 0 ? null : block.substring(start + 1, end);
    }

    static void write(File config, List<String[]> mods) throws IOException {
        StringBuilder b = new StringBuilder("{\n  \"mods\": [");
        for (int i = 0; i < mods.size(); i++) {
            String[] m = mods.get(i);
            b.append(i == 0 ? "\n" : ",\n").append("    {\n      \"jar\": \"").append(m[0]).append('"');
            if (m[1] != null) b.append(",\n      \"mixin\": \"").append(m[1]).append('"');
            if (m[2] != null) b.append(",\n      \"property\": \"").append(m[2]).append('"');
            b.append("\n    }");
        }
        b.append("\n  ]\n}\n");
        File tmp = new File(config.getPath() + ".tmp");
        Files.write(tmp.toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(tmp.toPath(), config.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
