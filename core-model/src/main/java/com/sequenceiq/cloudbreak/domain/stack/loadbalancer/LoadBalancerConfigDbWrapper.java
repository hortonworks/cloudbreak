package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;

/**
 * A wrapper for the cloud provider specific load balancer metadata stored in the database. Only one
 * type of config should be set, depending on what the cloud platform for the stack is. This class
 * is intended to be serialized into a JSON string and stored in the database, and deserialized
 * when fetched.
 */
public class LoadBalancerConfigDbWrapper {

    private AwsLoadBalancerConfigDb awsConfig;

    public AwsLoadBalancerConfigDb getAwsConfig() {
        return awsConfig;
    }

    public void setAwsConfig(AwsLoadBalancerConfigDb awsConfig) {
        this.awsConfig = awsConfig;
    }

    @Override
    public String toString() {
        return "CloudLoadBalancerConfig{" +
            "awsConfig=" + awsConfig +
            '}';
    }
}
