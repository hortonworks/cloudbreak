package com.sequenceiq.cloudbreak.telemetry.context;

public class MeteringContext {

    private final boolean enabled;

    private final String serviceType;

    private final String version;

    private MeteringContext(Builder builder) {
        this.enabled = builder.enabled;
        this.serviceType = builder.serviceType;
        this.version = builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "MeteringContext{" +
                "enabled=" + enabled +
                ", serviceType='" + serviceType + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public static class Builder {

        private boolean enabled;

        private String serviceType;

        private String version;

        private Builder() {
        }

        public MeteringContext build() {
            return new MeteringContext(this);
        }

        public Builder enabled() {
            this.enabled = true;
            return this;
        }

        public Builder withServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

    }
}
