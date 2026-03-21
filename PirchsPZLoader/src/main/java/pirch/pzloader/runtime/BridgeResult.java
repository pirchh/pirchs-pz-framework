package pirch.pzloader.runtime;

public final class BridgeResult {
    private final boolean success;
    private final Object data;
    private final String error;

    private BridgeResult(boolean success, Object data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static BridgeResult ok(Object data) {
        return new BridgeResult(true, data, null);
    }

    public static BridgeResult fail(String error) {
        return new BridgeResult(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        if (success) {
            return "BridgeResult{success=true, data=" + data + "}";
        }
        return "BridgeResult{success=false, error='" + error + "'}";
    }
}