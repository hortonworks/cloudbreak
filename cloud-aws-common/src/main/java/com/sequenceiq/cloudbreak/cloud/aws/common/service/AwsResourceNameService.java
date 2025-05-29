package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.CaseFormat;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;

@Service("AwsResourceNameServiceV2")
public class AwsResourceNameService extends CloudbreakResourceNameService {

    public static final String TG_PART_NAME = "TG";

    private static final int NAME_LAST_RANDOM_PART_LENGTH = 4;

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.max.aws.loadbalancer.resource.name.length:32}")
    private int maxLoadBalancerResourceNameLength;

    public String attachedDisk(String stackName, String groupName, Long privateId) {
        String name = instance(stackName, groupName, privateId);
        name = trimHash(name);
        name = appendPart(name, privateId);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String nativeInstance(String stackName, String groupName, Long stackId, Long privateId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustPartLength(name, maxResourceNameLength);
        return name;
    }

    public String rootDisk(String stackName, Long stackId, String groupName, Long privateId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String securityGroup(String stackName, String groupName, Long stackId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "SecurityGroup");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String eip(String stackName, String groupName, Long stackId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "EIPAllocationID");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String loadBalancer(String stackName, String scheme, CloudContext context) {
        String name;
        int numberOfAppends = 2;
        int stackNameLength = stackName.length();
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - scheme.length() - numberOfAppends;
        String reducedStackName = stackName.substring(0, Math.min(stackNameLength, maxLengthOfStackName));
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, scheme);
        String crnPart = getCrnPart(context);
        name = appendPart(name, crnPart);
        name = appendPart(name, RandomStringUtils.randomAlphanumeric(NAME_LAST_RANDOM_PART_LENGTH));
        name = adjustBaseLength(name, maxLoadBalancerResourceNameLength);
        return name;
    }

    private String getCrnPart(CloudContext context) {
        String crnResourcePart = Crn.safeFromString(context.getCrn()).getResource();
        return StringUtils.removeEnd(crnResourcePart.substring(0, getDefaultHashLength() - NAME_LAST_RANDOM_PART_LENGTH - 1), "-");
    }

    public String loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(String scheme, int port) {
        return TG_PART_NAME + port + scheme;
    }

    public String loadBalancerTargetGroup(String stackName, String scheme, int port, CloudContext context) {
        String name;
        String resourceNameWithScheme = loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(scheme, port);
        int numberOfAppends = 2;
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - resourceNameWithScheme.length() - numberOfAppends;
        String reducedStackName = stackName.substring(0, maxLengthOfStackName);
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, resourceNameWithScheme);
        name = appendPart(name, getCrnPart(context));
        name = appendPart(name, RandomStringUtils.randomAlphanumeric(NAME_LAST_RANDOM_PART_LENGTH));
        name = adjustBaseLength(name, maxLoadBalancerResourceNameLength);
        return name;
    }

    public String cloudWatch(String stackName, Long stackId, String groupName, Long privateId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, stackId);
        name = appendPart(name, "CloudWatch");
        name = appendPart(name, StringUtils.capitalize(normalize(instanceGroupName)));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    private String instance(String stackName, String groupName, Long privateId) {
        String name;
        String instanceGroupName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, String.valueOf(groupName).toLowerCase(Locale.ROOT));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }
}
