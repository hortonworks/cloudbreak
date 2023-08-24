package com.sequenceiq.cloudbreak.cloud.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;

@Component
public class InstanceInAutoScalingGroupUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceInAutoScalingGroupUpdater.class);

    public void updateInstanceInAutoscalingGroup(AmazonEc2Client ec2Client, AutoScalingGroup autoScalingGroup, Group group) {
        for (Instance instance : autoScalingGroup.instances()) {
            String requestedFlavor = group.getReferenceInstanceTemplate().getFlavor();
            LOGGER.info("Instance with name: {}, will use the new type which is: {}", instance.instanceId(),
                    requestedFlavor);
            if (!instance.instanceType().equals(requestedFlavor)) {
                ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = ModifyInstanceAttributeRequest.builder()
                        .instanceId(instance.instanceId())
                        .instanceType(AttributeValue.builder().value(requestedFlavor).build())
                        .build();
                ec2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
            } else {
                LOGGER.info("Instance {} using the same type what was requested: {}", instance.instanceId(), requestedFlavor);
            }
        }
    }
}
