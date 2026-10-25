import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.spongepowered.asm.lib.*;

/**
 * Build-time check of the Lunar jar: every field/method the patched classes, shims and mixins use must
 * exist either in this jar or in vanilla 1.8.9 (no Forge). Forge-only members compile fine against the
 * Forge jar but throw NoSuchMethodError/NoSuchFieldError on Lunar, so they have to be redirected in Patch.
 * With the SRG->MCP mappings it also checks every mixin target: Lunar runs with MCP names and the mixins
 * use require = 0, so a target that does not exist would silently never be applied.
 * Usage: Verify <out dir> <vanilla minecraft-client.jar> <srg.tsrg> [mappings-srg-named.srg]
 */
public class Verify {
    static Path out;
    static final Map<String, byte[]> vanilla = new HashMap<String, byte[]>(); // obf name -> bytes
    static final Map<String, String> srgToObfClass = new HashMap<String, String>(), obfToSrgClass = new HashMap<String, String>();
    static final Map<String, Map<String, String>> obfMember = new HashMap<String, Map<String, String>>(); // obf class -> (obf name[+desc] -> srg name)
    static final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    static final Set<String> problems = new TreeSet<String>();

    static class ClassInfo {
        String name, superName; String[] interfaces = new String[0];
        Set<String> methods = new HashSet<String>(), fields = new HashSet<String>(); // "name desc" / "name"
        boolean external; // outside what we can inspect (libraries): assume members exist
    }

    public static void main(String[] a) throws Exception {
        out = Paths.get(a[0]);
        try (JarFile jf = new JarFile(a[1])) {
            for (JarEntry e : Collections.list(jf.entries()))
                if (e.getName().endsWith(".class")) vanilla.put(e.getName().substring(0, e.getName().length() - 6), readAll(jf.getInputStream(e)));
        }
        String cur = null;
        for (String line : Files.readAllLines(Paths.get(a[2]))) {
            if (line.isEmpty()) continue;
            String[] t = line.trim().split(" ");
            if (line.charAt(0) != '\t') { cur = t[0]; srgToObfClass.put(t[1], t[0]); obfToSrgClass.put(t[0], t[1]); obfMember.put(t[0], new HashMap<String, String>()); }
            else if (t.length == 2) obfMember.get(cur).put(t[0], t[1]);
            else obfMember.get(cur).put(t[0] + t[1], t[2]);
        }
        final int[] refs = {0};
        Files.walk(out).filter(p -> p.toString().endsWith(".class")).forEach(p -> {
            String rel = out.relativize(p).toString().replace('\\', '/');
            if (rel.startsWith("org/spongepowered/") || rel.startsWith("META-INF/")) return; // bundled Mixin, provided by Lunar
            try {
                final String self = rel.substring(0, rel.length() - 6);
                new ClassReader(Files.readAllBytes(p)).accept(new ClassVisitor(Opcodes.ASM5) {
                    public void visit(int v, int acc, String n, String s, String sup, String[] itf) {
                        type(self, sup); if (itf != null) for (String i : itf) type(self, i);
                    }
                    public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                        return new MethodVisitor(Opcodes.ASM5) {
                            public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) { refs[0]++; check(self, owner, name, desc, true); }
                            public void visitFieldInsn(int op, String owner, String name, String desc) { refs[0]++; check(self, owner, name, desc, false); }
                            public void visitTypeInsn(int op, String t) { if (!t.startsWith("[")) type(self, t); }
                        };
                    }
                }, ClassReader.SKIP_FRAMES);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
        int mixinTargets = a.length > 3 ? checkMixins(Paths.get(a[3])) : -1;
        for (String s : problems) System.out.println("  " + s);
        System.out.println("verified " + refs[0] + " member references" + (mixinTargets < 0 ? "" : " and " + mixinTargets + " mixin targets") + ": " + problems.size() + " problem(s)");
        if (!problems.isEmpty()) System.exit(1);
    }

    // ---- mixin targets (MCP namespace) ----

    static final Map<String, String> srgToMcp = new HashMap<String, String>();
    static final Set<String> INJECTORS = new HashSet<String>(Arrays.asList("Inject", "Redirect", "ModifyVariable", "ModifyArg", "ModifyArgs", "ModifyConstant"));

    static String mcp(String n) { return srgToMcp.containsKey(n) ? srgToMcp.get(n) : n; }

    static int checkMixins(Path mappings) throws IOException {
        for (String line : Files.readAllLines(mappings)) {
            String[] t = line.split(" ");
            if (line.startsWith("FD: ")) srgToMcp.put(t[1].substring(t[1].lastIndexOf('/') + 1), t[2].substring(t[2].lastIndexOf('/') + 1));
            else if (line.startsWith("MD: ")) srgToMcp.put(t[1].substring(t[1].lastIndexOf('/') + 1), t[3].substring(t[3].lastIndexOf('/') + 1));
        }
        int count = 0;
        DirectoryStream<Path> configs = Files.newDirectoryStream(out, "mixins.*.json");
        for (Path cfg : configs) {
            if (cfg.getFileName().toString().contains("refmap")) continue;
            String json = new String(Files.readAllBytes(cfg), "UTF-8");
            java.util.regex.Matcher pk = java.util.regex.Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            String pkg = pk.find() ? pk.group(1).replace('.', '/') : "";
            java.util.regex.Matcher arr = java.util.regex.Pattern.compile("\"(?:client|mixins)\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
            while (arr.find()) {
                java.util.regex.Matcher item = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(arr.group(1));
                while (item.find()) count += checkMixin(pkg + "/" + item.group(1).replace('.', '/'));
            }
        }
        configs.close();
        return count;
    }

    static int checkMixin(final String mixin) throws IOException {
        Path p = out.resolve(mixin + ".class");
        if (!Files.exists(p)) { problems.add("mixin config lists missing class " + mixin); return 0; }
        final List<String> targets = new ArrayList<String>();
        final List<String[]> wanted = new ArrayList<String[]>(); // {kind, spec, origin}
        new ClassReader(Files.readAllBytes(p)).accept(new ClassVisitor(Opcodes.ASM5) {
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
                return new AnnotationVisitor(Opcodes.ASM5) {
                    public AnnotationVisitor visitArray(final String name) {
                        return new AnnotationVisitor(Opcodes.ASM5) {
                            public void visit(String n, Object v) {
                                if (v instanceof org.spongepowered.asm.lib.Type) targets.add(((org.spongepowered.asm.lib.Type) v).getInternalName());
                                else if ("targets".equals(name)) targets.add(((String) v).replace('.', '/'));
                            }
                        };
                    }
                    public void visit(String name, Object v) {
                        if (v instanceof org.spongepowered.asm.lib.Type) targets.add(((org.spongepowered.asm.lib.Type) v).getInternalName());
                    }
                };
            }
            public MethodVisitor visitMethod(int acc, final String mname, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM5) {
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        final String simple = desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
                        final String origin = mixin + "." + mname;
                        if (simple.equals("Accessor") || simple.equals("Invoker")) {
                            return new AnnotationVisitor(Opcodes.ASM5) {
                                public void visit(String n, Object v) { if ("value".equals(n)) wanted.add(new String[]{simple.equals("Accessor") ? "field" : "method", (String) v, origin}); }
                            };
                        }
                        if (!INJECTORS.contains(simple)) return null;
                        return new AnnotationVisitor(Opcodes.ASM5) {
                            public AnnotationVisitor visitArray(String n) {
                                if ("method".equals(n)) return new AnnotationVisitor(Opcodes.ASM5) {
                                    public void visit(String x, Object v) { wanted.add(new String[]{"method", (String) v, origin}); }
                                };
                                if ("at".equals(n)) return new AnnotationVisitor(Opcodes.ASM5) {
                                    public AnnotationVisitor visitAnnotation(String x, String d2) { return at(origin, wanted); }
                                };
                                return null;
                            }
                            public AnnotationVisitor visitAnnotation(String n, String d2) { return "at".equals(n) ? at(origin, wanted) : null; }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE);
        if (targets.isEmpty()) problems.add(mixin + " has no @Mixin target");
        for (String t : targets) {
            ClassInfo c = info(t);
            if (c == null) { problems.add(mixin + " -> missing target class " + t); continue; }
            for (String[] w : wanted) {
                if (w[0].equals("at")) { checkAt(w[2], w[1]); continue; }
                boolean found = false;
                Set<String> declared = w[0].equals("field") ? c.fields : c.methods;
                int paren = w[1].indexOf('(');
                String name = paren < 0 ? w[1] : w[1].substring(0, paren), desc = paren < 0 ? null : w[1].substring(paren);
                for (String m : declared) {
                    int dp = m.indexOf('(');
                    String mn = mcp(dp < 0 ? m : m.substring(0, dp));
                    if (mn.equals(name) && (desc == null || m.substring(dp).equals(desc))) { found = true; break; }
                }
                if (!found) problems.add(w[2] + " -> mixin target " + w[0] + " " + t + "." + w[1] + " not in vanilla (MCP)");
            }
        }
        return wanted.size();
    }

    static AnnotationVisitor at(final String origin, final List<String[]> wanted) {
        return new AnnotationVisitor(Opcodes.ASM5) {
            public void visit(String n, Object v) { if ("target".equals(n)) wanted.add(new String[]{"at", (String) v, origin}); }
        };
    }

    /** @At target "Lowner;name(desc)ret" or "Lowner;name:desc". Only Minecraft owners are checked. */
    static void checkAt(String origin, String target) {
        if (!target.startsWith("L") || target.indexOf(';') < 0) return;
        String owner = target.substring(1, target.indexOf(';')), rest = target.substring(target.indexOf(';') + 1);
        if (!owner.startsWith("net/minecraft/")) return;
        boolean method = rest.indexOf('(') >= 0;
        String name = method ? rest.substring(0, rest.indexOf('(')) : rest.substring(0, rest.indexOf(':'));
        String desc = method ? rest.substring(rest.indexOf('(')) : null;
        if (!resolveMcp(owner, name, desc, method, new HashSet<String>())) problems.add(origin + " -> @At target " + target + " not in vanilla (MCP)");
    }

    static boolean resolveMcp(String owner, String name, String desc, boolean method, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        ClassInfo c = info(owner);
        if (c == null) return false;
        if (c.external) return true;
        for (String m : method ? c.methods : c.fields) {
            int dp = method ? m.indexOf('(') : -1;
            if (mcp(dp < 0 ? m : m.substring(0, dp)).equals(name) && (!method || m.substring(dp).equals(desc))) return true;
        }
        if (resolveMcp(c.superName, name, desc, method, seen)) return true;
        for (String i : c.interfaces) if (resolveMcp(i, name, desc, method, seen)) return true;
        return false;
    }

    static void type(String from, String t) {
        if (t == null) return;
        if (t.startsWith("net/minecraftforge/") || t.startsWith("net/minecraft/launchwrapper/") || t.startsWith("cpw/mods/"))
            problems.add(from + " -> Forge class " + t);
        else if (info(t) == null) problems.add(from + " -> missing class " + t);
    }

    static void check(String from, String owner, String name, String desc, boolean method) {
        if (owner.startsWith("[")) return; // array clone/length
        type(from, owner);
        if (name.startsWith("lambda$")) return;
        if (owner.equals("java/lang/invoke/MethodHandle") && name.startsWith("invoke")) return; // signature polymorphic
        if (!resolve(owner, name, desc, method, new HashSet<String>()))
            problems.add(from + " -> " + (method ? "method " : "field ") + owner + "." + name + (method ? desc : ""));
    }

    static boolean resolve(String owner, String name, String desc, boolean method, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        ClassInfo c = info(owner);
        if (c == null) return false;
        if (c.external) return true;
        if (method ? c.methods.contains(name + desc) : c.fields.contains(name)) return true;
        if (resolve(c.superName, name, desc, method, seen)) return true;
        for (String i : c.interfaces) if (resolve(i, name, desc, method, seen)) return true;
        return method && c.superName == null && owner.equals("java/lang/Object") == false && resolve("java/lang/Object", name, desc, true, seen);
    }

    /** Class in SRG namespace: our jar, else vanilla (obf -> srg), else JDK by reflection, else an unchecked library. */
    static ClassInfo info(String n) {
        if (cache.containsKey(n)) return cache.get(n);
        ClassInfo c = null;
        try {
            Path p = out.resolve(n + ".class");
            if (Files.exists(p) && !n.startsWith("org/spongepowered/")) c = fromBytes(Files.readAllBytes(p), null);
            else if (n.startsWith("net/minecraft/")) {
                String obf = srgToObfClass.containsKey(n) ? srgToObfClass.get(n) : n;
                if (vanilla.containsKey(obf)) c = fromBytes(vanilla.get(obf), obf);
            } else if (n.startsWith("java/") || n.startsWith("javax/") || n.startsWith("sun/")) {
                c = fromReflection(Class.forName(n.replace('/', '.'), false, ClassLoader.getSystemClassLoader()));
            } else { c = new ClassInfo(); c.name = n; c.external = true; }
        } catch (Throwable t) { c = null; }
        cache.put(n, c);
        return c;
    }

    static String srgClass(String obf) { return obfToSrgClass.containsKey(obf) ? obfToSrgClass.get(obf) : obf; }

    static String srgDesc(String d) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            char ch = d.charAt(i); b.append(ch);
            if (ch == 'L') { int e = d.indexOf(';', i); b.append(srgClass(d.substring(i + 1, e))).append(';'); i = e; }
        }
        return b.toString();
    }

    static ClassInfo fromBytes(byte[] bytes, final String obf) {
        final ClassInfo c = new ClassInfo();
        final Map<String, String> mm = obf == null ? null : obfMember.get(obf);
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            public void visit(int v, int acc, String n, String s, String sup, String[] itf) {
                c.name = obf == null ? n : srgClass(n);
                c.superName = sup == null ? null : (obf == null ? sup : srgClass(sup));
                c.interfaces = itf == null ? new String[0] : itf.clone();
                if (obf != null) for (int i = 0; i < c.interfaces.length; i++) c.interfaces[i] = srgClass(c.interfaces[i]);
            }
            public FieldVisitor visitField(int acc, String n, String d, String s, Object v) {
                c.fields.add(mm == null ? n : (mm.containsKey(n) ? mm.get(n) : n)); return null;
            }
            public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                if (mm == null) c.methods.add(n + d);
                else c.methods.add((mm.containsKey(n + d) ? mm.get(n + d) : n) + srgDesc(d));
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return c;
    }

    static ClassInfo fromReflection(Class<?> k) {
        ClassInfo c = new ClassInfo();
        c.name = k.getName().replace('.', '/');
        c.superName = k.getSuperclass() == null ? null : k.getSuperclass().getName().replace('.', '/');
        Class<?>[] itf = k.getInterfaces(); c.interfaces = new String[itf.length];
        for (int i = 0; i < itf.length; i++) c.interfaces[i] = itf[i].getName().replace('.', '/');
        for (Field f : k.getDeclaredFields()) c.fields.add(f.getName());
        for (Method m : k.getDeclaredMethods()) c.methods.add(m.getName() + org.spongepowered.asm.lib.Type.getMethodDescriptor(m));
        for (Constructor<?> m : k.getDeclaredConstructors()) c.methods.add("<init>" + org.spongepowered.asm.lib.Type.getConstructorDescriptor(m));
        c.methods.add("<clinit>()V");
        return c;
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close(); return b.toByteArray();
    }
}
