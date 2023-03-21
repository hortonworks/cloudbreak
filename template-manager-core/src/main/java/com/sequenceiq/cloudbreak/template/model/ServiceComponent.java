package com.sequenceiq.cloudbreak.template.model;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceComponent implements Comparable<ServiceComponent> {

    private static final String UNKNOWN_SERVICE = "";

    private final Pair<String, String> serviceComponent;

    @JsonCreator
    private ServiceComponent(@JsonProperty("service") String service,
            @JsonProperty("component") String component) {
        serviceComponent = Pair.of(service, component);
    }

    /**
     * Factory method for CM.
     */
    public static ServiceComponent of(String service, String component) {
        return new ServiceComponent(service, component);
    }

    /**
     * Factory method for Ambari.
     */
    public static ServiceComponent of(String component) {
        return new ServiceComponent(UNKNOWN_SERVICE, component);
    }

    public String getService() {
        return serviceComponent.getLeft();
    }

    public String getComponent() {
        return serviceComponent.getRight();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ServiceComponent other = (ServiceComponent) obj;
        return Objects.equals(serviceComponent, other.serviceComponent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceComponent);
    }

    @Override
    public String toString() {
        return Objects.toString(serviceComponent);
    }

    @Override
    public int compareTo(@Nonnull ServiceComponent other) {
        return serviceComponent.compareTo(other.serviceComponent);
    }
}
