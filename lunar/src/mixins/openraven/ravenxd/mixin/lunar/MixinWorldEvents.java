package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Mixin(value = World.class)
public abstract class MixinWorldEvents {
    @Inject(method = "spawnEntityInWorld", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$join(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Events.joinWorld(entity, (World) (Object) this)) cir.setReturnValue(false);
    }
}
