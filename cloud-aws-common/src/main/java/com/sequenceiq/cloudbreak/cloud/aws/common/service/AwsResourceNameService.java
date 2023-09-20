package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.CaseFormat;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;

@Service("AwsResourceNameServiceV2")
public class AwsResourceNameService extends CloudbreakResourceNameService {

    public static final String TG_PART_NAME = "TG";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceNameService.class);

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.max.aws.loadbalancer.resource.name.length:32}")
    private int maxLoadBalancerResourceNameLength;

    public String attachedDisk(String stackName, String groupName, Long privateId) {
        String name = instance(stackName, groupName, privateId);
        name = trimHash(name);
        name = appendPart(name, privateId);
        name = appendHash(name, new Date());
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

    public String loadBalancer(String stackName, String scheme) {
        String name;
        String resourceNameWithScheme = "LB" + scheme;
        int numberOfAppends = 2;
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - resourceNameWithScheme.length() - numberOfAppends;
        String reducedStackName = String.valueOf(stackName).substring(0, maxLengthOfStackName);
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, resourceNameWithScheme);
        name = appendHash(name, new Date());
        name = adjustBaseLength(name, maxLoadBalancerResourceNameLength);
        return name;
    }

    public String loadBalancerTargetGroup(String stackName, String scheme, int port) {
        String name;
        String resourceNameWithScheme = TG_PART_NAME + port + scheme;
        int numberOfAppends = 2;
        int maxLengthOfStackName = maxLoadBalancerResourceNameLength - getDefaultHashLength() - resourceNameWithScheme.length() - numberOfAppends;
        String reducedStackName = String.valueOf(stackName).substring(0, maxLengthOfStackName);
        name = normalize(reducedStackName);
        name = adjustPartLength(name);
        name = appendPart(name, resourceNameWithScheme);
        name = appendHash(name, new Date());
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
