import openraven.ravenxd.forge.common.MinecraftForge;
import openraven.ravenxd.forge.client.event.MouseEvent;
import openraven.ravenxd.forge.fml.common.eventhandler.*;
import openraven.ravenxd.forge.fml.common.gameevent.TickEvent;
import java.util.*;
public class SmokeTest {
  static List<String> log = new ArrayList<String>();
  public static class A {
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){ log.add("A.tick."+e.phase); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void mouseHi(MouseEvent e){ log.add("A.mouseHi"); e.setCanceled(true); }
    @SubscribeEvent public void mouseNormal(MouseEvent e){ log.add("A.mouseNormal(should be skipped)"); }
    @SubscribeEvent(receiveCanceled=true) public void mouseRc(MouseEvent e){ log.add("A.mouseRc"); }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent e){ log.add("A.render."+e.phase+"."+e.renderTickTime); }
    @SubscribeEvent public void boom(TickEvent e){ throw new RuntimeException("listener failure must not escape"); }
  }
  public static void main(String[] a){
    A o=new A(); MinecraftForge.EVENT_BUS.register(o); MinecraftForge.EVENT_BUS.register(o);
    MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
    MinecraftForge.EVENT_BUS.post(new TickEvent.RenderTickEvent(TickEvent.Phase.END,0.5f));
    boolean c=MinecraftForge.EVENT_BUS.post(new MouseEvent());
    System.out.println("mouse canceled="+c);
    MinecraftForge.EVENT_BUS.unregister(o);
    MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    for(String s:log) System.out.println(s);
  }
}
