package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@ExtendWith(MockitoExtension.class)
class Ec2TagUpdateStrategyTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @InjectMocks
    private Ec2TagUpdateStrategy underTest;

    @Test
    void testUpdateTagsForAwsInstance() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_INSTANCE, INSTANCE_ID, null);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(INSTANCE_ID)));
    }

    @Test
    void testUpdateTagsForAwsRootDisk() {
        String volumeId = "volumeId";
        CloudResource cloudResource = buildResource(ResourceType.AWS_ROOT_DISK, INSTANCE_ID, null);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(
                        Volume.builder()
                                .volumeId(volumeId)
                                .build())
                .build();
        when(ec2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenReturn(describeVolumesResponse);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(volumeId)));
    }

    @Test
    void testUpdateTagsForAwsVolumeSet() {
        String volumeId = "volumeId";
        CloudResource cloudResource = buildResource(ResourceType.AWS_VOLUMESET, INSTANCE_ID, null);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(
                        Volume.builder()
                                .volumeId(volumeId)
                        .build())
                .build();
        when(ec2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenReturn(describeVolumesResponse);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(volumeId)));
    }

    @Test
    void testUpdateTagsForAwsRootDiskTagging() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_ROOT_DISK_TAGGING, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsSecurityGroup() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_SECURITY_GROUP, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsEncryptedVolume() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_ENCRYPTED_VOLUME, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsSnapshot() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_SNAPSHOT, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsEncryptedAmi() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_ENCRYPTED_AMI, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsReservedIp() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_RESERVED_IP, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    @Test
    void testUpdateTagsForAwsSshKey() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_SSH_KEY, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(createTagsRequestWithResources(List.of(RESOURCE_REFERENCE)));
    }

    private CloudResource buildResource(ResourceType type, String instanceId, String reference) {
        return CloudResource.builder()
                .withType(type)
                .withName(type.name().toLowerCase())
                .withInstanceId(instanceId)
                .withReference(reference)
                .withParameters(Collections.emptyMap())
                .build();
    }

    private CreateTagsRequest createTagsRequestWithResources(List<String> resources) {
        return CreateTagsRequest.builder()
                .resources(resources)
                .tags(toEc2Tags(USER_DEFINED_TAGS))
                .build();
    }

    private List<Tag> toEc2Tags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();
    }
}