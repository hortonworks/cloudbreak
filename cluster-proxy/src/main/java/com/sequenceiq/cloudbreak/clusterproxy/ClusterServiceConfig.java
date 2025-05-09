package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceConfig {

    @JsonProperty
    private final String name;

    @JsonProperty
    private final List<String> endpoints;

    @JsonProperty
    private final AuthenticationType authenticationType;

    @JsonProperty
    private final boolean removeProxyContentType;

    @JsonProperty
    private final List<ClusterServiceCredential> credentials;

    @JsonProperty
    private final ClientCertificate clientCertificate;

    @JsonCreator
    public ClusterServiceConfig(String serviceName, List<String> endpoints, AuthenticationType authenticationType, boolean removeProxyContentType,
            List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate) {
        this.name = serviceName;
        this.endpoints = endpoints;
        this.authenticationType = authenticationType;
        this.removeProxyContentType = removeProxyContentType;
        this.credentials = credentials;
        this.clientCertificate = clientCertificate;
    }

    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials, ClientCertificate clientCertificate) {
        this(serviceName, endpoints, null, false, credentials, clientCertificate);
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
        return removeProxyContentType == that.removeProxyContentType &&
                Objects.equals(name, that.name) &&
                Objects.equals(endpoints, that.endpoints) &&
                Objects.equals(authenticationType, that.authenticationType) &&
                Objects.equals(credentials, that.credentials) &&
                Objects.equals(clientCertificate, that.clientCertificate);
    }
    //CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints, authenticationType, removeProxyContentType, credentials, clientCertificate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterServiceConfig.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("endpoints=" + endpoints)
                .add("authenticationType='" + authenticationType + "'")
                .add("removeProxyContentType=" + removeProxyContentType)
                .add("credentials=" + credentials)
                .add("clientCertificate=" + clientCertificate)
                .toString();
    }
}
