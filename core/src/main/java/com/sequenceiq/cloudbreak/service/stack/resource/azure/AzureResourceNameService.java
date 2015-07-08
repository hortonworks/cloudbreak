package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.service.stack.resource.CloudbreakResourceNameService;

@Service("AzureResourceNameService")
public class AzureResourceNameService extends CloudbreakResourceNameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceNameService.class);

    private static final int CLOUDSERVICE_PART_COUNT = 3;
    private static final String RESERVED_IP_PREFIX = "reserved-ip";

    @Value("${cb.max.azure.resource.name.length:50}")
    private int maxResourceNameLength;

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        LOGGER.debug("Generating resource name from parts: {}", parts);
        String resourceName = null;
        switch (resourceType) {
            case AZURE_CLOUD_SERVICE:
                resourceName = cloudServiceResourceName(parts);
                break;
            case AZURE_SERVICE_CERTIFICATE:
                resourceName = azureServiceCertificateName(parts);
                break;
            case AZURE_VIRTUAL_MACHINE:
                resourceName = virtualMachineName(parts);
                break;
            case AZURE_NETWORK:
                resourceName = networkResourceName(parts);
                break;
            case AZURE_RESERVED_IP:
                resourceName = reservedIpTesourceName(parts);
                break;
            default:
                throw new IllegalStateException("Unsupported resource type:" + resourceType);
        }
        return resourceName;
    }

    private String reservedIpTesourceName(Object[] parts) {
        if (parts.length != 1) {
            LOGGER.error("No valid parts provided for building  reserved ip resource name. Parts: {}", parts);
            throw new IllegalStateException("No valid parts provided for building  reserved ip resource name: " + parts);
        }

        String stackId = String.valueOf(parts[0]);

        return appendPart(RESERVED_IP_PREFIX, stackId);
    }


    private String cloudServiceResourceName(Object... parts) {
        checkArgs(CLOUDSERVICE_PART_COUNT, parts);
        String networkName = String.valueOf(parts[0]);
        String index = String.valueOf(parts[1]);
        String instanceGroupName = String.valueOf(parts[2]);

        String resourceName = normalize(instanceGroupName);

        resourceName = appendPart(resourceName, index);
        resourceName = appendPart(resourceName, trimHash(networkName));
        resourceName = appendHash(resourceName, new Date());
        return resourceName;
    }

    private String virtualMachineName(Object[] parts) {
        checkArgs(1, parts);
        return String.valueOf(parts[0]);
    }

    private String networkResourceName(Object[] parts) {
        checkArgs(2, parts);
        String cloudServiceName = String.valueOf(parts[0]);
        Date ts = (Date) parts[1];

        String networkResourceName = normalize(cloudServiceName);
        networkResourceName = adjustPartLength(networkResourceName);
        networkResourceName = appendHash(networkResourceName, ts);

        return networkResourceName;
    }

    private String azureServiceCertificateName(Object[] parts) {
        checkArgs(1, parts);
        return String.valueOf(parts[0]);
    }

    @Override
    protected int getMaxResourceLength() {
        return maxResourceNameLength;
    }
}



