package com.sequenceiq.cloudbreak.service.spot;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class SpotInstanceUsageCondition {

    public boolean isStackRunsOnSpotInstances(Stack stack) {
        return isSupportedCloudPlatform(stack) && isUsingSpotInstance(stack);
    }

    private boolean isSupportedCloudPlatform(Stack stack) {
        return stack.getCloudPlatform().equals(CloudPlatform.AWS.name());
    }

    private boolean isUsingSpotInstance(Stack stack) {
        return stack.getInstanceGroups()
                .stream()
                .map(InstanceGroup::getTemplate)
                .map(Template::getAttributes)
                .map(Json::getMap)
                .map(attributes -> attributes.getOrDefault(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 0))
                .map(spotPercentage -> Integer.parseInt(spotPercentage.toString()))
                .anyMatch(spotPercentage -> spotPercentage != 0);
    }
}
