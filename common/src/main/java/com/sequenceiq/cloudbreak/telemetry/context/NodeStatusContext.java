package com.sequenceiq.cloudbreak.telemetry.context;

public class NodeStatusContext {

    private final String password;

    private NodeStatusContext(Builder builder) {
        this.password = builder.password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "NodeStatusContext{" +
                ", password=****" +
                '}';
    }

    public static class Builder {

        private String password;

        private Builder() {
        }

        public NodeStatusContext build() {
            return new NodeStatusContext(this);
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }
    }
}
