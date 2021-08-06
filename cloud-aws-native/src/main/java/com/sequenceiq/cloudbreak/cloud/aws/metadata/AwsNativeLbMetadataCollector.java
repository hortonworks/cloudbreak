package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeLbMetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLbMetadataCollector.class);

    private static final Pattern TG_NAME_PATTERN = Pattern.compile("(" + AwsResourceNameService.TG_PART_NAME + "\\d+)");

    public Map<String, Object> getParameters(String loadBalancerArn, List<CloudResource> resources) {
        LOGGER.info("Gathering AWS load balancer ARN information");
        Map<String, Object> parameters = parseTargetGroupCloudParams(loadBalancerArn, resources);
        parameters.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, loadBalancerArn);
        return parameters;
    }

    private Map<String, Object> parseTargetGroupCloudParams(String loadBalancerArn, List<CloudResource> resources) {
        Set<CloudResource> targetGroups = resources.stream()
            .filter(resource -> loadBalancerArn.equals(resource.getInstanceId()))
            .filter(resource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(resource.getType()))
            .collect(Collectors.toSet());

        Map<String, Object> targetGroupParameters = new HashMap<>();
        targetGroups.forEach(targetGroup -> {
            Optional<Integer> port = getPortFromTargetGroupName(targetGroup.getName());
            if (port.isPresent()) {
                String targetGroupArn = targetGroup.getReference();
                String listenerArn = resources.stream()
                    .filter(resource -> targetGroup.getName().equals(resource.getName()))
                    .filter(resource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(resource.getType()))
                    .map(CloudResource::getReference)
                    .findFirst().orElse(null);

                LOGGER.debug("Found target group ARN {} and listern ARN {} associated with load balancer {}, port {}",
                    targetGroupArn, listenerArn, loadBalancerArn, port);
                targetGroupParameters.put(AwsLoadBalancerMetadataView.getTargetGroupParam(port.get()), targetGroupArn);
                targetGroupParameters.put(AwsLoadBalancerMetadataView.getListenerParam(port.get()), listenerArn);
            } else {
                LOGGER.warn("Unable to parse target group and listener port from target group name [{}]. Setup will continue, " +
                    "but load balancer metadata will be missing target group and listener information.", targetGroup.getName());
            }
        });
        return targetGroupParameters;
    }

    private Optional<Integer> getPortFromTargetGroupName(String targetGroupName) {
        Matcher matcher = TG_NAME_PATTERN.matcher(targetGroupName);
        if (matcher.find()) {
            String matchedGroup = matcher.group();
            Integer port = Integer.valueOf(matchedGroup.replace(AwsResourceNameService.TG_PART_NAME, ""));
            return Optional.of(port);
        }
        return Optional.empty();
    }
}
