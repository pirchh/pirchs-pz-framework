package pirch.pz.lua;

import pirch.pzloader.util.LoaderLog;
import zombie.Lua.LuaManager;

public final class LuaExposureBootstrap {
    private static boolean exposed = false;
    private static int attempts = 0;

    private LuaExposureBootstrap() {
    }

    public static synchronized boolean tryExposeNow() {
        attempts++;

        if (exposed) {
            return true;
        }

        try {
            if (LuaManager.exposer == null) {
                if (attempts == 1 || attempts % 10 == 0) {
                    LoaderLog.info("[PZLIFE][LUA] LuaManager.exposer not ready yet. attempts=" + attempts);
                }
                return false;
            }

            LuaManager.exposer.setExposed(LuaBridgeFacade.class);
            exposed = true;
            LoaderLog.info("[PZLIFE][LUA] Exposed LuaBridgeFacade to Lua on attempt=" + attempts);
            return true;
        } catch (Throwable t) {
            LoaderLog.error("[PZLIFE][LUA] Failed to expose LuaBridgeFacade on attempt=" + attempts + ": " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean tryExposeFromMainThread() {
        attempts++;

        if (exposed) {
            return true;
        }

        try {
            if (LuaManager.exposer == null) {
                if (attempts == 1 || attempts % 10 == 0) {
                    LoaderLog.info("[PZLIFE][LUA] LuaManager.exposer still not ready on main-thread expose attempt. attempts=" + attempts);
                }
                return false;
            }

            LuaManager.exposer.setExposed(LuaBridgeFacade.class);
            LuaManager.exposer.exposeAll();

            exposed = true;
            LoaderLog.info("[PZLIFE][LUA] Main-thread exposure completed for LuaBridgeFacade on attempt=" + attempts);
            return true;
        } catch (Throwable t) {
            LoaderLog.error("[PZLIFE][LUA] Main-thread exposure failed on attempt=" + attempts + ": " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean isExposed() {
        return exposed;
    }

    public static synchronized int getAttempts() {
        return attempts;
    }
}
