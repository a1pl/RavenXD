package openraven.ravenxd.forge.event.entity;
import net.minecraft.entity.Entity; import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class EntityEvent extends Event { public final Entity entity; public EntityEvent(Entity entity) { this.entity = entity; } }
