package pirch.pzloader.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class ModuleRegistry {
    private static final Map<String, RegisteredMethod> METHODS = new LinkedHashMap<>();

    private ModuleRegistry() {
    }

    public static void register(String methodName, BridgeMethod handler) {
        register(
            BridgeMethodDefinition.builder(methodName)
                .description("Legacy bridge method registration")
                .build(),
            handler
        );
    }

    public static void register(BridgeMethodDefinition definition, BridgeMethod handler) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        String methodName = definition.getMethodName();
        if (METHODS.containsKey(methodName)) {
            LoaderLog.info("Overwriting existing method registration: " + methodName);
        }

        METHODS.put(methodName, new RegisteredMethod(definition, handler));
        LoaderLog.info("Registered method: " + methodName + " (" + definition.getVersion() + ")");
    }

    public static BridgeMethod get(String methodName) {
        RegisteredMethod registeredMethod = METHODS.get(methodName);
        return registeredMethod == null ? null : registeredMethod.handler();
    }

    public static BridgeMethodDefinition getDefinition(String methodName) {
        RegisteredMethod registeredMethod = METHODS.get(methodName);
        return registeredMethod == null ? null : registeredMethod.definition();
    }

    public static boolean has(String methodName) {
        return METHODS.containsKey(methodName);
    }

    public static int count() {
        return METHODS.size();
    }

    public static Map<String, BridgeMethodDefinition> getAllDefinitions() {
        Map<String, BridgeMethodDefinition> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, RegisteredMethod> entry : METHODS.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().definition());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    @FunctionalInterface
    public interface BridgeMethod {
        Object invoke(Object... args);
    }

    private record RegisteredMethod(BridgeMethodDefinition definition, BridgeMethod handler) {
    }
}
