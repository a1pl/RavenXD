package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = EntityPlayer.class)
public abstract class MixinPlayerUseItemEvents {
    @Inject(method = "onItemUseFinish", at = @At("HEAD"), require = 0)
    private void ravenxd$useFinish(CallbackInfo ci) { Events.useItemFinish((EntityPlayer) (Object) this); }
}
