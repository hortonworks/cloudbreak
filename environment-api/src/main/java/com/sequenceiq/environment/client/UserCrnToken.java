package com.sequenceiq.environment.client;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.sequenceiq.cloudbreak.restclient.CloudbreakToStringStyle;

public class UserCrnToken implements WebToken {
    protected static final String CRN_HEADER = "x-cdp-actor-crn";

    private String token;

    public UserCrnToken(String token) {
        this.token = token;
    }

    @Override
    public String getHeader() {
        return CRN_HEADER;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }

        UserCrnToken userCrnToken = (UserCrnToken) o;

        return new EqualsBuilder()
                .append(token, userCrnToken.token)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(token)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, CloudbreakToStringStyle.getInstance())
                .append("token", token)
                .build();
    }
}
