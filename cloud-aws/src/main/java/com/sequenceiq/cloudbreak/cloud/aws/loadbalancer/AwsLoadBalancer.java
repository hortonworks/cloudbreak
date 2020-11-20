package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AwsLoadBalancer {

    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final AwsLoadBalancerScheme scheme;

    private final String awsScheme;

    private final List<AwsListener> listeners;

    private final String name;

    private String arn;

    private boolean createListeners;

    public AwsLoadBalancer(AwsLoadBalancerScheme scheme, List<AwsListener> listeners) {
        this.scheme = scheme;
        this.awsScheme = scheme.awsScheme();
        this.listeners = listeners;
        this.name = getLoadBalancerName(scheme);
        this.createListeners = false;
    }

    public AwsLoadBalancerScheme getScheme() {
        return scheme;
    }

    public List<AwsListener> getListeners() {
        return listeners;
    }

    public String getName() {
        return name;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getAwsScheme() {
        return awsScheme;
    }

    public boolean isCreateListeners() {
        return createListeners;
    }

    public boolean canCreateListeners() {
        createListeners = arn != null && !arn.isEmpty() &&
            listeners.stream().allMatch(AwsListener::areTargetGroupArnsSet);
        return createListeners;
    }

    public static String getLoadBalancerName(AwsLoadBalancerScheme scheme) {
        return LOAD_BALANCER_NAME_PREFIX +
            StringUtils.capitalize(scheme.name().toLowerCase());
    }

    private static String sanitizeGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }
}
