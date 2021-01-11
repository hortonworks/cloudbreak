package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

public enum AwsLoadBalancerScheme {
    INTERNET_FACING("internet-facing", "External"),
    INTERNAL("internal", "Internal");

    private final String awsScheme;

    private final String resourceName;

    AwsLoadBalancerScheme(String awsScheme, String resourceName) {
        this.awsScheme = awsScheme;
        this.resourceName = resourceName;
    }

    public String awsScheme() {
        return awsScheme;
    }

    public String resourceName() {
        return resourceName;
    }
}
