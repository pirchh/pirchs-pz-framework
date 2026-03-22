package pirch.pzloader.runtime;

public final class BridgeMethodDefinition {
    private final String methodName;
    private final String version;
    private final String description;
    private final int minArgCount;

    private BridgeMethodDefinition(Builder builder) {
        this.methodName = builder.methodName;
        this.version = builder.version;
        this.description = builder.description;
        this.minArgCount = builder.minArgCount;
    }

    public static Builder builder(String methodName) {
        return new Builder(methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public int getMinArgCount() {
        return minArgCount;
    }

    public static final class Builder {
        private final String methodName;
        private String version = "v1";
        private String description = "";
        private int minArgCount = 0;

        private Builder(String methodName) {
            this.methodName = methodName;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder minArgCount(int minArgCount) {
            this.minArgCount = minArgCount;
            return this;
        }

        public BridgeMethodDefinition build() {
            if (methodName == null || methodName.isBlank()) {
                throw new IllegalArgumentException("methodName cannot be null or blank");
            }
            if (minArgCount < 0) {
                throw new IllegalArgumentException("minArgCount cannot be negative");
            }
            return new BridgeMethodDefinition(this);
        }
    }
}
