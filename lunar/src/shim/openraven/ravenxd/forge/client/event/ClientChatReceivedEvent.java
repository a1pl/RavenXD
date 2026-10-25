package openraven.ravenxd.forge.client.event;
import net.minecraft.util.IChatComponent;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class ClientChatReceivedEvent extends Event {
    public final byte type; public IChatComponent message;
    public ClientChatReceivedEvent(byte type, IChatComponent message) { this.type = type; this.message = message; }
}
