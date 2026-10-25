package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

@Mixin(value = EntityRenderer.class)
public abstract class MixinEntityRendererEvents {
    @Inject(method = "updateCameraAndRender", at = @At("HEAD"), require = 0)
    private void ravenxd$renderStart(float partialTicks, long nano, CallbackInfo ci) { Events.renderTick(true, partialTicks); }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"), require = 0)
    private void ravenxd$renderEnd(float partialTicks, long nano, CallbackInfo ci) { Events.renderTick(false, partialTicks); }

    /** Same injection point Myau uses for its Render3D event: just before the hand is drawn == Forge's RenderWorldLastEvent. */
    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE, ordinal = 0), require = 0)
    private void ravenxd$worldLast(int pass, float partialTicks, long nano, CallbackInfo ci) {
        Events.renderWorldLast(Minecraft.func_71410_x().field_71438_f, partialTicks);
    }
}
