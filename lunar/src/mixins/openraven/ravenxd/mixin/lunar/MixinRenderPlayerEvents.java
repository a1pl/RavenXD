package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;

@Mixin(value = RenderPlayer.class)
public abstract class MixinRenderPlayerEvents {
    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$pre(AbstractClientPlayer p, double x, double y, double z, float yaw, float pt, CallbackInfo ci) {
        if (Events.renderPlayerPre(p, (RenderPlayer) (Object) this, pt, x, y, z)) ci.cancel();
    }
    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("RETURN"), require = 0)
    private void ravenxd$post(AbstractClientPlayer p, double x, double y, double z, float yaw, float pt, CallbackInfo ci) {
        Events.renderPlayerPost(p, (RenderPlayer) (Object) this, pt, x, y, z);
    }
}
