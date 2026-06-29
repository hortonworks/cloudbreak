package com.sequenceiq.it.cloudbreak.util.azure;

public enum AzureResources {

    AZURE_RESOURCE_GROUP("Microsoft.Resources/resourceGroups"),

    AZURE_INSTANCE("Microsoft.Compute/virtualMachines"),
    AZURE_DISK("Microsoft.Compute/disks"),
    AZURE_VOLUMESET("Microsoft.Compute/disks"),
    AZURE_AVAILABILITY_SET("Microsoft.Compute/availabilitySets"),

    AZURE_VIRTUAL_NETWORK("Microsoft.Network/virtualNetworks"),
    AZURE_SECURITY_GROUP("Microsoft.Network/networkSecurityGroups"),
    AZURE_NETWORK_INTERFACE("Microsoft.Network/networkInterfaces"),
    AZURE_PUBLIC_IP("Microsoft.Network/publicIPAddresses"),
    AZURE_LOAD_BALANCER("Microsoft.Network/loadBalancers"),

    AZURE_POSTGRESQL_SERVER("Microsoft.DBforPostgreSQL/servers"),
    AZURE_POSTGRESQL_FLEXIBLE_SERVER("Microsoft.DBforPostgreSQL/flexibleServers"),
    AZURE_DATABASE("Microsoft.DBforPostgreSQL/servers/databases");

    private final String resourceType;

    AzureResources(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }
}