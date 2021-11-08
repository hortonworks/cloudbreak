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

    public static final String TG_PART_NAME = "TG";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceNameService.class);

    private static final String FIREWALL_INTERNAL_NAME_SUFFIX = "internal";

    private static final String FIREWALL_IN_NAME_SUFFIX = "in";

    private static final int ATTACHED_DISKS_PART_COUNT = 3;

    private static final int ROOT_DISKS_PART_COUNT = 3;

    private static final int INSTANCE_NAME_PART_COUNT = 3;

    private static final int SECURITY_GROUP_PART_COUNT = 3;

    private static final int EIP_PART_COUNT = 3;

    private static final int LOAD_BALANCER_PART_COUNT = 2;

    private static final int LOAD_BALANCER_TARGET_GROUP_PART_COUNT = 3;

    private static final int CLOUD_WATCH_PART_COUNT = 4;

    private static final int PART_3 = 3;

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.max.aws.loadbalancer.resource.name.length:32}")
    private int maxLoadBalancerResourceNameLength;

    @Override
    public String resourceName(ResourceType resourceType, Object... parts) {
        String resourceName;

        if (resourceType == ResourceType.AWS_VOLUMESET) {
            resourceName = attachedDiskResourceName(parts);
        } else if (resourceType == ResourceType.AWS_INSTANCE) {
            resourceName = instanceName(parts);
        } else if (resourceType == ResourceType.AWS_SECURITY_GROUP) {
            resourceName = securityGroup(parts);
        } else if (resourceType == ResourceType.AWS_RESERVED_IP) {
            resourceName = eip(parts);
        } else if (resourceType == ResourceType.ELASTIC_LOAD_BALANCER) {
            resourceName = loadBalancer(parts);
        } else if (resourceType == ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP) {
            resourceName = loadBalancerTargetGroup(parts);
        } else if (resourceType == ResourceType.AWS_CLOUD_WATCH) {
            resourceName = cloudWatch(parts);
        } else if (resourceType == ResourceType.AWS_ROOT_DISK_TAGGING) {
            resourceName = rootDiskResourceName(parts);
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

    private String rootDiskResourceName(Object[] parts) {
        checkArgs(ROOT_DISKS_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String stackId = String.valueOf(parts[1]);
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(parts[2]).toLowerCase());
        String privateId = String.valueOf(parts[PART_3]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
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
        name = appendPart(name, "SecurityGroup");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }

    private String eip(Object[] parts) {
        checkArgs(EIP_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(parts[1]).toLowerCase());
        String stackId = String.valueOf(parts[2]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "EIPAllocationID");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }

    private String loadBalancer(Object[] parts) {
        checkArgs(LOAD_BALANCER_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String scheme = String.valueOf(parts[1]);
        String resourceNameWithScheme = "LB" + scheme;
        int numberOfAppends = 2;
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - resourceNameWithScheme.length() - numberOfAppends;
        String reducedStackName = stackName.substring(0, maxLengthOfStackName);
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, resourceNameWithScheme);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxLoadBalancerResourceNameLength);

        return name;
    }

    private String loadBalancerTargetGroup(Object[] parts) {
        checkArgs(LOAD_BALANCER_TARGET_GROUP_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String scheme = String.valueOf(parts[1]);
        String port = String.valueOf(parts[2]);
        String resourceNameWithScheme = TG_PART_NAME + port + scheme;
        int numberOfAppends = 2;
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - resourceNameWithScheme.length() - numberOfAppends;
        String reducedStackName = stackName.substring(0, maxLengthOfStackName);
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, resourceNameWithScheme);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxLoadBalancerResourceNameLength);

        return name;
    }

    private String cloudWatch(Object[] parts) {
        checkArgs(CLOUD_WATCH_PART_COUNT, parts);
        String name;
        String stackName = String.valueOf(parts[0]);
        String stackId = String.valueOf(parts[1]);
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(parts[2]).toLowerCase());
        String privateId = String.valueOf(parts[PART_3]);

        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "CloudWatch");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);

        return name;
    }
}
