package pirch.pz.lua;

import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.InvocationDispatcher;
import pirch.pzloader.runtime.ModuleRegistry;

public final class LuaBridgeFacade {
    private LuaBridgeFacade() {
    }

    public static boolean hasMethod(String methodName) {
        return methodName != null && ModuleRegistry.has(methodName);
    }

    public static Object invoke(String methodName) {
        return invokeInternal(methodName);
    }

    public static Object invoke(String methodName, Object arg1) {
        return invokeInternal(methodName, arg1);
    }

    public static Object invoke(String methodName, Object arg1, Object arg2) {
        return invokeInternal(methodName, arg1, arg2);
    }

    public static Object invoke(String methodName, Object arg1, Object arg2, Object arg3) {
        return invokeInternal(methodName, arg1, arg2, arg3);
    }

    private static Object invokeInternal(String methodName, Object... args) {
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
