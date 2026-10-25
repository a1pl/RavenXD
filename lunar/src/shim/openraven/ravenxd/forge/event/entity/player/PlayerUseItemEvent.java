package openraven.ravenxd.forge.event.entity.player;
import net.minecraft.entity.player.EntityPlayer; import net.minecraft.item.ItemStack;
public class PlayerUseItemEvent extends PlayerEvent {
    public final ItemStack item; public int duration;
    public PlayerUseItemEvent(EntityPlayer p, ItemStack item, int duration) { super(p); this.item = item; this.duration = duration; }
    public static class Finish extends PlayerUseItemEvent {
        public ItemStack result;
        public Finish(EntityPlayer p, ItemStack item, int duration, ItemStack result) { super(p, item, duration); this.result = result; }
    }
}
