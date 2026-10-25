package openraven.ravenxd.forge.event.entity.player;
import net.minecraft.entity.player.EntityPlayer; import openraven.ravenxd.forge.event.entity.living.LivingEvent;
public class PlayerEvent extends LivingEvent { public final EntityPlayer entityPlayer; public PlayerEvent(EntityPlayer p) { super(p); entityPlayer = p; } }
