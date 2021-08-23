package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure;

/**
 * The top level AWS specific load balancer metadata database object. For Azure, the Azure
 * load balancer name is recorded.
 */
public class AzureLoadBalancerConfigDb {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancerConfigDb{" +
                "name='" + name + '\'' +
                '}';
    }
}
