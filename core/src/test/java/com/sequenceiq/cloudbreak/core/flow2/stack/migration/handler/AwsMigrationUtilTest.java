package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

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

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private MetadataSetupService metadataSetupService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private EventBus eventBus;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenASGroupNotFound() {
        when(cloudResource.getName()).thenReturn("stack-name");
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenThrow(CloudFormationException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("stack-name does not exist").build()).build());
        boolean actual = underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource);
        assertTrue(actual);
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id1");
        verify(cfStackUtil, never()).getInstanceIds(amazonAutoScalingClient, "id2");
    }

    @Test
    public void testAllInstancesDeletedFromCloudFormationWhenThrowAwsClientException() {
        when(cloudResource.getName()).thenReturn("stack-name");
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.describeStackResources(any())).thenThrow(CloudFormationException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("something error happened").build()).build());
        CloudFormationException actual = assertThrows(CloudFormationException.class,
                () -> underTest.allInstancesDeletedFromCloudFormation(ac, cloudResource));
        assertEquals("something error happened", actual.awsErrorDetails().errorMessage());
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
    void testChangeLoadbalancer() throws InterruptedException {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(0L);
        AwsMigrationUtil spiedUnderTest = spy(underTest);
        CollectLoadBalancerMetadataCloudPlatformRequest collectLoadBalancerMetadataRequest = mock(CollectLoadBalancerMetadataCloudPlatformRequest.class);
        doReturn(collectLoadBalancerMetadataRequest).when(spiedUnderTest).getCollectLoadBalancerMetadataRequest(any(), any(), any());
        when(collectLoadBalancerMetadataRequest.await()).thenReturn(new CollectLoadBalancerMetadataCloudPlatformResult(0L, List.of()));
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(anyLong(), eq(Boolean.FALSE))).thenReturn(stackDto);

        spiedUnderTest.changeLoadBalancer(ac);

        verify(metadataSetupService, times(1)).saveLoadBalancerMetadata(any(), any());
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForLoadBalancers(any());
    }
}
