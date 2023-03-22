package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@ExtendWith(MockitoExtension.class)
public class AutoScalingGroupHandlerTest {

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    @InjectMocks
    private AutoScalingGroupHandler underTest;

    @Test
    public void testUpdateAutoScalingGroupWithLaunchConfiguration() {
        String autoScalingGroupName = "autoScalingGroupName";
        String launchConfigurationName = "launchConfigurationName";
        LaunchConfiguration oldLaunchConfiguration = LaunchConfiguration.builder().build();
        underTest.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroupName, launchConfigurationName);
        ArgumentCaptor<UpdateAutoScalingGroupRequest> captor = ArgumentCaptor.forClass(UpdateAutoScalingGroupRequest.class);
        verify(autoScalingClient, times(1)).updateAutoScalingGroup(captor.capture());
        UpdateAutoScalingGroupRequest request = captor.getValue();

        assertNotNull(request);
        assertEquals(autoScalingGroupName, request.autoScalingGroupName());
        assertEquals(launchConfigurationName, request.launchConfigurationName());
    }

    @Test
    public void testGetAutoScalingGroups() {
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName("cf")
                .build();
        StackResource stackResource = StackResource.builder()
                .logicalResourceId("logicalResourceId")
                .physicalResourceId("physicalResourceId")
                .resourceType("AWS::AutoScaling::AutoScalingGroup").build();
        DescribeStackResourcesResponse resourcesResult = DescribeStackResourcesResponse.builder()
                .stackResources(List.of(stackResource, StackResource.builder().resourceType("other").build()))
                .build();

        when(cloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class))).thenReturn(resourcesResult);

        AutoScalingGroup autoScalingGroup = AutoScalingGroup.builder().autoScalingGroupName(stackResource.physicalResourceId()).build();
        DescribeAutoScalingGroupsResponse scalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(autoScalingGroup)
                .build();
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(scalingGroupsResult);

        Map<AutoScalingGroup, String> autoScalingGroups = underTest.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        assertEquals(1, autoScalingGroups.size());
        assertEquals(autoScalingGroup, autoScalingGroups.entrySet().stream().findFirst().get().getKey());
        assertEquals(stackResource.logicalResourceId(), autoScalingGroups.entrySet().stream().findFirst().get().getValue());

        ArgumentCaptor<DescribeStackResourcesRequest> stackResourcesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeStackResourcesRequest.class);
        verify(cloudFormationClient).describeStackResources(stackResourcesRequestArgumentCaptor.capture());
        assertEquals(cfResource.getName(), stackResourcesRequestArgumentCaptor.getValue().stackName());

        ArgumentCaptor<DescribeAutoScalingGroupsRequest> scalingGroupsRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        verify(autoScalingClient).describeAutoScalingGroups(scalingGroupsRequestArgumentCaptor.capture());
        assertEquals(1, scalingGroupsRequestArgumentCaptor.getValue().autoScalingGroupNames().size());
        assertEquals(stackResource.physicalResourceId(), scalingGroupsRequestArgumentCaptor.getValue().autoScalingGroupNames().get(0));
    }
}
