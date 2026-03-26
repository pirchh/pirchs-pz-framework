package pirch.pz.lua;

import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.InvocationDispatcher;
import pirch.pzloader.runtime.ModuleRegistry;

/**
 * Exposed as global Lua functions through LuaManager.exposer.exposeGlobalFunctions(...).
 *
 * This avoids relying on Lua package-path access or luajava, and mirrors the
 * GlobalObject-style global function pattern that Project Zomboid Lua already uses.
 */
public final class LuaBridgeGlobals {

    public boolean PZLifeBridgeReady() {
        return true;
    }

    public boolean PZLifeBridgeHasMethod(String methodName) {
        return methodName != null && ModuleRegistry.has(methodName);
    }

    public Object PZLifeBridgeInvoke0(String methodName) {
        return invokeInternal(methodName);
    }

    public Object PZLifeBridgeInvoke1(String methodName, Object arg1) {
        return invokeInternal(methodName, arg1);
    }

    public Object PZLifeBridgeInvoke2(String methodName, Object arg1, Object arg2) {
        return invokeInternal(methodName, arg1, arg2);
    }

    public Object PZLifeBridgeInvoke3(String methodName, Object arg1, Object arg2, Object arg3) {
        return invokeInternal(methodName, arg1, arg2, arg3);
    }

    private Object invokeInternal(String methodName, Object... args) {
        BridgeResult result = InvocationDispatcher.invoke(methodName, args);
        if (result == null) {
            throw new IllegalStateException("bridge invocation returned null for " + methodName);
        }
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getError());
        }
        return result.getData();
    }
}
