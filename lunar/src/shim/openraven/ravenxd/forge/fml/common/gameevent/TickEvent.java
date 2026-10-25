package openraven.ravenxd.forge.fml.common.gameevent;
import openraven.ravenxd.forge.fml.common.eventhandler.Event;
public class TickEvent extends Event {
    public enum Type { WORLD, PLAYER, CLIENT, SERVER, RENDER }
    public enum Phase { START, END }
    public final Type type; public final Phase phase;
    public TickEvent(Type type, Phase phase) { this.type = type; this.phase = phase; }
    public static class ClientTickEvent extends TickEvent { public ClientTickEvent(Phase phase) { super(Type.CLIENT, phase); } }
    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;
        public RenderTickEvent(Phase phase, float renderTickTime) { super(Type.RENDER, phase); this.renderTickTime = renderTickTime; }
    }
}
