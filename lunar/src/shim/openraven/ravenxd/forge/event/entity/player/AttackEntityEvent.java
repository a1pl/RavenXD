package openraven.ravenxd.forge.event.entity.player;
import net.minecraft.entity.Entity; import net.minecraft.entity.player.EntityPlayer;
public class AttackEntityEvent extends PlayerEvent { public final Entity target; public AttackEntityEvent(EntityPlayer p, Entity target) { super(p); this.target = target; } }
