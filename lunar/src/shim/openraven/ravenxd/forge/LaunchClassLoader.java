package openraven.ravenxd.forge;
/** Stand-in for LaunchWrapper's class loader (not present on Lunar). Used only as a parent loader by the script compiler. */
public class LaunchClassLoader extends ClassLoader { public LaunchClassLoader(ClassLoader parent) { super(parent); } }
