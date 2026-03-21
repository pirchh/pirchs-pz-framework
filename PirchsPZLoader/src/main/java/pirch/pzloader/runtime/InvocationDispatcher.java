package pirch.pzloader.runtime;

import pirch.pzloader.util.LoaderLog;

public final class InvocationDispatcher {
    private InvocationDispatcher() {
    }

    public static BridgeResult invoke(String methodName, Object... args) {
        ModuleRegistry.BridgeMethod handler = ModuleRegistry.get(methodName);

        if (handler == null) {
            String error = "No such method registered: " + methodName;
            LoaderLog.error(error);
            return BridgeResult.fail(error);
        }

        try {
            Object result = handler.invoke(args);

            if (result instanceof BridgeResult) {
                BridgeResult bridgeResult = (BridgeResult) result;
                LoaderLog.info("Invoked method: " + methodName + " -> " + bridgeResult);
                return bridgeResult;
            }

            BridgeResult wrapped = BridgeResult.ok(result);
            LoaderLog.info("Invoked method: " + methodName + " -> " + wrapped);
            return wrapped;
        } catch (Exception ex) {
            String error = "Invocation failed for " + methodName + ": " + ex.getMessage();
            LoaderLog.error(error);
            ex.printStackTrace();
            return BridgeResult.fail(error);
        }
    }
}