package com.sequenceiq.cloudbreak.cloud.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;

@Component
class AzureVmPublicIpProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVmPublicIpProvider.class);

    String getPublicIp(NetworkInterface networkInterface) {
        PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();

        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            LOGGER.info("Azure network interface {} has public IP address {}.", networkInterface.id(), publicIpAddress.ipAddress());
            return publicIpAddress.ipAddress();
        } else {
            LOGGER.info("Azure network interface {} does not have a public IP.", networkInterface.id());
            return null;
        }
    }
}
