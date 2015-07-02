package com.sequenceiq.cloudbreak.service.stack.resource.gcp;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.service.stack.resource.CloudbreakResourceNameService;

@Service("GcpResourceNameService")
public class GcpResourceNameService extends CloudbreakResourceNameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceNameService.class);

    private static final String FIREWALL_INTERNAL_NAME_SUFFIX = "internal";
    private static final String FIREWALL_IN_NAME_SUFFIX = "in";
    private static final String RESERVED_IP_SUFFIX = "reserved-ip";
    private static final int ATTACHED_DISKS_PART_COUNT = 4;
    private static final int INSTANCE_NAME_PART_COUNT = 3;

    @Value("${cb.max.gcp.resource.name.length:50}")
    private int maxResourceNameLength;

    @Override
    protected int getMaxResourceLength() {
        return maxResourceNameLength;
    }

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        String resourceName;

        switch (resourceType) {
            case GCP_NETWORK:
                resourceName = gcpNetworkResourceName(parts);
                break;
            case GCP_FIREWALL_INTERNAL:
                resourceName = stackBasedResourceWithSuffix(FIREWALL_INTERNAL_NAME_SUFFIX, parts);
                break;
            case GCP_FIREWALL_IN:
                resourceName = stackBasedResourceWithSuffix(FIREWALL_IN_NAME_SUFFIX, parts);
                break;
            case GCP_RESERVED_IP:
                resourceName = stackBasedResourceWithSuffix(RESERVED_IP_SUFFIX, parts);
                break;
            case GCP_INSTANCE:
                resourceName = instanceName(parts);
                break;
            case GCP_DISK:
                resourceName = instanceName(parts);
                break;
            case GCP_ATTACHED_DISK:
                resourceName = attachedDiskResourceName(parts);
                break;
            default:
                throw new IllegalStateException("Unsupported resource type: " + resourceType);
        }
        return resourceName;
    }

    private String attachedDiskResourceName(Object[] parts) {
        checkArgs(ATTACHED_DISKS_PART_COUNT, parts);
        String cnt = String.valueOf(parts[ATTACHED_DISKS_PART_COUNT - 1]);
        String name = instanceName(parts);
        name = trimHash(name);
        name = appendPart(name, cnt);
        name = appendHash(name, new Date());
        return name;
    }

    private String instanceName(Object[] parts) {
        checkArgs(INSTANCE_NAME_PART_COUNT, parts);
        String name = null;
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = String.valueOf(parts[1]);
        String index = String.valueOf(parts[2]);

        name = normalize(instanceGroupName);
        name = adjustPartLength(name);
        name = appendPart(name, index);
        name = appendPart(name, stackName);
        name = appendHash(name, new Date());

        return name;
    }

    private String stackBasedResourceWithSuffix(String suffix, Object[] parts) {
        checkArgs(1, parts);
        String stackName = String.valueOf(parts[0]);
        LOGGER.debug("Generating stack based resource name with suffix. Stack {}; suffix {}", parts, suffix);
        String name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, suffix);
        name = appendHash(name, new Date());
        return name;
    }

    private String gcpNetworkResourceName(Object[] parts) {
        checkArgs(1, parts);
        String networkName = null;
        String stackName = String.valueOf(parts[0]);
        networkName = normalize(stackName);
        networkName = adjustPartLength(networkName);
        networkName = appendHash(networkName, new Date());
        return networkName;
    }
}
