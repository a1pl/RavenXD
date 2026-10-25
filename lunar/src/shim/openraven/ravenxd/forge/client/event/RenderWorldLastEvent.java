package openraven.ravenxd.forge.client.event;
import net.minecraft.client.renderer.RenderGlobal;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class RenderWorldLastEvent extends Event {
    public final RenderGlobal context; public final float partialTicks;
    public RenderWorldLastEvent(RenderGlobal context, float partialTicks) { this.context = context; this.partialTicks = partialTicks; }
}
