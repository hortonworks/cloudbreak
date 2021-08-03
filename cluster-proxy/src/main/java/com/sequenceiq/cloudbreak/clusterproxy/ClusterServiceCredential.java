package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceCredential {

    @JsonProperty
    private final String username;

    @JsonProperty
    private final String credentialRef;

    @JsonProperty
    private final boolean isDefault;

    @JsonCreator
    public ClusterServiceCredential(String username, String credentialRef) {
        this(username, credentialRef, false);
    }

    @JsonCreator
    public ClusterServiceCredential(String username, String credentialRef, boolean isDefault) {
        this.username = username;
        this.credentialRef = credentialRef;
        this.isDefault = isDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClusterServiceCredential that = (ClusterServiceCredential) o;

        return isDefault == that.isDefault &&
                Objects.equals(username, that.username) &&
                Objects.equals(credentialRef, that.credentialRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDefault, username, credentialRef);
    }

    @Override
    public String toString() {
        return "ClusterServiceCredential{username='" + username + '\'' + ", credentialRef='" + credentialRef + '\'' + ", isDefault='" + isDefault + '\'' + '}';
    }
}
