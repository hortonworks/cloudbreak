package com.sequenceiq.environment.client;


import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.sequenceiq.cloudbreak.restclient.CloudbreakToStringStyle;
import com.sequenceiq.cloudbreak.restclient.ConfigKey;

public class UserCrnConfigKey extends ConfigKey {

    private final WebToken token;

    public UserCrnConfigKey(boolean secure, boolean debug, boolean ignorePreValidation, WebToken token) {
        super(secure, debug, ignorePreValidation);
        this.token = token;
    }

    public WebToken getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }

        UserCrnConfigKey userCrnConfigKey = (UserCrnConfigKey) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(token, userCrnConfigKey.token)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .appendSuper(super.hashCode())
                .append(token)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, CloudbreakToStringStyle.getInstance())
                .appendSuper(super.toString())
                .append("token", token)
                .build();
    }
}
