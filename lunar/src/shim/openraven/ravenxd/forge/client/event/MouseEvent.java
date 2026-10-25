package openraven.ravenxd.forge.client.event;
import org.lwjgl.input.Mouse;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class MouseEvent extends Event {
    public int x, y, dx, dy, dwheel, button; public boolean buttonstate; public long nanoseconds;
    public MouseEvent() {
        x = Mouse.getEventX(); y = Mouse.getEventY(); dx = Mouse.getEventDX(); dy = Mouse.getEventDY();
        dwheel = Mouse.getEventDWheel(); button = Mouse.getEventButton(); buttonstate = Mouse.getEventButtonState();
        nanoseconds = Mouse.getEventNanoseconds();
    }
}
