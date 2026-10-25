package openraven.ravenxd.forge.client.event;
import net.minecraft.client.renderer.RenderGlobal; import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack; import net.minecraft.util.MovingObjectPosition;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class DrawBlockHighlightEvent extends Event {
    public final RenderGlobal context; public final EntityPlayer player; public final MovingObjectPosition target;
    public final int subID; public final ItemStack currentItem; public final float partialTicks;
    public DrawBlockHighlightEvent(RenderGlobal c, EntityPlayer p, MovingObjectPosition t, int s, ItemStack i, float pt) {
        context = c; player = p; target = t; subID = s; currentItem = i; partialTicks = pt; }
}
