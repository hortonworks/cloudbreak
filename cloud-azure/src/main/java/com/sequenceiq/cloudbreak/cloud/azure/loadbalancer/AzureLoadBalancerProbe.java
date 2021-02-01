package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

public class AzureLoadBalancerProbe {
    private final int port;

    private final String name;

    public AzureLoadBalancerProbe(int port) {
        this(port, "port-" + Integer.toString(port) + "-probe");
    }

    public AzureLoadBalancerProbe(int port, String name) {
        Objects.requireNonNull(name);

        this.port = port;
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancerProbe{" +
                "port=" + port +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        AzureLoadBalancerProbe rhs = (AzureLoadBalancerProbe) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(port, rhs.port)
                .append(name, rhs.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 35)
                .append(name)
                .append(port)
                .toHashCode();
    }
}
