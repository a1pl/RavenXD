package openraven.ravenxd.forge.client.event;
import net.minecraft.client.renderer.entity.RenderPlayer; import net.minecraft.entity.player.EntityPlayer;
import openraven.ravenxd.forge.event.entity.player.PlayerEvent;
public class RenderPlayerEvent extends PlayerEvent {
    public final RenderPlayer renderer; public final float partialRenderTick; public final double x, y, z;
    public RenderPlayerEvent(EntityPlayer p, RenderPlayer r, float pt, double x, double y, double z) { super(p); renderer = r; partialRenderTick = pt; this.x = x; this.y = y; this.z = z; }
    public static class Pre extends RenderPlayerEvent { public Pre(EntityPlayer p, RenderPlayer r, float pt, double x, double y, double z) { super(p, r, pt, x, y, z); } }
    public static class Post extends RenderPlayerEvent { public Post(EntityPlayer p, RenderPlayer r, float pt, double x, double y, double z) { super(p, r, pt, x, y, z); } }
}
