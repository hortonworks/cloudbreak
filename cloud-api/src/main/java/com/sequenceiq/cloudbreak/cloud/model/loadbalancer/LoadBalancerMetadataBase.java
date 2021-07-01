package com.sequenceiq.cloudbreak.cloud.model.loadbalancer;

/**
 * Abstract class used to store cloud provider specific load balancer metadata. This class exists as a base
 * for more specific extended cloud provider classes, and has methods to convert itself to the cloud provider
 * specific clases.
 */
public abstract class LoadBalancerMetadataBase {

    public AwsLoadBalancerMetadata getAwsMetadata() {
        return (AwsLoadBalancerMetadata) this;
    }
}
