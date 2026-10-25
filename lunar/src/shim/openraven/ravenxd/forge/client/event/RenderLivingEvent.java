package openraven.ravenxd.forge.client.event;
import net.minecraft.entity.EntityLivingBase; import net.minecraft.client.renderer.entity.RendererLivingEntity;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class RenderLivingEvent extends Event {
    public final EntityLivingBase entity; public final RendererLivingEntity renderer; public final double x, y, z;
    public RenderLivingEvent(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { entity = e; renderer = r; this.x = x; this.y = y; this.z = z; }
    public static class Pre extends RenderLivingEvent { public Pre(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { super(e, r, x, y, z); } }
    public static class Post extends RenderLivingEvent { public Post(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { super(e, r, x, y, z); } }
    public static class Specials extends RenderLivingEvent {
        public Specials(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { super(e, r, x, y, z); }
        public static class Pre extends Specials { public Pre(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { super(e, r, x, y, z); } }
        public static class Post extends Specials { public Post(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) { super(e, r, x, y, z); } }
    }
}
