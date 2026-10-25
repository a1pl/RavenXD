package openraven.ravenxd.forge.fml.common.eventhandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Small Forge-compatible event bus (register/unregister/post, priorities, receiveCanceled). */
public class EventBus {
    private static final class Listener {
        final Object target; final Method method; final Class<?> type; final int prio; final boolean recvCanceled; final long seq;
        Listener(Object t, Method m, Class<?> ty, int p, boolean rc, long s) { target = t; method = m; type = ty; prio = p; recvCanceled = rc; seq = s; }
    }
    private final CopyOnWriteArrayList<Listener> all = new CopyOnWriteArrayList<Listener>();
    private final Map<Object, List<Listener>> byTarget = Collections.synchronizedMap(new IdentityHashMap<Object, List<Listener>>());
    private final Map<Class<?>, Listener[]> cache = new ConcurrentHashMap<Class<?>, Listener[]>();
    private final Map<String, Integer> errors = new ConcurrentHashMap<String, Integer>();
    private long counter;

    public void register(Object target) {
        if (target == null) return;
        synchronized (this) {
            if (byTarget.containsKey(target)) return;
            boolean isClass = target instanceof Class;
            Class<?> cls = isClass ? (Class<?>) target : target.getClass();
            List<Listener> mine = new ArrayList<Listener>();
            Set<Class<?>> supers = new LinkedHashSet<Class<?>>();
            collect(cls, supers);
            for (Method m : cls.getMethods()) {
                if (isClass && !Modifier.isStatic(m.getModifiers())) continue;
                for (Class<?> c : supers) {
                    Method real;
                    try { real = c.getDeclaredMethod(m.getName(), m.getParameterTypes()); } catch (Throwable t) { continue; }
                    SubscribeEvent a = real.getAnnotation(SubscribeEvent.class);
                    if (a == null) continue;
                    Class<?>[] p = real.getParameterTypes();
                    if (p.length != 1 || !Event.class.isAssignableFrom(p[0])) {
                        System.err.println("[RavenXD] Ignoring bad @SubscribeEvent " + real);
                        break;
                    }
                    try { real.setAccessible(true); } catch (Throwable ignored) {}
                    mine.add(new Listener(isClass ? null : target, real, p[0], a.priority().ordinal(), a.receiveCanceled(), counter++));
                    break;
                }
            }
            byTarget.put(target, mine);
            all.addAll(mine);
            cache.clear();
        }
    }

    private static void collect(Class<?> c, Set<Class<?>> out) {
        while (c != null && c != Object.class) {
            out.add(c);
            for (Class<?> i : c.getInterfaces()) collect(i, out);
            c = c.getSuperclass();
        }
    }

    public void unregister(Object target) {
        if (target == null) return;
        synchronized (this) {
            List<Listener> mine = byTarget.remove(target);
            if (mine == null) return;
            all.removeAll(mine);
            cache.clear();
        }
    }

    private Listener[] listenersFor(Class<?> evClass) {
        Listener[] arr = cache.get(evClass);
        if (arr == null) {
            List<Listener> l = new ArrayList<Listener>();
            for (Listener x : all) if (x.type.isAssignableFrom(evClass)) l.add(x);
            Collections.sort(l, new Comparator<Listener>() {
                public int compare(Listener a, Listener b) { return a.prio != b.prio ? (a.prio < b.prio ? -1 : 1) : (a.seq < b.seq ? -1 : a.seq > b.seq ? 1 : 0); }
            });
            arr = l.toArray(new Listener[l.size()]);
            cache.put(evClass, arr);
        }
        return arr;
    }

    /** True when at least one handler would receive an event of this class (cheap pre-check for hot paths). */
    public boolean hasListeners(Class<?> evClass) { return listenersFor(evClass).length > 0; }

    /** @return true if the event ended up canceled */
    public boolean post(Event event) {
        Listener[] ls = listenersFor(event.getClass());
        for (Listener l : ls) {
            if (event.isCanceled() && !l.recvCanceled) continue;
            try {
                l.method.invoke(l.target, event);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                report(l, ite.getCause() != null ? ite.getCause() : ite);
            } catch (Throwable t) {
                report(l, t);
            }
        }
        return event.isCanceled();
    }

    private void report(Listener l, Throwable t) {
        String key = l.method.toString();
        Integer n = errors.get(key);
        int c = n == null ? 0 : n;
        if (c < 3) {
            errors.put(key, c + 1);
            System.err.println("[RavenXD] Exception in event handler " + key + (c == 2 ? " (further errors from this handler are suppressed)" : ""));
            t.printStackTrace();
        }
    }
}
