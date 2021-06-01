package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public class AutoScalingGroupHandlerTest {

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    private AutoScalingGroupHandler underTest = new AutoScalingGroupHandler();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateAutoScalingGroupWithLaunchConfiguration() {
        String autoScalingGroupName = "autoScalingGroupName";
        String launchConfigurationName = "launchConfigurationName";
        LaunchConfiguration oldLaunchConfiguration = new LaunchConfiguration();
        underTest.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroupName, oldLaunchConfiguration, launchConfigurationName);
        ArgumentCaptor<UpdateAutoScalingGroupRequest> captor = ArgumentCaptor.forClass(UpdateAutoScalingGroupRequest.class);
        verify(autoScalingClient, times(1)).updateAutoScalingGroup(captor.capture());
        UpdateAutoScalingGroupRequest request = captor.getValue();

        assertNotNull(request);
        assertEquals(autoScalingGroupName, request.getAutoScalingGroupName());
        assertEquals(launchConfigurationName, request.getLaunchConfigurationName());
    }

    @Test
    public void testGetAutoScalingGroups() {
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name("cf")
                .build();
        DescribeStackResourcesResult resourcesResult = new DescribeStackResourcesResult();
        StackResource stackResource = new StackResource()
                .withLogicalResourceId("logicalResourceId")
                .withPhysicalResourceId("physicalResourceId")
                .withResourceType("AWS::AutoScaling::AutoScalingGroup");
        resourcesResult.getStackResources().add(stackResource);
        resourcesResult.getStackResources().add(new StackResource().withResourceType("other"));
        when(cloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class))).thenReturn(resourcesResult);

        DescribeAutoScalingGroupsResult scalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withAutoScalingGroupName(stackResource.getPhysicalResourceId());
        scalingGroupsResult.getAutoScalingGroups().add(autoScalingGroup);
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(scalingGroupsResult);

        Map<AutoScalingGroup, String> autoScalingGroups = underTest.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        assertEquals(1, autoScalingGroups.size());
        assertEquals(autoScalingGroup, autoScalingGroups.entrySet().stream().findFirst().get().getKey());
        assertEquals(stackResource.getLogicalResourceId(), autoScalingGroups.entrySet().stream().findFirst().get().getValue());

        ArgumentCaptor<DescribeStackResourcesRequest> stackResourcesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeStackResourcesRequest.class);
        verify(cloudFormationClient).describeStackResources(stackResourcesRequestArgumentCaptor.capture());
        assertEquals(cfResource.getName(), stackResourcesRequestArgumentCaptor.getValue().getStackName());

        ArgumentCaptor<DescribeAutoScalingGroupsRequest> scalingGroupsRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        verify(autoScalingClient).describeAutoScalingGroups(scalingGroupsRequestArgumentCaptor.capture());
        assertEquals(1, scalingGroupsRequestArgumentCaptor.getValue().getAutoScalingGroupNames().size());
        assertEquals(stackResource.getPhysicalResourceId(), scalingGroupsRequestArgumentCaptor.getValue().getAutoScalingGroupNames().get(0));
    }
}