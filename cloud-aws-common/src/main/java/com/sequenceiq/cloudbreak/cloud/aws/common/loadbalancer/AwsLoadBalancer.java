package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AwsLoadBalancer {

    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final AwsLoadBalancerScheme scheme;

    private final String awsScheme;

    private final String name;

    private final List<AwsListener> listeners;

    private final Set<String> subnetIds;

    private String arn;

    private boolean listenerConfigSet;

    public AwsLoadBalancer(AwsLoadBalancerScheme scheme) {
        this.scheme = scheme;
        this.awsScheme = scheme.awsScheme();
        this.name = getLoadBalancerName(scheme);
        this.listeners = new ArrayList<>();
        this.subnetIds = new HashSet<>();
        this.listenerConfigSet = false;
    }

    public AwsLoadBalancerScheme getScheme() {
        return scheme;
    }

    public List<AwsListener> getListeners() {
        return listeners;
    }

    public AwsListener getOrCreateListener(int port, int healthCheckPort) {
        return listeners.stream()
            .filter(l -> l.getPort() == port)
            .findFirst().orElseGet(() -> createListener(port, healthCheckPort));
    }

    private AwsListener createListener(int port, int healthCheckPort) {
        AwsListener listener = new AwsListener(scheme, port, healthCheckPort);
        listeners.add(listener);
        return listener;
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

    public boolean isListenerConfigSet() {
        return listenerConfigSet;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void addSubnets(Set<String> newSubnetIds) {
        subnetIds.addAll(newSubnetIds);
    }

    public boolean validateListenerConfigIsSet() {
        listenerConfigSet = arn != null && !arn.isEmpty() &&
            listeners.stream().allMatch(AwsListener::areTargetGroupArnsSet);
        return listenerConfigSet;
    }

    public static String getLoadBalancerName(AwsLoadBalancerScheme scheme) {
        return LOAD_BALANCER_NAME_PREFIX + scheme.resourceName();
    }

    private static String sanitizeGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }
}
