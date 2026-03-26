package pirch.pz.lua;

import org.jetbrains.annotations.Nullable;
import pirch.pzloader.runtime.InvocationDispatcher;
import pirch.pzloader.runtime.ModuleRegistry;
import se.krka.kahlua.integration.annotations.LuaMethod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin Lua-facing host object.
 *
 * <p>This is intentionally tiny. Lua should only see a stable set of global functions and never
 * need to resolve internal bridge classes by package path.</p>
 */
@SuppressWarnings("unused")
public final class PZLifeGlobalObject {

    @LuaMethod(global = true)
    public static boolean pzlife() {
        return true;
    }

    @LuaMethod(global = true)
    public static boolean pzlife_has(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return false;
        }
        return ModuleRegistry.has(methodName);
    }

    @LuaMethod(global = true)
    public static Object pzlife_invoke0(String methodName) {
        return InvocationDispatcher.invoke(methodName);
    }

    @LuaMethod(global = true)
    public static Object pzlife_invoke1(String methodName, Object arg1) {
        return InvocationDispatcher.invoke(methodName, arg1);
    }

    @LuaMethod(global = true)
    public static Object pzlife_invoke2(String methodName, Object arg1, Object arg2) {
        return InvocationDispatcher.invoke(methodName, arg1, arg2);
    }

    @LuaMethod(global = true)
    public static Object pzlife_invoke3(String methodName, Object arg1, Object arg2, Object arg3) {
        return InvocationDispatcher.invoke(methodName, arg1, arg2, arg3);
    }

    @LuaMethod(global = true)
    public static String pzlife_bridge_status() {
        return snapshot().toString();
    }

    @LuaMethod(global = true)
    public static String pzlife_bridge_ping(@Nullable String value) {
        return value == null ? "pong" : "pong:" + value;
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("present", true);
        out.put("debugBridgeEnabled", ModuleRegistry.has("pz.bridge.debug.isLocalIdentityReady"));
        out.put("hasInvoke0", true);
        out.put("hasInvoke1", true);
        out.put("hasInvoke2", true);
        out.put("hasInvoke3", true);
        return out;
    }
}
