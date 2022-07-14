package com.sequenceiq.cloudbreak.service.spot;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class SpotInstanceUsageCondition {

    public boolean isStackRunsOnSpotInstances(StackDtoDelegate stack) {
        return isSupportedCloudPlatform(stack) && isUsingSpotInstance(stack);
    }

    private boolean isSupportedCloudPlatform(StackDtoDelegate stack) {
        return stack.getCloudPlatform().equals(CloudPlatform.AWS.name());
    }

    private boolean isUsingSpotInstance(StackDtoDelegate stack) {
        return stack.getInstanceGroupViews()
                .stream()
                .map(InstanceGroupView::getTemplate)
                .map(Template::getAttributes)
                .map(Json::getMap)
                .map(attributes -> attributes.getOrDefault(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 0))
                .map(spotPercentage -> Integer.parseInt(spotPercentage.toString()))
                .anyMatch(spotPercentage -> spotPercentage != 0);
    }
}
