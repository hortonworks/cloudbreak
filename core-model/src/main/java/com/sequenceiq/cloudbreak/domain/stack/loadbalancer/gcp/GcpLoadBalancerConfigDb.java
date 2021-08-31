package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp;

public class GcpLoadBalancerConfigDb {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GcpLoadBalancerConfigDb{" +
                "name='" + name + '\'' +
                '}';
    }
}
