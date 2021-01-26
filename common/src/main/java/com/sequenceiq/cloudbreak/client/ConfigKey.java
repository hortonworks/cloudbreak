package com.sequenceiq.cloudbreak.client;


import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ConfigKey {

    private final boolean secure;

    private final boolean debug;

    private final boolean ignorePreValidation;

    private final Optional<Integer> timeout;

    private ConfigKey(ConfigKey.Builder builder) {
        this.secure = builder.secure;
        this.debug = builder.debug;
        this.ignorePreValidation = builder.ignorePreValidation;
        this.timeout = builder.timeout;
    }

    public ConfigKey(boolean secure, boolean debug, boolean ignorePreValidation) {
        this.secure = secure;
        this.debug = debug;
        this.ignorePreValidation = ignorePreValidation;
        this.timeout = Optional.empty();
    }

    public ConfigKey(boolean secure, boolean debug, boolean ignorePreValidation, int timeout) {
        this.secure = secure;
        this.debug = debug;
        this.ignorePreValidation = ignorePreValidation;
        this.timeout = Optional.of(timeout);
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isIgnorePreValidation() {
        return ignorePreValidation;
    }

    public Optional<Integer> getTimeout() {
        return timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }

        ConfigKey configKey = (ConfigKey) o;

        return new EqualsBuilder()
                .append(secure, configKey.secure)
                .append(debug, configKey.debug)
                .append(ignorePreValidation, configKey.ignorePreValidation)
                .append(timeout, configKey.timeout)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(secure)
                .append(debug)
                .append(ignorePreValidation)
                .append(timeout.orElse(null))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, CloudbreakToStringStyle.getInstance())
                .append("secure", secure)
                .append("debug", debug)
                .append("ignorePreValidation", ignorePreValidation)
                .append("timeout", timeout)
                .build();
    }

    public static ConfigKey.Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean secure;

        private boolean debug;

        private boolean ignorePreValidation;

        private Optional<Integer> timeout = Optional.empty();

        public ConfigKey build() {

            return new ConfigKey(this);
        }

        public Builder withSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder withIgnorePreValidation(boolean ignorePreValidation) {
            this.ignorePreValidation = ignorePreValidation;
            return this;
        }

        public Builder withTimeOut(Integer timeout) {
            this.timeout = Optional.of(timeout);
            return this;
        }
    }
}
