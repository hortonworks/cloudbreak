package com.sequenceiq.cloudbreak.cloud.aws.common;

import static java.util.Map.entry;
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
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;
import software.amazon.awssdk.services.ec2.model.VolumeType;
import software.amazon.awssdk.services.kms.model.Tag;

@ExtendWith(MockitoExtension.class)
public class AwsTaggingServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String INSTANCE_ID = "i-nstance";

    private static final String VOLUME_ID = "vol-12345678";

    private static final String GROUP = "COMPUTE";

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
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED)
                .withGroup(GROUP).build();

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

        VolumeAttachment volumeAttachment = VolumeAttachment.builder().instanceId(INSTANCE_ID).volumeId(VOLUME_ID).device("/dev/sda1").build();
        Volume volume = Volume.builder().volumeType(VolumeType.GP2).volumeId(VOLUME_ID).attachments(volumeAttachment).availabilityZone("az")
                .size(200).build();
        DescribeVolumesResponse volumesResponse = DescribeVolumesResponse.builder().volumes(volume).build();

        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(ec2Client.describeInstances(any())).thenReturn(describeResult);
        when(ec2Client.describeVolumes(any())).thenReturn(volumesResponse);
        Map<String, String> userTags = Map.of("key1", "val1", "key2", "val2");

        List<CloudResource> rootVolumeResources = awsTaggingService.tagRootVolumes(authenticatedContext(), ec2Client, List.of(instance), userTags);

        verify(ec2Client, times(1)).createTags(tagRequestCaptor.capture());
        CreateTagsRequest request = tagRequestCaptor.getValue();
        assertEquals(1, request.resources().size());
        assertEquals(VOLUME_ID, request.resources().get(0));
        List<software.amazon.awssdk.services.ec2.model.Tag> tags = request.tags();
        assertThat(userTags, hasEntry(tags.get(0).key(), tags.get(0).value()));
        assertThat(userTags, hasEntry(tags.get(1).key(), tags.get(1).value()));
        assertEquals(1, rootVolumeResources.size());
        CloudResource rootResource = rootVolumeResources.get(0);
        VolumeSetAttributes volumeSetAttributes = rootResource.getParameter("attributes", VolumeSetAttributes.class);
        assertEquals(INSTANCE_ID, rootResource.getInstanceId());
        VolumeSetAttributes.Volume rootVolume = volumeSetAttributes.getVolumes().get(0);
        assertEquals(VOLUME_ID, rootVolume.getId());
        assertEquals("/dev/sda1", rootVolume.getDevice());
        assertEquals(VolumeType.GP2.toString(), rootVolume.getType());
        assertEquals(200, rootVolume.getSize());
    }

    @Test
    public void tesTagRootVolumesWhenNoInstancesReturned() {
        CloudResource instance = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID).withName(INSTANCE_ID).withStatus(CommonStatus.CREATED)
                .withGroup(GROUP).build();

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

        CloudResource instance;

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
            instance = CloudResource.builder()
                    .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID + "_" + i)
                    .withName(INSTANCE_ID).withStatus(CommonStatus.CREATED)
                    .withGroup(GROUP).build();
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

        CloudResource instance;

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
            instance = CloudResource.builder()
                    .withType(ResourceType.AWS_INSTANCE).withInstanceId(INSTANCE_ID + "_" + i)
                    .withName(INSTANCE_ID + "_" + i).withStatus(CommonStatus.CREATED)
                    .withGroup(GROUP).build();
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

    @Test
    void prepareKmsTagsTest() {
        Map<String, String> userDefinedTags = Map.ofEntries(entry("key1", "value1"), entry("key2", "value2"));

        Collection<Tag> resultTags = awsTaggingService.prepareKmsTags(userDefinedTags);
        Assertions.assertThat(resultTags).isNotNull();
        Assertions.assertThat(resultTags).hasSize(2);
        Map<String, String> resultTagsMap = resultTags.stream()
                .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue));
        Assertions.assertThat(resultTagsMap).isEqualTo(userDefinedTags);
    }

}
