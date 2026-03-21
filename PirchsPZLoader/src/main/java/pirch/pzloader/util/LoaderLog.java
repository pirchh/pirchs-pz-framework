package pirch.pzloader.util;

public final class LoaderLog {
    private static final String PREFIX = "[PirchsPZLoader] ";

    private LoaderLog() {
    }

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void error(String message) {
        System.err.println(PREFIX + message);
    }
}