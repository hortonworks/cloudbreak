package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerInboundNatRule;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Component
class AzureVmPublicIpProvider {

    String getPublicIp(AzureClient azureClient, AzureUtils azureUtils, NetworkInterface networkInterface, String resourceGroup) {
        PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();

        List<LoadBalancerBackend> backends = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerBackends();
        List<LoadBalancerInboundNatRule> inboundNatRules = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerInboundNatRules();
        String publicIp = null;
        if (!backends.isEmpty() || !inboundNatRules.isEmpty()) {
            publicIp = azureClient.getLoadBalancerIps(resourceGroup, azureUtils.getLoadBalancerId(resourceGroup)).get(0);
        }

        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            publicIp = publicIpAddress.ipAddress();
        }

        return publicIp;
    }
}
