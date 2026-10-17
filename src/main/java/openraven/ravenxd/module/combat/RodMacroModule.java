package openraven.ravenxd.module.combat;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.KeyUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// kclient rodmacro
public class RodMacroModule extends Module {
    public static SliderSetting minLaunch, maxLaunch, minSwitchback, maxSwitchback;
    public static ButtonSetting showFailMessage, infiniteRod;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Rod Macro");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> currentTask;
    private volatile boolean processing;
    private volatile int originalSlot = -1;
    private volatile boolean bobberThrown;
    private volatile int swordSlot = -1;

    public RodMacroModule(int key) {
        super("Rod Macro", Category.combat, key);
        MinecraftForge.EVENT_BUS.register(new BobberWatcher());
    }

    public void link() {
        minLaunch = (SliderSetting) ModuleManager.setting(this, "Min launch delay");
        maxLaunch = (SliderSetting) ModuleManager.setting(this, "Max launch delay");
        minSwitchback = (SliderSetting) ModuleManager.setting(this, "Min switchback delay");
        maxSwitchback = (SliderSetting) ModuleManager.setting(this, "Max switchback delay");
        showFailMessage = (ButtonSetting) ModuleManager.setting(this, "Show fail message");
        infiniteRod = (ButtonSetting) ModuleManager.setting(this, "Infinite rod");
    }

    private static long delay(SliderSetting min, SliderSetting max) {
        double a = min.getInput(), b = max.getInput();
        return (long) (a >= b ? a : ThreadLocalRandom.current().nextDouble(a, b));
    }

    private void tap(KeyBinding key) {
        mc.addScheduledTask(() -> KeyUtils.click(key));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            this.disable();
            return;
        }
        int rodSlot = findRod();
        if (rodSlot == -1) {
            if (showFailMessage.isToggled()) {
                Chat.moduleMessage(this, "&cNo rod found in hotbar!");
            }
            this.disable();
            return;
        }
        if (this.processing) {
            if (this.currentTask != null) {
                this.currentTask.cancel(false);
            }
            this.tap(mc.gameSettings.keyBindUseItem);
            this.currentTask = this.executor.schedule(() -> {
                this.tap(mc.gameSettings.keyBindUseItem);
                this.scheduleSwitchback();
            }, delay(minLaunch, maxLaunch), TimeUnit.MILLISECONDS);
            this.disable();
            return;
        }
        this.processing = true;
        this.originalSlot = mc.thePlayer.inventory.currentItem;
        this.tap(mc.gameSettings.keyBindsHotbar[rodSlot]);
        this.currentTask = this.executor.schedule(() -> {
            this.tap(mc.gameSettings.keyBindUseItem);
            if (infiniteRod.isToggled()) {
                this.swordSlot = this.findSword();
                this.bobberThrown = true;
            } else {
                this.scheduleSwitchback();
            }
        }, delay(minLaunch, maxLaunch), TimeUnit.MILLISECONDS);
        this.disable();
    }

    private void scheduleSwitchback() {
        this.currentTask = this.executor.schedule(() -> {
            if (this.originalSlot != -1) {
                this.tap(mc.gameSettings.keyBindsHotbar[this.originalSlot]);
            }
            this.processing = false;
            this.originalSlot = -1;
        }, delay(minSwitchback, maxSwitchback), TimeUnit.MILLISECONDS);
    }

    private int findRod() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemFishingRod) {
                return i;
            }
        }
        return -1;
    }

    private int findSword() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) {
                return i;
            }
        }
        return this.originalSlot;
    }

    private final class BobberWatcher {
        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent e) {
            if (!bobberThrown || e.phase != TickEvent.Phase.END || e.player != mc.thePlayer || mc.theWorld == null) {
                return;
            }
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityFishHook && ((EntityFishHook) entity).angler == mc.thePlayer) {
                    return;
                }
            }
            bobberThrown = false;
            if (swordSlot != -1) {
                KeyUtils.click(mc.gameSettings.keyBindsHotbar[swordSlot]);
            }
            swordSlot = -1;
            processing = false;
            originalSlot = -1;
        }
    }
}
