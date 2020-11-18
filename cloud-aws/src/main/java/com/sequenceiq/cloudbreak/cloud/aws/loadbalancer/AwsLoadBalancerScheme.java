package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

public enum AwsLoadBalancerScheme {
    PUBLIC("internet-facing"),
    PRIVATE("internal");

    private final String awsScheme;

    AwsLoadBalancerScheme(String awsScheme) {
        this.awsScheme = awsScheme;
    }

    public String awsScheme() {
        return awsScheme;
    }
}
