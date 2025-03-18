package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import io.micrometer.common.util.StringUtils;

public class AzureLoadBalancerProbe {
    private final int port;

    private final String name;

    private final String path;

    private final String protocol;

    private final int interval;

    private final int threshold;

    public AzureLoadBalancerProbe(int port, String name, String healthCheckPath, String healthCheckProtocol, int interval, int threshold) {
        this.port = port;
        this.name = StringUtils.isBlank(name) ? "port-" + port + "-probe" : name;
        this.path = healthCheckPath;
        this.protocol = healthCheckProtocol;
        this.interval = interval;
        this.threshold = threshold;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getInterval() {
        return interval;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancerProbe{" +
                "port=" + port +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", protocol='" + protocol + '\'' +
                ", interval='" + interval + '\'' +
                ", threshold='" + threshold + '\'' +
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
                .append(name, rhs.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 35)
                .append(name)
                .append(port)
                .append(path)
                .append(protocol)
                .append(interval)
                .append(threshold)
                .toHashCode();
    }
}
