package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = EntityPlayer.class)
public abstract class MixinPlayerAttackEvents {
    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$attack(Entity target, CallbackInfo ci) {
        if (Events.attack((EntityPlayer) (Object) this, target)) ci.cancel();
    }
}
