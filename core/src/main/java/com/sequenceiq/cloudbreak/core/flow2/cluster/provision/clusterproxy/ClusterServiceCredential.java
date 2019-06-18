package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ClusterServiceCredential {
    @JsonProperty
    private String username;

    @JsonProperty
    private String credentialRef;

    @JsonCreator
    ClusterServiceCredential(String username, String credentialRef) {
        this.username = username;
        this.credentialRef = credentialRef;
    }

    @Override
    public String toString() {
        return "ClusterServiceCredential{username='" + username + '\'' + ", credentialRef='" + credentialRef + '\'' + '}';
    }
}
