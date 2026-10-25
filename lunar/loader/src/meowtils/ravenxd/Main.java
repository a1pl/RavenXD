package meowtils.ravenxd;

import wtf.tatp.meowtils.extension.Extension;

/** Meowtils extension entry point (META-INF/meowtils.extension: main=meowtils.ravenxd.Main). */
public final class Main {
    private Main() {
    }

    public static void init() {
        Extension.registerModule(new RavenXDLoader());
    }
}
