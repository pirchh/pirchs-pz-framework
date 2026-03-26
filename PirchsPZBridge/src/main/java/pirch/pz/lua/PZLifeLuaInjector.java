package pirch.pz.lua;

import zombie.debug.DebugLog;

/**
 * Called from the patched zombie.Lua.LuaManager.Exposer once the engine Lua environment is being built.
 */
public final class PZLifeLuaInjector {
    private static volatile boolean exposed;

    private PZLifeLuaInjector() {
    }

    public static void expose(PZLifeLuaExposer exposer) {
        if (exposer == null) {
            return;
        }
        try {
            exposer.exposeGlobalFunctions(new PZLifeGlobalObject());
            exposed = true;
            DebugLog.Lua.debugln("[PZLIFE][JAVA][lua] exposed PZLifeGlobalObject into main Lua environment");
        } catch (Throwable t) {
            DebugLog.Lua.error("[PZLIFE][JAVA][lua] failed to expose PZLifeGlobalObject: " + t);
        }
    }

    public static boolean isExposed() {
        return exposed;
    }
}
