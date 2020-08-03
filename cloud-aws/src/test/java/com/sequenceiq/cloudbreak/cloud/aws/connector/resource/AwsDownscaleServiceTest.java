package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsDownscaleServiceTest {

    @InjectMocks
    private AwsDownscaleService awsDownscaleService;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private AwsClient awsClient;

    @Test
    void downscaleASGRetryTest() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication);
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")), "1", "1"),
                new CloudCredential());
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);

        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        AmazonServiceException amazonServiceException = new AmazonServiceException("i-worker2, i-worker3 are not part of Auto Scaling");
        amazonServiceException.setErrorCode("ValidationError");
        when(amazonAutoScalingRetryClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
                .thenThrow(amazonServiceException)
                .thenReturn(new DetachInstancesResult());

        awsDownscaleService.downscale(authenticatedContext, stack, resources, cloudInstances, null);

        List<DetachInstancesRequest> allValues = detachInstancesRequestArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).getInstanceIds(), contains("i-worker1", "i-worker2", "i-worker3"));
        assertThat(allValues.get(1).getInstanceIds(), contains("i-worker1"));
        verify(amazonAutoScalingRetryClient, times(2)).detachInstances(any());
    }

    @Test
    void downscaleASGRetryStackOverflowPreventionTest() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication);
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")), "1", "1"),
                new CloudCredential());
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);

        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        AmazonServiceException firstAmazonServiceException = new AmazonServiceException("i-worker2, i-worker3 are not part of Auto Scaling");
        AmazonServiceException secondAmazonServiceException = new AmazonServiceException("i-worker1 are not part of Auto Scaling");
        firstAmazonServiceException.setErrorCode("ValidationError");
        secondAmazonServiceException.setErrorCode("ValidationError");
        when(amazonAutoScalingRetryClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
                .thenThrow(firstAmazonServiceException)
                .thenThrow(secondAmazonServiceException);

        Assertions.assertThrows(AmazonServiceException.class, () -> {
            awsDownscaleService.downscale(authenticatedContext, stack, resources, cloudInstances, null);
        });


        List<DetachInstancesRequest> allValues = detachInstancesRequestArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).getInstanceIds(), contains("i-worker1", "i-worker2", "i-worker3"));
        assertThat(allValues.get(1).getInstanceIds(), contains("i-worker1"));
        verify(amazonAutoScalingRetryClient, times(2)).detachInstances(any());
    }

}