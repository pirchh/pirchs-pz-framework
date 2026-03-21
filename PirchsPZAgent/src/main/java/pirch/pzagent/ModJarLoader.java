package pirch.pzagent;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModJarLoader {
    private ModJarLoader() {
    }

    public static ClassLoader loadModLibFolder(Path libFolder) throws IOException {
        if (!Files.exists(libFolder)) {
            throw new IOException("Mod lib folder does not exist: " + libFolder);
        }

        List<URL> urls = new ArrayList<>();

        try (var paths = Files.list(libFolder)) {
            paths.filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                 .sorted()
                 .forEach(path -> {
                     try {
                         AgentLog.info("Found mod jar: " + path);
                         urls.add(path.toUri().toURL());
                     } catch (Exception ex) {
                         throw new RuntimeException("Failed to convert jar path to URL: " + path, ex);
                     }
                 });
        }

        if (urls.isEmpty()) {
            throw new IOException("No jars found in mod lib folder: " + libFolder);
        }

        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }
}