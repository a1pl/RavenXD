package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.gui.GuiScreen;

@Mixin(value = GuiScreen.class)
public abstract class MixinGuiScreenEvents {
    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$mousePre(CallbackInfo ci) { if (Events.mouseInputPre((GuiScreen) (Object) this)) ci.cancel(); }

    @Inject(method = "handleMouseInput", at = @At("RETURN"), require = 0)
    private void ravenxd$mousePost(CallbackInfo ci) { Events.mouseInputPost((GuiScreen) (Object) this); }

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$keyPre(CallbackInfo ci) { if (Events.keyboardInputPre((GuiScreen) (Object) this)) ci.cancel(); }

    @Inject(method = "handleKeyboardInput", at = @At("RETURN"), require = 0)
    private void ravenxd$keyPost(CallbackInfo ci) { Events.keyboardInputPost((GuiScreen) (Object) this); }
}
