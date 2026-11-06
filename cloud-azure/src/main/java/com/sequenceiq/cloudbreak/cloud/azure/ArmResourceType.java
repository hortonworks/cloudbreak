package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Azure Resource Manager resource types (e.g. {@code Microsoft.Network/loadBalancers}) as they appear in ARM templates and deployment operations.
 */
public enum ArmResourceType {

    LOAD_BALANCER("Microsoft.Network/loadBalancers"),
    PUBLIC_IP_ADDRESS("Microsoft.Network/publicIPAddresses"),
    NETWORK_INTERFACE("Microsoft.Network/networkInterfaces"),
    NETWORK_SECURITY_GROUP("Microsoft.Network/networkSecurityGroups"),
    VIRTUAL_NETWORK("Microsoft.Network/virtualNetworks"),
    PRIVATE_ENDPOINT("Microsoft.Network/privateEndpoints"),
    PRIVATE_DNS_ZONE("Microsoft.Network/privateDnsZones"),
    VIRTUAL_NETWORK_LINK("Microsoft.Network/privateDnsZones/virtualNetworkLinks"),
    VIRTUAL_MACHINE("Microsoft.Compute/virtualMachines"),
    AVAILABILITY_SET("Microsoft.Compute/availabilitySets"),
    MANAGED_DISK("Microsoft.Compute/disks"),
    IMAGE("Microsoft.Compute/images"),
    STORAGE_ACCOUNT("Microsoft.Storage/storageAccounts"),
    POSTGRESQL_SERVER("Microsoft.DBforPostgreSQL/servers"),
    POSTGRESQL_FLEXIBLE_SERVER("Microsoft.DBforPostgreSQL/flexibleServers"),
    POSTGRESQL_SECURITY_ALERT_POLICY("Microsoft.DBforPostgreSQL/servers/securityAlertPolicies"),
    MANAGED_CLUSTER("Microsoft.ContainerService/managedClusters");

    private static final Map<String, ArmResourceType> BY_TYPE = Stream.of(values())
            .collect(Collectors.toMap(ArmResourceType::getType, Function.identity()));

    private final String type;

    ArmResourceType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean matches(String other) {
        return type.equals(other);
    }

    /**
     * Looks up the constant by its exact (case-sensitive) ARM resource type.
     */
    public static Optional<ArmResourceType> getByType(String type) {
        return type == null ? Optional.empty() : Optional.ofNullable(BY_TYPE.get(type));
    }
}

