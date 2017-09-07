package com.sequenceiq.cloudbreak.cloud.openstack.nativ.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service("OpenStackResourceNameService")
public class OpenStackResourceNameService extends CloudbreakResourceNameService {

    private static final int ATTACHED_DISKS_PART_COUNT = 4;

    private static final int INSTANCE_NAME_PART_COUNT = 3;

    @Value("${cb.max.openstack.resource.name.length:}")
    private int maxResourceNameLength;

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        String resourceName;

        switch (resourceType) {
            case OPENSTACK_NETWORK:
                resourceName = openStackNetworkResourceName(parts);
                break;
            case OPENSTACK_SUBNET:
                resourceName = openStackNetworkResourceName(parts);
                break;
            case OPENSTACK_ROUTER:
                resourceName = openStackNetworkResourceName(parts);
                break;
            case OPENSTACK_SECURITY_GROUP:
                resourceName = openStackNetworkResourceName(parts);
                break;
            case OPENSTACK_INSTANCE:
            case OPENSTACK_PORT:
            case OPENSTACK_FLOATING_IP:
                resourceName = instanceName(parts);
                break;
            case OPENSTACK_ATTACHED_DISK:
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
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }

    private String openStackNetworkResourceName(Object[] parts) {
        checkArgs(1, parts);
        String stackName = String.valueOf(parts[0]);
        String networkName = normalize(stackName);
        networkName = adjustPartLength(networkName);
        networkName = appendHash(networkName, new Date());
        networkName = adjustBaseLength(networkName, maxResourceNameLength);
        return networkName;
    }
}
