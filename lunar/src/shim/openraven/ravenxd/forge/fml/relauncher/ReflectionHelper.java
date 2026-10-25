package openraven.ravenxd.forge.fml.relauncher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
public class ReflectionHelper {
    public static class UnableToFindFieldException extends RuntimeException { public UnableToFindFieldException(Exception e) { super(e); } }
    public static class UnableToFindMethodException extends RuntimeException { public UnableToFindMethodException(Exception e) { super(e); } }
    public static class UnableToAccessFieldException extends RuntimeException { public UnableToAccessFieldException(Exception e) { super(e); } }

    public static Field findField(Class<?> clazz, String... fieldNames) {
        Exception failed = null;
        for (String name : fieldNames) {
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
                catch (Exception e) { failed = e; }
            }
        }
        throw new UnableToFindFieldException(failed);
    }
    public static Method findMethod(Class<?> clazz, Object instance, String[] methodNames, Class<?>... methodTypes) {
        Exception failed = null;
        for (String name : methodNames) {
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                try { Method m = c.getDeclaredMethod(name, methodTypes); m.setAccessible(true); return m; }
                catch (Exception e) { failed = e; }
            }
        }
        throw new UnableToFindMethodException(failed);
    }
    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String... fieldNames) {
        try { return (T) findField(classToAccess, fieldNames).get(instance); }
        catch (IllegalAccessException e) { throw new UnableToAccessFieldException(e); }
    }
    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String... fieldNames) {
        try { findField(classToAccess, fieldNames).set(instance, value); }
        catch (IllegalAccessException e) { throw new UnableToAccessFieldException(e); }
    }
}
