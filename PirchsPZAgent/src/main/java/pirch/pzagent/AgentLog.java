package pirch.pzagent;

public final class AgentLog {
    private static final String PREFIX = "[PirchsPZAgent] ";

    private AgentLog() {
    }

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void error(String message) {
        System.err.println(PREFIX + message);
    }
}