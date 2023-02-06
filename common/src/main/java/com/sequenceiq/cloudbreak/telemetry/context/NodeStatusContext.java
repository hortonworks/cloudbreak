package com.sequenceiq.cloudbreak.telemetry.context;

public class NodeStatusContext {

    private final String username;

    private final String password;

    private final boolean saltPingEnabled;

    private NodeStatusContext(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.saltPingEnabled = builder.saltPingEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSaltPingEnabled() {
        return saltPingEnabled;
    }

    @Override
    public String toString() {
        return "NodeStatusContext{" +
                "username='" + username + '\'' +
                ", password=****" +
                ", saltPingEnabled=" + saltPingEnabled +
                '}';
    }

    public static class Builder {

        private String username;

        private String password;

        private boolean saltPingEnabled;

        private Builder() {
        }

        public NodeStatusContext build() {
            return new NodeStatusContext(this);
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder saltPingEnabled() {
            this.saltPingEnabled = true;
            return this;
        }
    }
}
