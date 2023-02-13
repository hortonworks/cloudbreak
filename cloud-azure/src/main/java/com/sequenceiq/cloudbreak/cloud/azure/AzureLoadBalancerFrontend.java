package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Objects;

import com.sequenceiq.common.api.type.LoadBalancerType;

public class AzureLoadBalancerFrontend {

    private final String name;

    private final String ip;

    private final LoadBalancerType loadBalancerType;

    public AzureLoadBalancerFrontend(String name, String ip, LoadBalancerType loadBalancerType) {
        this.name = name;
        this.ip = ip;
        this.loadBalancerType = loadBalancerType;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public LoadBalancerType getLoadBalancerType() {
        return loadBalancerType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AzureLoadBalancerFrontend)) {
            return false;
        }
        AzureLoadBalancerFrontend that = (AzureLoadBalancerFrontend) o;
        return Objects.equals(name, that.name) && Objects.equals(ip, that.ip) && Objects.equals(loadBalancerType, that.loadBalancerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ip, loadBalancerType);
    }

    @Override
    public String toString() {
        return "AzureLoadBalancerFrontend{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", loadBalancerType=" + loadBalancerType +
                '}';
    }
}
