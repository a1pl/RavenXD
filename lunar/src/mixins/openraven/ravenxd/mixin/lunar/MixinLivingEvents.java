package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.entity.EntityLivingBase;

@Mixin(value = EntityLivingBase.class)
public abstract class MixinLivingEvents {
    @Inject(method = "onLivingUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$livingUpdate(CallbackInfo ci) {
        if (Events.livingUpdate((EntityLivingBase) (Object) this)) ci.cancel();
    }
}
