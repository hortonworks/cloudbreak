package com.sequenceiq.cloudbreak.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceCredential {
    @JsonProperty
    private String username;

    @JsonProperty
    private String credentialRef;

    @JsonProperty
    private boolean isDefault;

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
    public String toString() {
        return "ClusterServiceCredential{username='" + username + '\'' + ", credentialRef='" + credentialRef + '\'' + ", isDefault='" + isDefault + '\'' + '}';
    }
}
