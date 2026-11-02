package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import java.util.regex.Pattern;

import com.sequenceiq.common.api.type.LoadBalancerType;

/**
 * Names of the load balancer child resources Cloudbreak creates (see arm-v2.ftl). Anything else found on a Cloudbreak load balancer
 * was added outside Cloudbreak. The checks are purely name based, so they work without a CloudStack.
 */
public final class AzureLoadBalancerResourceNames {

    private static final String RULE_PREFIX = "port-";

    private static final String RULE_SUFFIX = "-rule";

    private static final String FRONTEND_SUFFIX = "-frontend";

    private static final String GATEWAY_SUFFIX = "-gateway";

    private static final String POOL_SUFFIX = "-pool";

    // The protocol part is empty in rules created before the traffic protocol was added to the name
    private static final String RULE_PROTOCOL_PATTERN = "[A-Z_]*";

    private AzureLoadBalancerResourceNames() {
    }

    public static String ruleName(int port, String protocol) {
        return RULE_PREFIX + port + protocol + RULE_SUFFIX;
    }

    public static boolean isCloudbreakRule(String ruleName, int backendPort) {
        return ruleName != null && Pattern.matches(
                Pattern.quote(RULE_PREFIX + backendPort) + RULE_PROTOCOL_PATTERN + Pattern.quote(RULE_SUFFIX) + "(" + Pattern.quote(GATEWAY_SUFFIX) + ")?",
                ruleName);
    }

    /**
     * The copy of a rule that arm-v2.ftl creates for the gateway frontend of a GATEWAY_PRIVATE load balancer.
     */
    public static boolean isGatewayRule(String ruleName) {
        return ruleName != null && ruleName.endsWith(GATEWAY_SUFFIX);
    }

    /**
     * The group behind a backend pool: {@code <group>-pool}, or {@code address} for the legacy {@code address-pool}. Only the suffix is removed.
     */
    public static String groupName(String poolName) {
        return poolName != null && poolName.endsWith(POOL_SUFFIX) ? poolName.substring(0, poolName.length() - POOL_SUFFIX.length()) : poolName;
    }

    public static String frontendName(String loadBalancerName, LoadBalancerType type) {
        return LoadBalancerType.GATEWAY_PRIVATE == type
                ? loadBalancerName + FRONTEND_SUFFIX + GATEWAY_SUFFIX
                : loadBalancerName + FRONTEND_SUFFIX;
    }
}
