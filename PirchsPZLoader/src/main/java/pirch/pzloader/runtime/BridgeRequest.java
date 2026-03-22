package pirch.pzloader.runtime;

import java.util.Arrays;

public final class BridgeRequest {
    private final String methodName;
    private final Object[] args;

    private BridgeRequest(String methodName, Object[] args) {
        this.methodName = methodName;
        this.args = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
    }

    public static BridgeRequest of(String methodName, Object... args) {
        return new BridgeRequest(methodName, args);
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArgs() {
        return Arrays.copyOf(args, args.length);
    }

    public int argCount() {
        return args.length;
    }
}
