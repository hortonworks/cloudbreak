package com.sequenceiq.freeipa.flow.stack.migration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.freeipa.entity.Stack;

import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@ExtendWith(MockitoExtension.class)
public class AwsMigrationUtilTest {

    private static final String ACCOUNT_ID = "accId";

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:accId:freeipa:969db858-ea8f-46ed-9e0d-216dd7ea8bf1";

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

    @Mock
    private EntitlementService entitlementService;

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupNotFound() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any()))
                .thenReturn(DescribeStackResourcesResponse.builder().stackResources(Collections.emptyList()).build());
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        assertTrue(actual);
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFound() {
        StackResource asg1 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id1").build();
        StackResource asg2 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id2").build();

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder()
                .stackResources(List.of(asg1, asg2)).build());
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(Collections.emptyList());
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id2")).thenReturn(Collections.emptyList());
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        assertTrue(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFoundFirstASGHasInstance() {
        StackResource asg1 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id1").build();
        StackResource asg2 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id2").build();

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder()
                .stackResources(List.of(asg1, asg2)).build());
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(List.of("instanceId1"));
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        assertFalse(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupFoundOSecondASGHasInstance() {
        StackResource asg1 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id1").build();
        StackResource asg2 = StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup")
                .physicalResourceId("id2").build();

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder()
                .stackResources(List.of(asg1, asg2)).build());
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id1")).thenReturn(Collections.emptyList());
        when(cfStackUtil.getInstanceIds(amazonAutoScalingClient, "id2")).thenReturn(List.of("instanceId1"));
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        assertFalse(actual);
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationDisabled() {
        Stack stack = new Stack();
        stack.setPlatformvariant("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(false);
        String actual = underTest.calculateUpgradeVariant(stack, ACCOUNT_ID);
        assertEquals("AWS", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledAndVariantIsAws() {
        Stack stack = new Stack();
        stack.setPlatformvariant("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, ACCOUNT_ID);
        assertEquals("AWS_NATIVE", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledWhenVariantIsNotAWS() {
        Stack stack = new Stack();
        stack.setPlatformvariant("GCP");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, ACCOUNT_ID);
        assertEquals("GCP", actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenTriggeredIsAwsNativeAndOriginalAwsAndNeedMigration() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setResourceCrn(RESOURCE_CRN);

        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertTrue(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenTriggeredIsAwsAndOriginalAwsAndDontNeedMigration() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setResourceCrn(RESOURCE_CRN);

        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS");
        assertFalse(actual);
        verify(entitlementService, never()).awsVariantMigrationEnabled(any());
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenTriggeredIsAwsNativeAndOriginalAwsAndMigrationDisabledAndDontNeedMigration() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setResourceCrn(RESOURCE_CRN);

        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(false);

        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenTriggeredIsAwsNativeAndOriginalAwsNativeAndDontNeedMigration() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS_NATIVE");
        stack.setResourceCrn(RESOURCE_CRN);

        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertFalse(actual);
        verify(entitlementService, never()).awsVariantMigrationEnabled(any());
    }
}
