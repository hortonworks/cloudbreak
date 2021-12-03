package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class AwsMigrationUtilTest {

    @InjectMocks
    private AwsMigrationUtil underTest;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupNotFound() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult()
                .withStackResources(Collections.emptyList()));
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        Assertions.assertTrue(actual);
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFound() {
        StackResource asg1 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id1");
        StackResource asg2 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id2");

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult()
                .withStackResources(List.of(asg1, asg2)));
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(Collections.emptyList());
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id2")).thenReturn(Collections.emptyList());
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        Assertions.assertTrue(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFoundFirstASGHasInstance() {
        StackResource asg1 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id1");
        StackResource asg2 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id2");

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult()
                .withStackResources(List.of(asg1, asg2)));
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(List.of("instanceId1"));
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        Assertions.assertFalse(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFoundOSecondASGHasInstance() {
        StackResource asg1 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id1");
        StackResource asg2 = new StackResource().withResourceType("AWS::AutoScaling::AutoScalingGroup")
                .withPhysicalResourceId("id2");

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult()
                .withStackResources(List.of(asg1, asg2)));
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(Collections.emptyList());
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id2")).thenReturn(List.of("instanceId1"));
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        Assertions.assertFalse(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id2");
    }
}
