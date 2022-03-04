package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AwsTaggingServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String INSTANCE_ID = "i-nstance";

    private static final String VOLUME_ID = "vol-12345678";

    @InjectMocks
    private AwsTaggingService awsTaggingService;

    @Captor
    private ArgumentCaptor<CreateTagsRequest> tagRequestCaptor;

    @Test
    public void testWhenUserTagsDefined() {
        Map<String, String> userDefined = Maps.newHashMap();
        userDefined.put("userdefinedkey", "userdefinedvalue");
        Collection<Tag> tags = awsTaggingService.prepareCloudformationTags(authenticatedContext(), userDefined);
        assertEquals(1L, tags.size());
    }

    @Test
    public void tesTagRootVolumesForSingleInstance() {
        CloudResource instance = CloudResource.builder()
                .type(ResourceType.AWS_INSTANCE).instanceId(INSTANCE_ID).name(INSTANCE_ID).status(CommonStatus.CREATED).build();

        DescribeInstancesResult describeResult = new DescribeInstancesResult()
                .withReservations(new Reservation()
                        .withInstances(new Instance()
                                .withInstanceId(INSTANCE_ID)
                                .withBlockDeviceMappings(new InstanceBlockDeviceMapping()
                                        .withDeviceName("/dev/sda1")
                                        .withEbs(new EbsInstanceBlockDevice().withVolumeId(VOLUME_ID)))
                                .withRootDeviceName("/dev/sda1"))
                );

        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);
        Map<String, String> userTags = Map.of("key1", "val1", "key2", "val2");

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, List.of(instance), userTags);

        verify(ec2Client, times(1)).createTags(tagRequestCaptor.capture());
        CreateTagsRequest request = tagRequestCaptor.getValue();
        assertEquals(1, request.getResources().size());
        assertEquals(VOLUME_ID, request.getResources().get(0));
        List<com.amazonaws.services.ec2.model.Tag> tags = request.getTags();
        assertThat(tags, containsInAnyOrder(
                hasProperty("key", Matchers.is("key1")),
                hasProperty("key", Matchers.is("key2"))
        ));
        assertThat(tags, containsInAnyOrder(
                hasProperty("value", Matchers.is("val1")),
                hasProperty("value", Matchers.is("val2"))
        ));
    }

    @Test
    public void tesTagRootVolumesWhenNoInstancesReturned() {
        CloudResource instance = CloudResource.builder()
                .type(ResourceType.AWS_INSTANCE).instanceId(INSTANCE_ID).name(INSTANCE_ID).status(CommonStatus.CREATED).build();

        DescribeInstancesResult describeResult = new DescribeInstancesResult()
                .withReservations(new Reservation());

        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, List.of(instance), Map.of());

        verify(ec2Client, times(0)).createTags(tagRequestCaptor.capture());
    }

    @Test
    public void tesTagRootVolumesForInstancesMoreThanSingleRequestLimit() {
        int instanceCount = 1200;

        CloudResource instance = CloudResource.builder()
                .type(ResourceType.AWS_INSTANCE).instanceId(INSTANCE_ID).name(INSTANCE_ID).status(CommonStatus.CREATED).build();

        Instance awsInstance = new Instance()
                .withInstanceId(INSTANCE_ID)
                .withBlockDeviceMappings(new InstanceBlockDeviceMapping()
                        .withDeviceName("/dev/sda1")
                        .withEbs(new EbsInstanceBlockDevice().withVolumeId(VOLUME_ID)))
                .withRootDeviceName("/dev/sda1");

        List<CloudResource> instanceList = new ArrayList<>(instanceCount);
        List<Instance> awsInstances = new ArrayList<>(instanceCount);
        for (int i = 0; i < instanceCount; i++) {
            instanceList.add(instance);
            awsInstances.add(awsInstance);
        }

        DescribeInstancesResult describeResult = new DescribeInstancesResult()
                .withReservations(new Reservation().withInstances(awsInstances));
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, instanceList, Map.of());

        verify(ec2Client, times(2)).createTags(tagRequestCaptor.capture());
        List<CreateTagsRequest> requests = tagRequestCaptor.getAllValues();
        assertEquals(1000, requests.get(0).getResources().size());
        assertEquals(200, requests.get(1).getResources().size());
    }

    @Test
    public void tesTagRootVolumesForInstancesMoreThanSingleRequestLimitAndNotAllVolumesFound() {
        int instanceCount = 1200;

        CloudResource instance = CloudResource.builder()
                .type(ResourceType.AWS_INSTANCE).instanceId(INSTANCE_ID).name(INSTANCE_ID).status(CommonStatus.CREATED).build();

        Instance awsInstance = new Instance()
                .withInstanceId(INSTANCE_ID)
                .withBlockDeviceMappings(new InstanceBlockDeviceMapping()
                        .withDeviceName("/dev/sda1")
                        .withEbs(new EbsInstanceBlockDevice().withVolumeId(VOLUME_ID)))
                .withRootDeviceName("/dev/sda1");

        Instance awsInstanceWithInvalidRootDisk = new Instance()
                .withInstanceId(INSTANCE_ID)
                .withBlockDeviceMappings(new InstanceBlockDeviceMapping()
                        .withDeviceName("/dev/sdb1")
                        .withEbs(new EbsInstanceBlockDevice().withVolumeId(VOLUME_ID)))
                .withRootDeviceName("/dev/sda1");

        List<CloudResource> instanceList = new ArrayList<>(instanceCount);
        for (int i = 0; i < instanceCount; i++) {
            instanceList.add(instance);
        }
        List<Instance> awsInstances = new ArrayList<>(instanceCount);
        for (int i = 0; i < 1100; i++) {
            awsInstances.add(awsInstance);
        }
        for (int i = 0; i < 100; i++) {
            awsInstances.add(awsInstanceWithInvalidRootDisk);
        }

        DescribeInstancesResult describeResult = new DescribeInstancesResult()
                .withReservations(new Reservation().withInstances(awsInstances));
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, instanceList, Map.of());

        verify(ec2Client, times(2)).createTags(tagRequestCaptor.capture());
        List<CreateTagsRequest> requests = tagRequestCaptor.getAllValues();
        assertEquals(1000, requests.get(0).getResources().size());
        assertEquals(100, requests.get(1).getResources().size());
    }

    private AuthenticatedContext authenticatedContext() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("AWS")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential("crn", "credentialname", "account");
        return new AuthenticatedContext(context, cloudCredential);
    }
}
