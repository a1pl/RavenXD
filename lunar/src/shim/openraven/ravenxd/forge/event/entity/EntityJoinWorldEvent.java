package openraven.ravenxd.forge.event.entity;
import net.minecraft.entity.Entity; import net.minecraft.world.World;
public class EntityJoinWorldEvent extends EntityEvent { public final World world; public EntityJoinWorldEvent(Entity entity, World world) { super(entity); this.world = world; } }
