package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsTerminateStackStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.Retry;

@RunWith(MockitoJUnitRunner.class)
public class AwsResourceConnectorTerminateTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsResourceConnector underTest;

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
    private AmazonEC2Client ec2Client;

    @Mock
    private Retry retryService;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private AwsTerminateStackStatusCheckerTask awsTerminateStackStatusCheckerTask;

    @Test
    public void testTerminateShouldNotCleanupEncryptedResourcesWhenNoResourcesExist() {

        underTest.terminate(authenticatedContext(), cloudStack, List.of());

        verify(encryptedImageCopyService, times(0)).deleteResources(any(), any(), any());
        verify(snapshotService, times(0)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackResourceDoesNotExist() {
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);

        List<CloudResource> resources = List.of(new CloudResource.Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new CloudResource.Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new CloudResource.Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackDoesNotExist() {
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(retryService.testWith2SecDelayMax5Times(any())).thenThrow(new Retry.ActionWentFailException("Stack not exists"));

        List<CloudResource> resources = List.of(new CloudResource.Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new CloudResource.Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new CloudResource.Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build(),
                new CloudResource.Builder().name("cfn-12345678").type(ResourceType.CLOUDFORMATION_STACK).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    @Test
    public void testTerminateShouldCleanupEncryptedResourcesWhenCloudformationStackTerminated() {
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(any(), any(), any(), any(), any(), any())).thenReturn(awsTerminateStackStatusCheckerTask);
        when(awsTerminateStackStatusCheckerTask.completed(any())).thenReturn(true);

        List<CloudResource> resources = List.of(new CloudResource.Builder().name("ami-87654321").type(ResourceType.AWS_ENCRYPTED_AMI).build(),
                new CloudResource.Builder().name("snap-1234567812345678").type(ResourceType.AWS_SNAPSHOT).build(),
                new CloudResource.Builder().name("vol-1234567812345678").type(ResourceType.AWS_ENCRYPTED_VOLUME).build(),
                new CloudResource.Builder().name("cfn-12345678").type(ResourceType.CLOUDFORMATION_STACK).build());

        underTest.terminate(authenticatedContext(), cloudStack, resources);

        verify(cloudFormationClient, times(1)).deleteStack(any());
        verify(encryptedImageCopyService, times(1)).deleteResources(any(), any(), any());
        verify(snapshotService, times(1)).deleteResources(any(), any(), any());
    }

    private static AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "owner", "variant", location);
        CloudCredential credential = new CloudCredential(1L, null);
        return new AuthenticatedContext(cloudContext, credential);
    }
}
