package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.junit.Test;

import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.privatednsManager;

public class DnsZoneNetworkLink {
    private final AzureTestCredentials azureTestCredentials = new AzureTestCredentials();

    private final privatednsManager privateDnsManager = privatednsManager.authenticate(azureTestCredentials.getCredentials(), azureTestCredentials.getSubscriptionId());

    @Test
    public void testLink() {
        String resourceGroupName = "rg-gpapp-single-rg";
        String dnsZoneName = "privatelink.postgres.database.azure.com";
        String virtualNetworkLinkName = "nw-gpapp-daily";
        VirtualNetworkLinkInner inner = privateDnsManager.virtualNetworkLinks().inner().get(resourceGroupName, dnsZoneName, virtualNetworkLinkName);
        System.out.println(inner.virtualNetwork().id());
        privateDnsManager.virtualNetworkLinks().inner().get(resourceGroupName, dnsZoneName, virtualNetworkLinkName).virtualNetwork().id();
    }
}
