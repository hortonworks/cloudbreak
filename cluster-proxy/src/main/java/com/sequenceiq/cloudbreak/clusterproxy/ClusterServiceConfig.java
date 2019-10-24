package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceConfig {

    @JsonProperty
    private String name;

    @JsonProperty
    private List<String> endpoints;

    @JsonProperty
    private List<ClusterServiceCredential> credentials;

    @JsonProperty
    private ClientCertificate clientCertificate;

    @JsonProperty
    private Boolean tlsStrictCheck;

    @JsonProperty
    private Boolean useTunnel;

    @JsonProperty
    private List<Tunnel> tunnels;

    @JsonProperty
    private String accountId;

    @JsonCreator
    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate,
                                Boolean tlsStrictCheck, Boolean useTunnel, List<Tunnel> tunnels, String accountId) {
        this.name = serviceName;
        this.endpoints = endpoints;
        this.credentials = credentials;
        this.clientCertificate = clientCertificate;
        this.tlsStrictCheck = tlsStrictCheck;
        this.useTunnel = useTunnel;
        this.tunnels = tunnels;
        this.accountId = accountId;
    }

    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate,
                                Boolean tlsStrictCheck) {
        this(serviceName, endpoints, credentials, clientCertificate, tlsStrictCheck, false, List.of(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClusterServiceConfig that = (ClusterServiceConfig) o;

        return Objects.equals(name, that.name) &&
                Objects.equals(endpoints, that.endpoints) &&
                Objects.equals(credentials, that.credentials) &&
                Objects.equals(clientCertificate, that.clientCertificate) &&
                Objects.equals(tlsStrictCheck, that.tlsStrictCheck) &&
                Objects.equals(useTunnel, that.useTunnel) &&
                Objects.equals(tunnels, that.tunnels) &&
                Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints, credentials, clientCertificate, tlsStrictCheck, useTunnel, tunnels, accountId);
    }

    @Override
    public String toString() {
        return "ClusterServiceConfig{serviceName='" + name + '\''
                + ", endpoints=" + endpoints
                + ", credentials=" + credentials
                + ", clientCertificate=" + clientCertificate
                + ", tlsStrictCheck=" + tlsStrictCheck
                + ", useTunnel=" + useTunnel
                + ", tunnels=" + tunnels
                + ", accountId=" + accountId
                + '}';
    }
}
