package openraven.ravenxd.mixin.lunar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import openraven.ravenxd.lunar.Events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;
import openraven.ravenxd.forge.client.event.ClientChatReceivedEvent;

@Mixin(value = NetHandlerPlayClient.class)
public abstract class MixinNetHandlerEvents {
    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true, require = 0)
    private void ravenxd$chat(S02PacketChat packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc == null || !mc.func_152345_ab()) return; // netty thread: vanilla re-queues it onto the main thread first
        ClientChatReceivedEvent e = Events.chat(packet.func_179841_c(), packet.func_148915_c());
        if (e.isCanceled()) ci.cancel();
    }
}
