package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;

@Component
public class InstanceInAutoScalingGroupUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceInAutoScalingGroupUpdater.class);

    public void updateInstanceInAutoscalingGroup(AmazonEc2Client ec2Client, AutoScalingGroup autoScalingGroup, Group group) {
        String requestedFlavor = group.getReferenceInstanceTemplate().getFlavor();

        Set<String> instanceIds = autoScalingGroup.instances().stream()
                .map(Instance::instanceId)
                .collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(instanceIds)) {
            List<Reservation> instanceReservations = describeInstances(ec2Client, instanceIds, autoScalingGroup.autoScalingGroupName());
            instanceReservations.forEach(reservation -> {
                reservation.instances().forEach(instance -> modifyInstanceTypeIfNecessary(ec2Client, instance, requestedFlavor));
            });
        } else {
            LOGGER.info("No instance is presented for group: {} and auto scaling group: {}. No modify instance request is required.", group.getName(),
                    autoScalingGroup.autoScalingGroupName());
        }
    }

    private static List<Reservation> describeInstances(AmazonEc2Client ec2Client, Set<String> instanceIds, String autoScalingGroupName) {
        try {
            LOGGER.debug("Describing instances for ids: '{}'", String.join(",", instanceIds));
            DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                    .instanceIds(instanceIds)
                    .build();
            return ec2Client.describeInstances(describeInstancesRequest).reservations();
        } catch (Ec2Exception e) {
            String message = String.format("Failed to describe instances with ids('%s') for auto scaling group('%s')", String.join(",", instanceIds),
                    autoScalingGroupName);
            if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND_ERROR_CODE)) {
                message = String.format("Some of the instances in the auto scaling group('%s') does not exist on EC2: ", autoScalingGroupName);
            }
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message + e.getMessage(), e);
        } catch (SdkClientException e) {
            String msg = String.format("AWS EC2 could not be contacted for a response during describing instances for auto scaling group('%s')",
                    autoScalingGroupName);
            LOGGER.warn(msg, e);
            throw new CloudbreakServiceException(msg + e.getMessage(), e);
        }
    }

    private void modifyInstanceTypeIfNecessary(AmazonEc2Client ec2Client, software.amazon.awssdk.services.ec2.model.Instance instance, String requestedFlavor) {
        String currentInstanceType = instance.instanceTypeAsString();
        if (!currentInstanceType.equals(requestedFlavor)) {
            LOGGER.info("Instance with name: {}, instance type will be modified from '{}' the new type '{}'", instance.instanceId(),
                    currentInstanceType, requestedFlavor);
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
