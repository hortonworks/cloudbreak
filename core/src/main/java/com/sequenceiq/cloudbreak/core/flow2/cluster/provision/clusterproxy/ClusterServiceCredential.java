package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

class ClusterServiceCredential {
    private String username;

    private String credentialRef;

    ClusterServiceCredential(String username, String credentialRef) {
        this.username = username;
        this.credentialRef = credentialRef;
    }

    @Override
    public String toString() {
        return "ClusterServiceCredential{username='" + username + '\'' + ", credentialRef='" + credentialRef + '\'' + '}';
    }
}
