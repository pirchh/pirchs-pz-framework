package pirch.pz.lua;

import org.jetbrains.annotations.NotNull;

/**
 * Small adapter interface so patched engine code can call into PZLife without knowing bridge internals.
 */
public interface PZLifeLuaExposer {
    void exposeGlobalFunctions(@NotNull Object globalObject);
}
