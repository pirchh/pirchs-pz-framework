package pirch.pzagent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentEntry {
    private static final String DEFAULT_MOD_LIB = "C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib";
    private static final String DEFAULT_BOOTSTRAP_CLASS = "pirch.pz.BridgeBootstrap";

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

            Map<String, String> parsedArgs = parseAgentArgs(agentArgs);
            String modLibPath = resolveModLibPath(parsedArgs);
            String bootstrapClassName = parsedArgs.getOrDefault("bootstrapClass", DEFAULT_BOOTSTRAP_CLASS).trim();

            AgentLog.info("Using mod lib path: " + modLibPath);
            AgentLog.info("Using bootstrap class: " + bootstrapClassName);

            Path modLibFolder = Path.of(modLibPath).toAbsolutePath().normalize();
            validateModLibFolder(modLibFolder);

            ClassLoader modClassLoader = ModJarLoader.loadModLibFolder(modLibFolder);
            Class<?> bootstrapClass = Class.forName(bootstrapClassName, true, modClassLoader);
            Method initializeMethod = bootstrapClass.getMethod("initialize");
            initializeMethod.invoke(null);

            AgentLog.info(bootstrapClassName + ".initialize() completed.");
        } catch (Throwable t) {
            AgentLog.error("Agent startup failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void validateModLibFolder(Path modLibFolder) throws IOException {
        if (!Files.exists(modLibFolder)) {
            throw new IllegalStateException("Configured mod lib folder does not exist: " + modLibFolder);
        }
        if (!Files.isDirectory(modLibFolder)) {
            throw new IllegalStateException("Configured mod lib path is not a directory: " + modLibFolder);
        }

        long jarCount;
        try (var stream = Files.list(modLibFolder)) {
            jarCount = stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString().toLowerCase())
                .filter(name -> name.endsWith(".jar"))
                .count();
        }

        if (jarCount == 0) {
            AgentLog.info("No jars were found in mod lib folder yet: " + modLibFolder);
        } else {
            AgentLog.info("Discovered " + jarCount + " jar(s) in mod lib folder.");
        }
    }

    private static String resolveModLibPath(Map<String, String> parsedArgs) {
        String fromArgs = parsedArgs.get("modLib");
        if (fromArgs != null && !fromArgs.isBlank()) {
            return fromArgs.trim();
        }

        String fromProperty = System.getProperty("pirch.pz.modLib", "").trim();
        if (!fromProperty.isEmpty()) {
            return fromProperty;
        }

        String fromEnv = System.getenv("PIRCHS_PZ_MOD_LIB");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        AgentLog.info("No modLib override supplied; falling back to default path.");
        return DEFAULT_MOD_LIB;
    }

    private static Map<String, String> parseAgentArgs(String agentArgs) {
        Map<String, String> values = new LinkedHashMap<>();
        if (agentArgs == null || agentArgs.isBlank()) {
            return values;
        }

        String[] parts = agentArgs.split(";");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }

            String[] entry = part.split("=", 2);
            if (entry.length != 2) {
                continue;
            }

            String key = entry[0].trim();
            String value = entry[1].trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
            }
        }

        if (!values.containsKey("modLib") && agentArgs.startsWith("modLib=")) {
            values.put("modLib", agentArgs.substring("modLib=".length()).trim());
        }

        return values;
    }
}
