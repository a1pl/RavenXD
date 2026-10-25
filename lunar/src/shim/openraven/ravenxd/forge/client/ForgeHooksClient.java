package openraven.ravenxd.forge.client;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
public class ForgeHooksClient {
    private static Method apply; private static boolean searched;
    /** Vanilla fallback: apply the model's own camera transform for the given perspective, return the model. */
    public static IBakedModel handleCameraTransforms(IBakedModel model, ItemCameraTransforms.TransformType type) {
        try {
            Object cam = model.func_177552_f();
            if (!searched) {
                searched = true;
                for (Method m : cam.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0] == type.getClass() && m.getReturnType() == void.class) { apply = m; break; }
                }
            }
            if (apply != null) apply.invoke(cam, type);
        } catch (Throwable ignored) {}
        return model;
    }
}
