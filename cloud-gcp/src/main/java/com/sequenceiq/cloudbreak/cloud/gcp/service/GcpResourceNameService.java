package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.service.naming.GcpInstanceGroupResourceName;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service("GcpResourceNameServiceV2")
public class GcpResourceNameService extends CloudbreakResourceNameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceNameService.class);

    private static final String FIREWALL_INTERNAL_NAME_SUFFIX = "internal";

    private static final String FIREWALL_IN_NAME_SUFFIX = "in";

    private static final int INSTANCE_GROUP_NO_STACK_PART_COUNT = 2;

    private static final int INSTANCE_GROUP_WITH_STACK_PART_COUNT = 3;

    @Value("${cb.max.gcp.resource.name.length:}")
    private int maxResourceNameLength;

    public GcpInstanceGroupResourceName decodeInstanceGroupResourceNameFromString(String cloudResourceName) {
        String[] parts = StringUtils.split(cloudResourceName, '-');
        if (parts.length == INSTANCE_GROUP_NO_STACK_PART_COUNT) {
            LOGGER.warn("old instance group naming pattern discovered {}", cloudResourceName);
            return new GcpInstanceGroupResourceName(parts[0], parts[1], "");
        }
        if (parts.length == INSTANCE_GROUP_WITH_STACK_PART_COUNT) {
            return new GcpInstanceGroupResourceName(parts[0], parts[1], parts[2]);
        }
        LOGGER.error("instance group name '{}' is not normalized, must follow the structure CONTEXT-GROUP-NUMBER", cloudResourceName);
        throw new IllegalArgumentException("instance group name '{}' is not normalized, must follow the structure CONTEXT-GROUP-NUMBER");
    }

    public String attachedDisk(String stackName, String groupName, Long privateId, int count) {
        String name = instance(stackName, groupName, String.valueOf(privateId));
        name = trimHash(name);
        name = appendPart(name, count);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String deploymentTemplate(String stackName, Long stackId) {
        String deploymentName;
        deploymentName = normalize(stackName);
        deploymentName = adjustPartLength(deploymentName);
        deploymentName = appendHash(deploymentName, stackId);
        deploymentName = adjustBaseLength(deploymentName, maxResourceNameLength);
        return deploymentName;
    }

    public String instance(String stackName, String groupName, Long privateId) {
        return instance(stackName, groupName, String.valueOf(privateId));
    }

    public String instance(String stackName, String groupName, String privateId) {
        String name;
        String instanceGroupName = WordUtils.initials(String.valueOf(groupName).replaceAll("_", " "));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String firewallIn(String stackName) {
        return stackBasedResourceWithSuffix(FIREWALL_IN_NAME_SUFFIX, stackName);
    }

    public String firewallInternal(String stackName) {
        return stackBasedResourceWithSuffix(FIREWALL_INTERNAL_NAME_SUFFIX, stackName);
    }

    private String stackBasedResourceWithSuffix(String suffix, String stackName) {
        LOGGER.debug("Generating stack based resource name with suffix. Stack {}; suffix {}", stackName, suffix);
        String name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, suffix);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String network(String stackName) {
        String networkName;
        networkName = normalize(stackName);
        networkName = adjustPartLength(networkName);
        networkName = appendDateAsHash(networkName, new Date());
        networkName = adjustBaseLength(networkName, maxResourceNameLength);
        return networkName;
    }

    public String subnet(String stackName) {
        String subnetName;
        subnetName = normalize(stackName);
        subnetName = adjustPartLength(subnetName);
        subnetName = appendDateAsHash(subnetName, new Date());
        subnetName = adjustBaseLength(subnetName, maxResourceNameLength);
        return subnetName;
    }

    public String group(String stackName, String groupName, Long stackId, String zone) {
        String name;
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(groupName));
        name = appendPart(name, normalize(stackId));
        if (!StringUtils.isEmpty(zone)) {
            name = appendPart(name, zone.substring(zone.lastIndexOf('-') + 1));
        }
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String loadBalancerWithPort(String stackName, LoadBalancerType loadBalancerType, Integer port) {
        String name;
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(loadBalancerType.name()));
        name = appendPart(name, port);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String loadBalancerWithProtocolAndPort(String stackName, LoadBalancerType loadBalancerType, String protocol, Integer port) {
        String name;
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(loadBalancerType.name()));
        name = appendPart(name, normalize(protocol));
        name = appendPart(name, port);
        name = appendDateAsHash(name, new Date());
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }
}
