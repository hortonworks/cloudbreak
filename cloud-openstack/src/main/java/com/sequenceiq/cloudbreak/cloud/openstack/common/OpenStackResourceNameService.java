package com.sequenceiq.cloudbreak.cloud.openstack.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class OpenStackResourceNameService extends CloudbreakResourceNameService {

    private static final int INSTANCE_NAME_PART_COUNT = 3;

    @Value("${cb.max.openstack.resource.name.length:120}")
    private int maxResourceNameLength;

    public String resourceName(ResourceType resourceType, Object... parts) {
        return switch (resourceType) {
            case OPENSTACK_NETWORK -> openStackNetworkResourceName(parts);
            case OPENSTACK_SUBNET -> openStackNetworkResourceName(parts);
            case OPENSTACK_SECURITY_GROUP -> openStackSecurityGroupResourceName(parts);
            case OPENSTACK_INSTANCE, OPENSTACK_PORT, OPENSTACK_FLOATING_IP -> instanceName(parts);
            case OPENSTACK_ATTACHED_DISK -> attachedDiskResourceName(parts);
            default -> throw new IllegalStateException("Unsupported resource type: " + resourceType);
        };
    }

    private String attachedDiskResourceName(Object[] parts) {
        String name = instanceName(parts);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    private String instanceName(Object[] parts) {
        checkArgs(INSTANCE_NAME_PART_COUNT, parts);
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = String.valueOf(parts[1]);
        String privateId = String.valueOf(parts[2]);

        String name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }

    private String openStackNetworkResourceName(Object[] parts) {
        checkArgs(1, parts);
        String stackName = String.valueOf(parts[0]);
        String networkName = normalize(stackName);
        networkName = adjustPartLength(networkName);
        networkName = adjustBaseLength(networkName, maxResourceNameLength);
        return networkName;
    }

    private String openStackSecurityGroupResourceName(Object[] parts) {
        checkArgs(2, parts);
        String stackName = String.valueOf(parts[0]);
        String groupName = String.valueOf(parts[1]);
        String networkName = normalize(stackName);
        networkName = adjustPartLength(networkName);
        networkName = appendPart(networkName, normalize(groupName));
        networkName = adjustBaseLength(networkName, maxResourceNameLength);
        return networkName;
    }

    private void checkArgs(int argCnt, Object... parts) {
        if (null == parts || parts.length < argCnt) {
            throw new IllegalStateException("No suitable name parts provided to generate resource name!");
        }
    }
}
