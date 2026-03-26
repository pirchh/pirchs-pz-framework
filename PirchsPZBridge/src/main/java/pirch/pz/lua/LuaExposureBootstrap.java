package pirch.pz.lua;

import java.lang.reflect.Method;
import pirch.pzloader.util.LoaderLog;
import zombie.Lua.LuaManager;

public final class LuaExposureBootstrap {
    private static boolean exposed = false;
    private static int attempts = 0;
    private static String lastStatus = "not_attempted";

    private LuaExposureBootstrap() {
    }

    public static synchronized boolean tryExposeNow() {
        attempts++;
        if (exposed) {
            lastStatus = "already_exposed";
            return true;
        }

        try {
            if (LuaManager.exposer == null) {
                lastStatus = "exposer_not_ready";
                if (attempts == 1 || attempts % 10 == 0) {
                    LoaderLog.info("[PZLIFE][LUA] LuaManager.exposer not ready yet. attempts=" + attempts);
                }
                return false;
            }

            Object exposer = LuaManager.exposer;
            PZLifeGlobalObject globalObject = new PZLifeGlobalObject();

            // Keep the lightweight facade exposed for compatibility with any older code paths.
            LuaManager.exposer.setExposed(LuaBridgeFacade.class);
            LuaManager.exposer.setExposed(PZLifeGlobalObject.class);

            Method exposeGlobalFunctions = findMethod(exposer.getClass(), "exposeGlobalFunctions", Object.class);
            if (exposeGlobalFunctions == null) {
                lastStatus = "missing_exposeGlobalFunctions";
                LoaderLog.error("[PZLIFE][LUA] Could not find exposeGlobalFunctions(Object) on Lua exposer.");
                return false;
            }

            exposeGlobalFunctions.setAccessible(true);
            exposeGlobalFunctions.invoke(exposer, globalObject);

            exposed = true;
            lastStatus = "exposed";
            LoaderLog.info("[PZLIFE][LUA] Exposed PZLife global bridge functions to Lua on attempt=" + attempts);
            return true;
        } catch (Throwable t) {
            lastStatus = "error:" + t.getClass().getSimpleName();
            LoaderLog.error("[PZLIFE][LUA] Failed to expose PZLife bridge globals on attempt=" + attempts + ": " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean tryExposeFromMainThread() {
        return tryExposeNow();
    }

    public static synchronized boolean isExposed() {
        return exposed;
    }

    public static synchronized int getAttempts() {
        return attempts;
    }

    public static synchronized String describeStatus() {
        return "exposed=" + exposed + ", attempts=" + attempts + ", lastStatus=" + lastStatus;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}
