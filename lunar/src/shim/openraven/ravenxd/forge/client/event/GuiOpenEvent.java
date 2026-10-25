package openraven.ravenxd.forge.client.event;
import net.minecraft.client.gui.GuiScreen;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class GuiOpenEvent extends Event { public GuiScreen gui; public GuiOpenEvent(GuiScreen gui) { this.gui = gui; } }
