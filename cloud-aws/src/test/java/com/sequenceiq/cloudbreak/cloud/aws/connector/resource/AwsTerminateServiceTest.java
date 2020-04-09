package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsTerminateStackStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AwsTerminateServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsTerminateService underTest;

    @Mock
    private AwsClient awsClient;

    @Mock
    private EncryptedSnapshotService snapshotService;

    @Mock
    private EncryptedImageCopyService encryptedImageCopyService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudStack cloudStack;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    @Mock
    private AmazonCloudFormationRetryClient cloudFormationRetryClient;

    @Mock
    private AmazonEC2Client ec2Client;

    @Mock
    private Retry retryService;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private AwsTerminateStackStatusCheckerTask awsTerminateStackStatusCheckerTask;

    @Mock
    private AwsBackoffSyncPollingScheduler scheduler;

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
    private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackResourceDoesNotExist() {
        List<CloudResource> resources = List.of(new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackDoesNotExist() {
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(null);

        List<CloudResource> resources = List.of(new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build(),
                new Builder().name("cfn-12345678").type(ResourceType.CLOUDFORMATION_STACK).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackTerminated() {
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);
        when(awsClient.createCloudFormationRetryClient(any())).thenReturn(cloudFormationRetryClient);
        when(awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(any(), any(), any(), any(), any(), any())).thenReturn(awsTerminateStackStatusCheckerTask);
        CloudResource cfStackResource = mock(CloudResource.class);
        when(cfStackResource.getName()).thenReturn("stackName");
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cfStackResource);

        List<CloudResource> resources = List.of(new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build(),
                new Builder().name("cfn-12345678").type(ResourceType.CLOUDFORMATION_STACK).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(cloudFormationRetryClient, times(1)).deleteStack(any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateWhenResourcesNull() {
        underTest.terminate(authenticatedContext(), cloudStack, null);
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingRetryClient, never()).describeAutoScalingGroups(any());

    }

    @Test
    public void testTerminateWhenResourcesEmpty() {
        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of());
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingRetryClient, never()).describeAutoScalingGroups(any());
        Assertions.assertEquals(0, result.size(), "Resources result should be empty");
    }

    @Test
    public void testTerminateWhenResourcesHasNoCf() {
        List<CloudResourceStatus> result = underTest
                .terminate(authenticatedContext(), cloudStack, List.of(new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build()));
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingRetryClient, never()).describeAutoScalingGroups(any());
        Assertions.assertEquals(0, result.size(), "Resources result should be empty");
    }

    @Test
    public void testTerminateWhenResourcesHasNoCfButStackNotExist() {
        CloudResource cf = new Builder().name("cfn-87654321").type(ResourceType.CLOUDFORMATION_STACK).build();
        CloudResource lc = new Builder().name("lc-87654321").type(ResourceType.AWS_LAUNCHCONFIGURATION).build();
        Group group = new Group("alma", InstanceGroupType.GATEWAY, List.of(), null, null, null, null, "", 0, Optional.empty());
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of());

        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cf);
        when(retryService.testWith2SecDelayMax15Times(any())).thenThrow(new Retry.ActionFailedException("Fail no more"));
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);

        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of(
                new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(), cf, lc
        ));
        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(cloudFormationRetryClient, never()).deleteStack(any());
        verify(amazonAutoScalingRetryClient, never()).describeAutoScalingGroups(any());
        Assertions.assertEquals(0, result.size(), "Resources result should be empty");

    }

    @Test
    public void testTerminateWhenResourcesHasCf() {
        CloudResource cf = new Builder().name("cfn-87654321").type(ResourceType.CLOUDFORMATION_STACK).build();
        CloudResource lc = new Builder().name("lc-87654321").type(ResourceType.AWS_LAUNCHCONFIGURATION).build();
        Group group = new Group("alma", InstanceGroupType.GATEWAY, List.of(), null, null, null, null, "", 0, Optional.empty());
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of());

        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cfStackUtil.getCloudFormationStackResource(any())).thenReturn(cf);
        when(cfStackUtil.getAutoscalingGroupName(any(), anyString(), anyString())).thenReturn("alma");
        when(awsClient.createCloudFormationRetryClient(any())).thenReturn(cloudFormationRetryClient);
        when(awsClient.createAutoScalingClient(any(), any())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingRetryClient(any(), any())).thenReturn(amazonAutoScalingRetryClient);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);

        List<CloudResourceStatus> result = underTest.terminate(authenticatedContext(), cloudStack, List.of(
                new Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(), cf, lc
        ));

        verify(awsResourceConnector, times(1)).check(any(), any());
        verify(awsComputeResourceService, times(1)).deleteComputeResources(any(), any(), any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(cloudFormationRetryClient, times(1)).deleteStack(any());
        verify(amazonAutoScalingRetryClient, times(1)).describeAutoScalingGroups(any());
        Assertions.assertEquals(0, result.size(), "Resources result should have one size list");
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential("crn", null);
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, credential);
        ac.putParameter(AmazonEC2Client.class, ec2Client);
        return ac;
    }
}
