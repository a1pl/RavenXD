package openraven.ravenxd.forge.client.event;
import net.minecraft.client.gui.GuiScreen;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class GuiScreenEvent extends Event {
    public final GuiScreen gui;
    public GuiScreenEvent(GuiScreen gui) { this.gui = gui; }
    public static class MouseInputEvent extends GuiScreenEvent {
        public MouseInputEvent(GuiScreen gui) { super(gui); }
        public static class Pre extends MouseInputEvent { public Pre(GuiScreen gui) { super(gui); } }
        public static class Post extends MouseInputEvent { public Post(GuiScreen gui) { super(gui); } }
    }
    public static class KeyboardInputEvent extends GuiScreenEvent {
        public KeyboardInputEvent(GuiScreen gui) { super(gui); }
        public static class Pre extends KeyboardInputEvent { public Pre(GuiScreen gui) { super(gui); } }
        public static class Post extends KeyboardInputEvent { public Post(GuiScreen gui) { super(gui); } }
    }
}
