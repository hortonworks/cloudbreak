package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Reservation;

@ExtendWith(MockitoExtension.class)
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
        Collection<software.amazon.awssdk.services.cloudformation.model.Tag> tags = awsTaggingService.prepareCloudformationTags(authenticatedContext(),
                userDefined);
        assertEquals(1L, tags.size());
    }

    @Test
    public void tesTagRootVolumesForSingleInstance() {
        CloudResource instance = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED).build();

        DescribeInstancesResponse describeResult = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .instanceId(INSTANCE_ID)
                                .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                                        .deviceName("/dev/sda1")
                                        .ebs(EbsInstanceBlockDevice.builder().volumeId(VOLUME_ID).build())
                                        .build())
                                .rootDeviceName("/dev/sda1")
                                .build())
                        .build()
                ).build();

        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);
        Map<String, String> userTags = Map.of("key1", "val1", "key2", "val2");

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, List.of(instance), userTags);

        verify(ec2Client, times(1)).createTags(tagRequestCaptor.capture());
        CreateTagsRequest request = tagRequestCaptor.getValue();
        assertEquals(1, request.resources().size());
        assertEquals(VOLUME_ID, request.resources().get(0));
        List<software.amazon.awssdk.services.ec2.model.Tag> tags = request.tags();
        assertThat(userTags, hasEntry(tags.get(0).key(), tags.get(0).value()));
        assertThat(userTags, hasEntry(tags.get(1).key(), tags.get(1).value()));
    }

    @Test
    public void tesTagRootVolumesWhenNoInstancesReturned() {
        CloudResource instance = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED).build();

        DescribeInstancesResponse describeResult = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().build()).build();

        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, List.of(instance), Map.of());

        verify(ec2Client, times(0)).createTags(tagRequestCaptor.capture());
    }

    @Test
    public void tesTagRootVolumesForInstancesMoreThanSingleRequestLimit() {
        int instanceCount = 1200;

        CloudResource instance = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED).build();

        Instance awsInstance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                        .deviceName("/dev/sda1")
                        .ebs(EbsInstanceBlockDevice.builder().volumeId(VOLUME_ID).build())
                        .build())
                .rootDeviceName("/dev/sda1").build();

        List<CloudResource> instanceList = new ArrayList<>(instanceCount);
        List<Instance> awsInstances = new ArrayList<>(instanceCount);
        for (int i = 0; i < instanceCount; i++) {
            instanceList.add(instance);
            awsInstances.add(awsInstance);
        }

        DescribeInstancesResponse describeResult = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(awsInstances).build())
                .build();
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, instanceList, Map.of());

        verify(ec2Client, times(2)).createTags(tagRequestCaptor.capture());
        List<CreateTagsRequest> requests = tagRequestCaptor.getAllValues();
        assertEquals(1000, requests.get(0).resources().size());
        assertEquals(200, requests.get(1).resources().size());
    }

    @Test
    public void tesTagRootVolumesForInstancesMoreThanSingleRequestLimitAndNotAllVolumesFound() {
        int instanceCount = 1200;

        CloudResource instance = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED).build();

        Instance awsInstance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                        .deviceName("/dev/sda1")
                        .ebs(EbsInstanceBlockDevice.builder().volumeId(VOLUME_ID).build())
                        .build())
                .rootDeviceName("/dev/sda1")
                .build();

        Instance awsInstanceWithInvalidRootDisk = Instance.builder()
                .instanceId(INSTANCE_ID)
                .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                        .deviceName("/dev/sdb1")
                        .ebs(EbsInstanceBlockDevice.builder().volumeId(VOLUME_ID).build())
                        .build())
                .rootDeviceName("/dev/sda1")
                .build();

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

        DescribeInstancesResponse describeResult = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(awsInstances).build())
                .build();
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);

        awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, instanceList, Map.of());

        verify(ec2Client, times(2)).createTags(tagRequestCaptor.capture());
        List<CreateTagsRequest> requests = tagRequestCaptor.getAllValues();
        assertEquals(1000, requests.get(0).resources().size());
        assertEquals(100, requests.get(1).resources().size());
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
