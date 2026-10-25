package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

/** Forge fires DrawBlockHighlightEvent right before drawSelectionBox (AutoPlace places blocks from it). MCP name + refmap, like the other Lunar mixins. */
@Mixin(value = RenderGlobal.class)
public abstract class MixinRenderGlobalEvents {
    @Inject(method = "drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$highlight(EntityPlayer p, MovingObjectPosition mop, int sub, float pt, CallbackInfo ci) {
        if (Events.drawBlockHighlight((RenderGlobal) (Object) this, p, mop, sub, pt)) ci.cancel();
    }
}
