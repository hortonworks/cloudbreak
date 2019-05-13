package com.sequenceiq.cloudbreak.restclient;


import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ConfigKey {

    private final boolean secure;

    private final boolean debug;

    private final boolean ignorePreValidation;

    public ConfigKey(boolean secure, boolean debug, boolean ignorePreValidation) {
        this.secure = secure;
        this.debug = debug;
        this.ignorePreValidation = ignorePreValidation;
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
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(secure)
                .append(debug)
                .append(ignorePreValidation)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, CloudbreakToStringStyle.getInstance())
                .append("secure", secure)
                .append("debug", debug)
                .append("ignorePreValidation", ignorePreValidation)
                .build();
    }
}
