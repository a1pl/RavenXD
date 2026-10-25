package openraven.ravenxd.lunar;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import openraven.ravenxd.forge.client.event.*;
import openraven.ravenxd.forge.common.MinecraftForge;
import openraven.ravenxd.forge.event.entity.EntityJoinWorldEvent;
import openraven.ravenxd.forge.event.entity.living.LivingEvent;
import openraven.ravenxd.forge.event.entity.player.AttackEntityEvent;
import openraven.ravenxd.forge.event.entity.player.PlayerUseItemEvent;
import openraven.ravenxd.forge.event.world.WorldEvent;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
import openraven.ravenxd.forge.fml.common.eventhandler.EventBus;
import openraven.ravenxd.forge.fml.common.gameevent.InputEvent;
import openraven.ravenxd.forge.fml.common.gameevent.TickEvent;

/**
 * Glue between the Lunar mixins and the Forge-style event bus. Mixins live in a package that
 * cannot be referenced directly, so they call these statics. Every method is exception-safe:
 * a failing listener must never take the game loop down.
 */
public final class Events {
    private Events() {}
    private static final EventBus BUS = MinecraftForge.EVENT_BUS;

    private static boolean post(Event e) {
        try { return BUS.post(e); } catch (Throwable t) { t.printStackTrace(); return false; }
    }

    public static void clientTick(boolean start) {
        post(new TickEvent.ClientTickEvent(start ? TickEvent.Phase.START : TickEvent.Phase.END));
    }
    public static void renderTick(boolean start, float partialTicks) {
        post(new TickEvent.RenderTickEvent(start ? TickEvent.Phase.START : TickEvent.Phase.END, partialTicks));
    }
    public static void renderWorldLast(RenderGlobal rg, float partialTicks) { post(new RenderWorldLastEvent(rg, partialTicks)); }

    /** @return true if cancelled */
    public static boolean mouse() {
        try { return BUS.post(new MouseEvent()); } catch (Throwable t) { t.printStackTrace(); return false; }
    }
    public static void keyInput() { post(new InputEvent.KeyInputEvent()); }

    /** @return true if the spawn must be refused */
    public static boolean joinWorld(Entity entity, World world) {
        if (!BUS.hasListeners(EntityJoinWorldEvent.class)) return false;
        return post(new EntityJoinWorldEvent(entity, world)) && !(entity instanceof EntityPlayer);
    }
    public static ClientChatReceivedEvent chat(byte type, IChatComponent msg) {
        ClientChatReceivedEvent e = new ClientChatReceivedEvent(type, msg);
        post(e);
        return e;
    }
    public static GuiOpenEvent guiOpen(GuiScreen gui) { GuiOpenEvent e = new GuiOpenEvent(gui); post(e); return e; }
    public static boolean mouseInputPre(GuiScreen gui) { return post(new GuiScreenEvent.MouseInputEvent.Pre(gui)); }
    public static void mouseInputPost(GuiScreen gui) { post(new GuiScreenEvent.MouseInputEvent.Post(gui)); }
    public static boolean keyboardInputPre(GuiScreen gui) { return post(new GuiScreenEvent.KeyboardInputEvent.Pre(gui)); }
    public static void keyboardInputPost(GuiScreen gui) { post(new GuiScreenEvent.KeyboardInputEvent.Post(gui)); }

    public static boolean attack(EntityPlayer p, Entity target) {
        if (!BUS.hasListeners(AttackEntityEvent.class)) return false;
        return post(new AttackEntityEvent(p, target));
    }
    public static boolean livingUpdate(EntityLivingBase e) {
        if (!BUS.hasListeners(LivingEvent.LivingUpdateEvent.class)) return false;
        return post(new LivingEvent.LivingUpdateEvent(e));
    }
    private static java.lang.reflect.Field fItem, fCount; private static boolean fSearched;
    public static void useItemFinish(EntityPlayer p) {
        if (!BUS.hasListeners(PlayerUseItemEvent.Finish.class)) return;
        try {
            if (!fSearched) {
                fSearched = true;
                for (String n : new String[]{"field_71074_e", "itemInUse"}) { try { fItem = EntityPlayer.class.getDeclaredField(n); fItem.setAccessible(true); break; } catch (Throwable ignored) {} }
                for (String n : new String[]{"field_71072_f", "itemInUseCount"}) { try { fCount = EntityPlayer.class.getDeclaredField(n); fCount.setAccessible(true); break; } catch (Throwable ignored) {} }
            }
            if (fItem == null) return;
            ItemStack stack = (ItemStack) fItem.get(p);
            if (stack == null) return;
            int count = fCount == null ? 0 : fCount.getInt(p);
            post(new PlayerUseItemEvent.Finish(p, stack, count, stack));
        } catch (Throwable t) { /* never break the game over an event */ }
    }
    public static void worldUnload(World w) { if (w != null) post(new WorldEvent.Unload(w)); }

    public static boolean renderSpecialsPre(EntityLivingBase e, RendererLivingEntity r, double x, double y, double z) {
        if (!BUS.hasListeners(RenderLivingEvent.Specials.Pre.class)) return false;
        return post(new RenderLivingEvent.Specials.Pre(e, r, x, y, z));
    }
    public static boolean renderPlayerPre(EntityPlayer p, RenderPlayer r, float pt, double x, double y, double z) {
        if (!BUS.hasListeners(RenderPlayerEvent.Pre.class)) return false;
        return post(new RenderPlayerEvent.Pre(p, r, pt, x, y, z));
    }
    public static void renderPlayerPost(EntityPlayer p, RenderPlayer r, float pt, double x, double y, double z) {
        if (BUS.hasListeners(RenderPlayerEvent.Post.class)) post(new RenderPlayerEvent.Post(p, r, pt, x, y, z));
    }
    public static boolean drawBlockHighlight(RenderGlobal rg, EntityPlayer p, MovingObjectPosition mop, int sub, float pt) {
        if (!BUS.hasListeners(DrawBlockHighlightEvent.class)) return false;
        return post(new DrawBlockHighlightEvent(rg, p, mop, sub, p == null ? null : p.func_70694_bm(), pt));
    }
}
