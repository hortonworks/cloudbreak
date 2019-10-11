package com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy;

import java.util.List;
import java.util.Map;

public abstract class AzureSubnetStrategy {

    public enum SubnetStratgyType {
        FILL {
            @Override
            AzureSubnetStrategy create(List<String> subnets, Map<String, Long> availableIPs) {
                return new FillSubnetStrategy(subnets, availableIPs);
            }
        };

        abstract AzureSubnetStrategy create(List<String> subnets, Map<String, Long> availableIPs);
    }

    private final List<String> subnets;

    private final Map<String, Long> availableIPs;

    AzureSubnetStrategy(List<String> subnets, Map<String, Long> availableIPs) {
        this.subnets = subnets;
        this.availableIPs = availableIPs;
    }

    public static AzureSubnetStrategy getAzureSubnetStrategy(SubnetStratgyType type, List<String> subnets, Map<String, Long> availableIPs) {
        return type.create(subnets, availableIPs);
    }

    public abstract String getNextSubnetId();

    List<String> getSubnets() {
        return subnets;
    }

    Map<String, Long> getAvailableIPs() {
        return availableIPs;
    }
}
