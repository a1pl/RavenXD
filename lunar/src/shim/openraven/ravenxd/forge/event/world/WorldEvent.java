package openraven.ravenxd.forge.event.world;
import net.minecraft.world.World; import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class WorldEvent extends Event {
    public final World world; public WorldEvent(World w) { world = w; }
    public static class Unload extends WorldEvent { public Unload(World w) { super(w); } }
}
