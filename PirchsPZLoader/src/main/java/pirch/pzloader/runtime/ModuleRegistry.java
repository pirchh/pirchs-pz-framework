package pirch.pzloader.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class ModuleRegistry {
    private static final Object LOCK = new Object();
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

        String methodName = normalizeMethodName(definition.getMethodName());
        BridgeMethodDefinition normalizedDefinition = BridgeMethodDefinition.builder(methodName)
            .version(definition.getVersion())
            .description(definition.getDescription())
            .minArgCount(definition.getMinArgCount())
            .build();

        synchronized (LOCK) {
            if (METHODS.containsKey(methodName)) {
                LoaderLog.info("Overwriting existing method registration: " + methodName);
            }

            METHODS.put(methodName, new RegisteredMethod(normalizedDefinition, handler));
            LoaderLog.info("Registered method: " + methodName + " (" + normalizedDefinition.getVersion() + ")");
        }
    }

    public static BridgeMethod get(String methodName) {
        synchronized (LOCK) {
            RegisteredMethod registeredMethod = METHODS.get(normalizeMethodNameOrNull(methodName));
            return registeredMethod == null ? null : registeredMethod.handler();
        }
    }

    public static BridgeMethodDefinition getDefinition(String methodName) {
        synchronized (LOCK) {
            RegisteredMethod registeredMethod = METHODS.get(normalizeMethodNameOrNull(methodName));
            return registeredMethod == null ? null : registeredMethod.definition();
        }
    }

    public static boolean has(String methodName) {
        synchronized (LOCK) {
            return METHODS.containsKey(normalizeMethodNameOrNull(methodName));
        }
    }

    public static int count() {
        synchronized (LOCK) {
            return METHODS.size();
        }
    }

    public static Map<String, BridgeMethodDefinition> getAllDefinitions() {
        synchronized (LOCK) {
            Map<String, BridgeMethodDefinition> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, RegisteredMethod> entry : METHODS.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().definition());
            }
            return Collections.unmodifiableMap(snapshot);
        }
    }

    public static List<String> listMethodNames() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<>(METHODS.keySet()));
        }
    }

    private static String normalizeMethodName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("methodName cannot be null or blank");
        }
        return methodName.trim();
    }

    private static String normalizeMethodNameOrNull(String methodName) {
        if (methodName == null) {
            return null;
        }
        String normalized = methodName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @FunctionalInterface
    public interface BridgeMethod {
        Object invoke(Object... args);
    }

    private record RegisteredMethod(BridgeMethodDefinition definition, BridgeMethod handler) {
    }
}
