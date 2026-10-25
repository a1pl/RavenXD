package openraven.ravenxd.forge;
import net.minecraft.block.Block; import net.minecraft.entity.Entity; import net.minecraft.item.Item; import net.minecraft.item.ItemStack;
import net.minecraft.client.multiplayer.WorldClient; import net.minecraft.util.BlockPos; import net.minecraft.util.EnumFacing;
/** Vanilla fallbacks for methods that Forge adds to vanilla classes. Call sites are rewritten to these statics at build time. */
public final class Compat {
    private Compat() {}
    public static String getRegistryName(Item item) {
        try { Object k = Item.field_150901_e.func_177774_c(item); return k == null ? null : k.toString(); } catch (Throwable t) { return null; }
    }
    public static String getRegistryName(Block block) {
        try { Object k = Block.field_149771_c.func_177774_c(block); return k == null ? null : k.toString(); } catch (Throwable t) { return null; }
    }
    public static int getMetadata(Item item, ItemStack stack) { return stack.func_77960_j(); }
    public static boolean canRiderInteract(Entity e) { return false; }
    public static boolean isSideSolid(WorldClient w, BlockPos pos, EnumFacing side, boolean def) {
        try { if (!w.func_175667_e(pos)) return def; return w.func_180495_p(pos).func_177230_c().func_149721_r(); } catch (Throwable t) { return def; }
    }
}
