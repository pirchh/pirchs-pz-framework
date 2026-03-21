package pirch.pzloader.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class ModuleRegistry {
    private static final Map<String, BridgeMethod> METHODS = new HashMap<>();

    private ModuleRegistry() {
    }

    public static void register(String methodName, BridgeMethod handler) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("methodName cannot be null or blank");
        }

        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        if (METHODS.containsKey(methodName)) {
            LoaderLog.info("Overwriting existing method registration: " + methodName);
        }

        METHODS.put(methodName, handler);
        LoaderLog.info("Registered method: " + methodName);
    }

    public static BridgeMethod get(String methodName) {
        return METHODS.get(methodName);
    }

    public static boolean has(String methodName) {
        return METHODS.containsKey(methodName);
    }

    public static int count() {
        return METHODS.size();
    }

    public static Map<String, BridgeMethod> getAll() {
        return Collections.unmodifiableMap(METHODS);
    }

    @FunctionalInterface
    public interface BridgeMethod {
        Object invoke(Object... args);
    }
}