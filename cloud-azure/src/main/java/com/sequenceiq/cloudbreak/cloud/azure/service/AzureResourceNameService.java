package com.sequenceiq.cloudbreak.cloud.azure.service;

import java.util.Date;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.common.api.type.ResourceType;

@Service("AzureResourceNameService")
public class AzureResourceNameService extends CloudbreakResourceNameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceNameService.class);

    private static final int VOLUMESET_PART_COUNT = 3;

    private static final int ATTACHED_DISKS_PART_COUNT = 4;

    private static final int INSTANCE_NAME_PART_COUNT = 3;

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        String resourceName;

        switch (resourceType) {
            case AZURE_VOLUMESET:
                resourceName = volumeSetResourceName(parts);
                break;
            case AZURE_DISK:
                resourceName = attachedDiskResourceName(parts);
                break;
            default:
                throw new IllegalStateException("Unsupported resource type: " + resourceType);
        }
        return resourceName;
    }

    private String volumeSetResourceName(Object[] parts) {
        checkArgs(VOLUMESET_PART_COUNT, parts);
        String name = instanceName(parts);
        name = trimHash(name);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    private String attachedDiskResourceName(Object[] parts) {
        checkArgs(ATTACHED_DISKS_PART_COUNT, parts);
        String cnt = String.valueOf(parts[ATTACHED_DISKS_PART_COUNT - 1]);
        String name = instanceName(parts);
        name = trimHash(name);
        name = appendPart(name, cnt);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    private String instanceName(Object[] parts) {
        checkArgs(INSTANCE_NAME_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = WordUtils.initials(String.valueOf(parts[1]).replaceAll("_", " "));
        String privateId = String.valueOf(parts[2]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }
}
