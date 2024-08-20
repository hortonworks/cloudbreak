package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;

public class AwsLoadBalancer {

    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final AwsLoadBalancerScheme scheme;

    private final String name;

    private final List<AwsListener> listeners;

    private final Set<String> subnetIds;

    private String arn;

    private boolean listenerConfigSet;

    private boolean useStickySessionForTargetGroup;

    public AwsLoadBalancer(AwsLoadBalancerScheme scheme) {
        this.scheme = scheme;
        this.name = getLoadBalancerName(scheme);
        this.listeners = new ArrayList<>();
        this.subnetIds = new HashSet<>();
        this.listenerConfigSet = false;
        this.useStickySessionForTargetGroup = false;
    }

    public AwsLoadBalancerScheme getScheme() {
        return scheme;
    }

    public List<AwsListener> getListeners() {
        return listeners;
    }

    public AwsListener getOrCreateListener(int port, ProtocolEnum protocol, String healthCheckPath, int healthCheckPort, ProtocolEnum healthCheckProtocol) {
        return listeners.stream()
            .filter(l -> l.getPort() == port)
            .findFirst().orElseGet(() -> createListener(port, protocol, healthCheckPath, healthCheckPort, healthCheckProtocol));
    }

    private AwsListener createListener(int port, ProtocolEnum protocol, String healthCheckPath, int healthCheckPort, ProtocolEnum healthCheckProtocol) {
        AwsListener listener = new AwsListener(scheme, port, protocol, healthCheckPath, healthCheckPort, healthCheckProtocol, useStickySessionForTargetGroup);
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

    /**
     * AWS Scheme getter, needed for the aws-cf-stack.ftl FreeMarker template, used in model.
     * @return AWS scheme ("internet-facing" or "internal")
     */
    public String getAwsScheme() {
        return scheme.awsScheme();
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

    public boolean isUseStickySessionForTargetGroup() {
        return useStickySessionForTargetGroup;
    }

    public void setUseStickySessionForTargetGroup(boolean useStickySessionForTargetGroup) {
        this.useStickySessionForTargetGroup = useStickySessionForTargetGroup;
    }

}
