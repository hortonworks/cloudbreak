package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import com.sequenceiq.common.api.type.LoadBalancerType;

public enum AwsLoadBalancerScheme {

    /**
     * It is used as an internet facing Load Balancer that has public IP address and can route traffic to private networks.
     * Placed in a public subnet.
     */
    INTERNET_FACING("internet-facing", "External", LoadBalancerType.PUBLIC),

    /**
     * It is used as an internal Load Balancer by users who have access to the private network where this Load Balancer resides.
     * It has private IP address and can route traffic to private networks.
     * Placed in a private subnet.
     */
    GATEWAY_PRIVATE("internal", "GwayPriv", LoadBalancerType.GATEWAY_PRIVATE),

    /**
     * It is used as an internal Load Balancer for CDP Runtime services. It has private IP address and can route traffic to private networks.
     * Placed in a private subnet.
     */
    INTERNAL("internal", "Internal", LoadBalancerType.PRIVATE);

    private final String awsScheme;

    private final String resourceName;

    private final LoadBalancerType loadBalancerType;

    AwsLoadBalancerScheme(String awsScheme, String resourceName, LoadBalancerType loadBalancerType) {
        this.awsScheme = awsScheme;
        this.resourceName = resourceName;
        this.loadBalancerType = loadBalancerType;
    }

    public String awsScheme() {
        return awsScheme;
    }

    public String resourceName() {
        return resourceName;
    }

    public LoadBalancerType getLoadBalancerType() {
        return loadBalancerType;
    }
}
