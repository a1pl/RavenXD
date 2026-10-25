import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.commons.*;

/** Build-time bytecode patcher: relocates the Forge API into openraven.ravenxd.forge and rewrites Forge-added vanilla calls to Compat statics. */
public class Patch {
    static final String FORGE = "net/minecraftforge/", FORGE_NEW = "openraven/ravenxd/forge/";
    static final String COMPAT = "openraven/ravenxd/forge/Compat";
    static int redirected = 0, classes = 0;

    static final Remapper REMAP = new Remapper() {
        public String map(String n) {
            if (n.startsWith(FORGE)) return FORGE_NEW + n.substring(FORGE.length());
            if (n.equals("net/minecraft/launchwrapper/Launch")) return "openraven/ravenxd/forge/Launch";
            if (n.equals("net/minecraft/launchwrapper/LaunchClassLoader")) return "openraven/ravenxd/forge/LaunchClassLoader";
            return n;
        }
    };

    static class ForgeAdded extends ClassVisitor {
        String cname;
        ForgeAdded(ClassVisitor cv) { super(Opcodes.ASM5, cv); }
        public void visit(int v, int a, String n, String sg, String sn, String[] i) { cname = n; super.visit(v, a, n, sg, sn, i); }
        public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
            MethodVisitor mv = super.visitMethod(a, n, d, s, e);
            final boolean inScriptReflection = cname != null && cname.startsWith("openraven/ravenxd/script/ScriptReflection");
            return mv == null ? null : new MethodVisitor(Opcodes.ASM5, mv) {
                public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                    if (inScriptReflection && op == Opcodes.INVOKEVIRTUAL && name.equals("setAccessible") && desc.equals("(Z)V")
                            && (owner.equals("java/lang/reflect/Field") || owner.equals("java/lang/reflect/Method"))) {
                        redirected++; super.visitMethodInsn(Opcodes.INVOKESTATIC, "openraven/ravenxd/lunar/Safe", "setAccessible", "(L" + owner + ";Z)V", false); return;
                    }
                    if (op == Opcodes.INVOKEVIRTUAL) {
                        String nd = null, nn = null;
                        if (owner.equals("net/minecraft/item/Item") && name.equals("getRegistryName") && desc.equals("()Ljava/lang/String;")) { nn = "getRegistryName"; nd = "(Lnet/minecraft/item/Item;)Ljava/lang/String;"; }
                        else if (owner.equals("net/minecraft/block/Block") && name.equals("getRegistryName") && desc.equals("()Ljava/lang/String;")) { nn = "getRegistryName"; nd = "(Lnet/minecraft/block/Block;)Ljava/lang/String;"; }
                        else if (owner.equals("net/minecraft/item/Item") && name.equals("getMetadata") && desc.equals("(Lnet/minecraft/item/ItemStack;)I")) { nn = "getMetadata"; nd = "(Lnet/minecraft/item/Item;Lnet/minecraft/item/ItemStack;)I"; }
                        else if (owner.equals("net/minecraft/entity/Entity") && name.equals("canRiderInteract") && desc.equals("()Z")) { nn = "canRiderInteract"; nd = "(Lnet/minecraft/entity/Entity;)Z"; }
                        else if (owner.equals("net/minecraft/client/multiplayer/WorldClient") && name.equals("isSideSolid") && desc.equals("(Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Z)Z")) { nn = "isSideSolid"; nd = "(Lnet/minecraft/client/multiplayer/WorldClient;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Z)Z"; }
                        if (nn != null) { redirected++; super.visitMethodInsn(Opcodes.INVOKESTATIC, COMPAT, nn, nd, false); return; }
                    }
                    if (op == Opcodes.INVOKESTATIC) {
                        if (owner.equals("javax/tools/ToolProvider") && name.equals("getSystemJavaCompiler")) { redirected++; super.visitMethodInsn(op, "openraven/ravenxd/lunar/Safe", "compiler", desc, false); return; }
                        if (owner.equals("openraven/ravenxd/script/ScriptManager") && name.equals("reload") && desc.equals("()V")) { redirected++; super.visitMethodInsn(op, "openraven/ravenxd/lunar/Safe", "reloadScripts", desc, false); return; }
                    }
                    super.visitMethodInsn(op, owner, name, desc, itf);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        final Path in = Paths.get(args[0]), out = Paths.get(args[1]);
        Files.walk(in).forEach(p -> {
            try {
                Path rel = in.relativize(p); Path dst = out.resolve(rel.toString());
                if (Files.isDirectory(p)) { Files.createDirectories(dst); return; }
                Files.createDirectories(dst.getParent());
                String r = rel.toString().replace('\\', '/');
                if (r.startsWith("openraven/") && r.endsWith(".class")) {
                    ClassReader cr = new ClassReader(Files.readAllBytes(p));
                    ClassWriter cw = new ClassWriter(0);
                    cr.accept(new ClassRemapper(new ForgeAdded(cw), REMAP), 0);
                    Files.write(dst, cw.toByteArray()); classes++;
                } else Files.copy(p, dst, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
        System.out.println("patched classes=" + classes + " forge-added call sites redirected=" + redirected);
    }
}
