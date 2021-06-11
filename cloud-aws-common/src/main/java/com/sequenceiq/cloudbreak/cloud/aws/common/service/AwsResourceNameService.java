package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.CaseFormat;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.common.api.type.ResourceType;

@Service("AwsResourceNameServiceV2")
public class AwsResourceNameService extends CloudbreakResourceNameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceNameService.class);

    private static final String FIREWALL_INTERNAL_NAME_SUFFIX = "internal";

    private static final String FIREWALL_IN_NAME_SUFFIX = "in";

    private static final int ATTACHED_DISKS_PART_COUNT = 3;

    private static final int INSTANCE_NAME_PART_COUNT = 3;

    private static final int SECURITY_GROUP_PART_COUNT = 3;

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        String resourceName;

        if (resourceType == ResourceType.AWS_VOLUMESET) {
            resourceName = attachedDiskResourceName(parts);
        } else if (resourceType == ResourceType.AWS_INSTANCE) {
            resourceName = instanceName(parts);
        } else if (resourceType == ResourceType.AWS_SECURITY_GROUP) {
            resourceName = securityGroup(parts);
        } else {
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
        String name;
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(parts[1]).toLowerCase());
        String privateId = String.valueOf(parts[2]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }

    private String securityGroup(Object[] parts) {
        checkArgs(SECURITY_GROUP_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(parts[1]).toLowerCase());
        String stackId = String.valueOf(parts[2]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "ClusterNodeSecurityGroup");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }
}
