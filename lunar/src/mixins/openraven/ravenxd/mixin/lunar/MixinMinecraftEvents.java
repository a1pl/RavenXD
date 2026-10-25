package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import openraven.ravenxd.forge.client.event.GuiOpenEvent;
import openraven.ravenxd.lunar.LunarBootstrap;

/** Fires the Forge-style client events that vanilla Minecraft itself never emits. Names are MCP + refmap (SRG), the same scheme as the original Raven mixins. */
@Mixin(value = Minecraft.class)
public abstract class MixinMinecraftEvents {
    private static boolean ravenxd$reenter;

    /** Replaces Forge's @Mod init: runs at the very end of Minecraft.startGame. */
    @Inject(method = "startGame", at = @At("RETURN"), require = 0)
    private void ravenxd$startGame(CallbackInfo ci) { LunarBootstrap.init(); }

    @Inject(method = "runTick", at = @At("HEAD"), require = 0)
    private void ravenxd$tickStart(CallbackInfo ci) { Events.clientTick(true); }

    @Inject(method = "runTick", at = @At("RETURN"), require = 0)
    private void ravenxd$tickEnd(CallbackInfo ci) { Events.clientTick(false); }

    /** Forge's `if (postMouseEvent()) continue;` expressed as a replacement for the loop condition. Not an Inject on KeyBinding.setKeyBindState, so no clash with Lunar's redirect. */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false), require = 0)
    private boolean ravenxd$mouseNext() {
        while (Mouse.next()) { if (!Events.mouse()) return true; }
        return false;
    }

    @Inject(method = "dispatchKeypresses", at = @At("HEAD"), require = 0)
    private void ravenxd$keyInput(CallbackInfo ci) { Events.keyInput(); }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$displayGui(GuiScreen gui, CallbackInfo ci) {
        if (ravenxd$reenter) return;
        GuiOpenEvent e = Events.guiOpen(gui);
        if (e.isCanceled()) { ci.cancel(); return; }
        if (e.gui != gui) {
            ci.cancel();
            ravenxd$reenter = true;
            try { ((Minecraft) (Object) this).func_147108_a(e.gui); } finally { ravenxd$reenter = false; }
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"), require = 0)
    private void ravenxd$loadWorld(net.minecraft.client.multiplayer.WorldClient world, String msg, CallbackInfo ci) {
        Events.worldUnload(((Minecraft) (Object) this).field_71441_e);
    }
}
