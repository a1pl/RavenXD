package openraven.ravenxd.forge.event.entity.living;
import net.minecraft.entity.EntityLivingBase; import openraven.ravenxd.forge.event.entity.EntityEvent;
public class LivingEvent extends EntityEvent {
    public final EntityLivingBase entityLiving;
    public LivingEvent(EntityLivingBase e) { super(e); entityLiving = e; }
    public static class LivingUpdateEvent extends LivingEvent { public LivingUpdateEvent(EntityLivingBase e) { super(e); } }
}
