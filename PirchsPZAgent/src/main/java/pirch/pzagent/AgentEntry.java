package pirch.pzagent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Path;

public final class AgentEntry {
    private static final String DEFAULT_MOD_LIB =
        "C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib";

    private static final String DEFAULT_BOOTSTRAP_CLASS =
        "pirch.pz.BridgeBootstrap";

    private AgentEntry() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        start(agentArgs);
    }

    public static void premain(String agentArgs) {
        start(agentArgs);
    }

    private static void start(String agentArgs) {
        try {
            AgentLog.info("Agent starting...");

            String modLibPath = parseModLibPath(agentArgs);
            AgentLog.info("Using mod lib path: " + modLibPath);

            ClassLoader modClassLoader = ModJarLoader.loadModLibFolder(Path.of(modLibPath));

            Class<?> bootstrapClass = Class.forName(
                DEFAULT_BOOTSTRAP_CLASS,
                true,
                modClassLoader
            );

            Method initializeMethod = bootstrapClass.getMethod("initialize");
            initializeMethod.invoke(null);

            AgentLog.info(DEFAULT_BOOTSTRAP_CLASS + ".initialize() completed.");
        } catch (Throwable t) {
            AgentLog.error("Agent startup failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static String parseModLibPath(String agentArgs) {
        if (agentArgs == null || agentArgs.isBlank()) {
            return DEFAULT_MOD_LIB;
        }

        String trimmed = agentArgs.trim();
        if (trimmed.startsWith("modLib=")) {
            return trimmed.substring("modLib=".length());
        }

        return DEFAULT_MOD_LIB;
    }
}