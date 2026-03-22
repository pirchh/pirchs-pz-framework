package pirch.pzloader.runtime;

import pirch.pzloader.util.LoaderLog;

public final class InvocationDispatcher {
    private InvocationDispatcher() {
    }

    public static BridgeResult invoke(String methodName, Object... args) {
        return invoke(BridgeRequest.of(methodName, args));
    }

    public static BridgeResult invoke(BridgeRequest request) {
        if (request == null) {
            return BridgeResult.fail("request cannot be null");
        }

        String methodName = request.getMethodName();
        if (methodName == null || methodName.isBlank()) {
            return BridgeResult.fail("methodName cannot be null or blank");
        }

        ModuleRegistry.BridgeMethod handler = ModuleRegistry.get(methodName);
        if (handler == null) {
            String error = "No such method registered: " + methodName;
            LoaderLog.error(error);
            return BridgeResult.fail(error);
        }

        BridgeMethodDefinition definition = ModuleRegistry.getDefinition(methodName);
        if (definition != null && request.argCount() < definition.getMinArgCount()) {
            String error = "Method " + methodName + " requires at least " + definition.getMinArgCount() + " argument(s)";
            LoaderLog.error(error);
            return BridgeResult.fail(error);
        }

        try {
            Object result = handler.invoke(request.getArgs());
            if (result instanceof BridgeResult bridgeResult) {
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
