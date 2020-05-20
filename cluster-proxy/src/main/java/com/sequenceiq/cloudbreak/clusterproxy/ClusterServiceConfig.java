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
    private ClusterServiceHealthCheck healthCheck;

    @JsonCreator
    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate,
                                ClusterServiceHealthCheck healthCheck) {
        this.name = serviceName;
        this.endpoints = endpoints;
        this.credentials = credentials;
        this.clientCertificate = clientCertificate;
        this.healthCheck = healthCheck;
    }

    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate) {
        this(serviceName, endpoints, credentials, clientCertificate, null);
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
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
                Objects.equals(healthCheck, that.healthCheck);
    }
    //CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints, credentials, clientCertificate, healthCheck);
    }

    @Override
    public String toString() {
        return "ClusterServiceConfig{serviceName='" + name + '\''
                + ", endpoints=" + endpoints
                + ", credentials=" + credentials
                + ", clientCertificate=" + clientCertificate
                + ", healthCheck=" + healthCheck
                + '}';
    }
}
