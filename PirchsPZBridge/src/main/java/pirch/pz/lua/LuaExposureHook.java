package pirch.pz.lua;

public final class LuaExposureHook {
    private LuaExposureHook() {
    }

    public static boolean ensureBridgeExposed() {
        return LuaExposureBootstrap.tryExposeFromMainThread();
    }

    public static boolean isBridgeExposed() {
        return LuaExposureBootstrap.isExposed();
    }

    public static int getExposureAttempts() {
        return LuaExposureBootstrap.getAttempts();
    }

    public static String getExposureStatus() {
        return LuaExposureBootstrap.describeStatus();
    }
}
