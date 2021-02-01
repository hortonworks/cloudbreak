package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;

public final class AzureLoadBalancingRule {
    private final String name;

    private final int backendPort;

    private final int frontendPort;

    private final AzureLoadBalancerProbe probe;

    public AzureLoadBalancingRule(TargetGroupPortPair portPair) {
        this.backendPort = portPair.getTrafficPort();
        this.frontendPort = portPair.getTrafficPort();
        this.name = defaultNameFromPort(portPair.getTrafficPort());
        this.probe = new AzureLoadBalancerProbe(portPair.getHealthCheckPort());
    }

    private String defaultNameFromPort(int port) {
        return "port-" + port + "-rule";
    }

    public String getName() {
        return name;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public int getFrontendPort() {
        return frontendPort;
    }

    public AzureLoadBalancerProbe getProbe() {
        return probe;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AzureLoadBalancingRule {\n");
        sb.append("    name: ").append(name).append("\n");
        sb.append("    backendPort: ").append(backendPort).append("\n");
        sb.append("    frontendPort: ").append(frontendPort).append("\n");
        sb.append("    probe: ").append(toIndentedString(probe)).append("\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
