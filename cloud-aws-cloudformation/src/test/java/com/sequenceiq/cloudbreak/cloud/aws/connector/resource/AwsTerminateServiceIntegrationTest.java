package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;

@ExtendWith(MockitoExtension.class)
public class AwsTerminateServiceIntegrationTest {

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private AwsTerminateService underTest;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudStack cloudStack;

    @Mock
    private AmazonCloudFormationClient cloudFormationRetryClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private Retry retryService;

    @Mock
    private CloudFormationWaiter cfWaiters;

    @Mock
    private AwsContextBuilder contextBuilder;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Mock
    private AwsLoadBalancerCommonService awsLoadBalancerCommonService;

    @Test
    public void testTerminateWhenCloudformationStackTerminated() {
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(cloudFormationRetryClient);
        CloudResource cfStackResource = mock(CloudResource.class);
        when(cfStackResource.getName()).thenReturn("stackName");
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cfStackResource);
        when(retryService.testWith2SecDelayMax5Times(any(Supplier.class))).thenReturn(Boolean.TRUE);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);

        List<CloudResource> resources = List.of(CloudResource.builder().withName("ami-87654321").withType(ResourceType.AWS_ENCRYPTED_AMI).build(),
                CloudResource.builder().withName("snap-1234567812345678").withType(ResourceType.AWS_SNAPSHOT).build(),
                CloudResource.builder().withName("vol-1234567812345678").withType(ResourceType.AWS_ENCRYPTED_VOLUME).build(),
                CloudResource.builder().withName("cfn-12345678").withType(ResourceType.CLOUDFORMATION_STACK).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(retryService, times(1)).testWith2SecDelayMax5Times(any(Supplier.class));
    }

    @Test
    public void testTerminateWhenResourcesNull() {
        underTest.terminate(authenticatedContext(), cloudStack, null);
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingClient, never()).describeAutoScalingGroups(any());

    }

    @Test
    public void testTerminateWhenResourcesEmpty() {
        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of());
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingClient, never()).describeAutoScalingGroups(any());
        assertEquals(0, result.size(), "Resources result should be empty");
    }

    @Test
    public void testTerminateWhenResourcesHasNoCf() {
        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack,
                List.of(CloudResource.builder().withName("ami-87654321").withType(ResourceType.AWS_ENCRYPTED_AMI).build()));
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingClient, never()).describeAutoScalingGroups(any());
        assertEquals(0, result.size(), "Resources result should be empty");
    }

    @Test
    public void testTerminateWhenResourcesHasNoCfButStackNotExist() {
        CloudResource cf = CloudResource.builder().withName("cfn-87654321").withType(ResourceType.CLOUDFORMATION_STACK).build();
        CloudResource lc = CloudResource.builder().withName("lc-87654321").withType(ResourceType.AWS_LAUNCHCONFIGURATION).build();
        Group group = Group.builder().build();

        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cf);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);

        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of(
                CloudResource.builder().withName("ami-87654321").withType(ResourceType.AWS_ENCRYPTED_AMI).build(), cf, lc
        ));
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingClient, never()).describeAutoScalingGroups(any());
        assertEquals(0, result.size(), "Resources result should be empty");
    }

    @Test
    public void testTerminateWhenResourcesHasCf() {
        CloudResource cf = CloudResource.builder().withName("cfn-87654321").withType(ResourceType.CLOUDFORMATION_STACK).build();
        CloudResource lc = CloudResource.builder().withName("lc-87654321").withType(ResourceType.AWS_LAUNCHCONFIGURATION).build();
        Group group = Group.builder()
                .withName("alma")
                .build();
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(List.of()).build();

        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cf);
        when(cfStackUtil.getAutoscalingGroupName(any(), anyString(), anyString())).thenReturn("alma");
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(cloudFormationRetryClient);
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
        when(retryService.testWith2SecDelayMax5Times(any(Supplier.class))).thenReturn(Boolean.TRUE);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);

        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of(
                CloudResource.builder().withName("ami-87654321").withType(ResourceType.AWS_ENCRYPTED_AMI).build(), cf, lc
        ));

        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(retryService, times(1)).testWith2SecDelayMax5Times(any(Supplier.class));
        verify(amazonAutoScalingClient, times(1)).describeAutoScalingGroups(any());
        assertEquals(0, result.size(), "Resources result should have one size list");
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(5L)
                .withName("name")
                .withCrn("crn")
                .withPlatform("platform")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential credential = new CloudCredential("crn", null, "account");
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, credential);
        ac.putParameter(AmazonEc2Client.class, ec2Client);
        return ac;
    }
}
