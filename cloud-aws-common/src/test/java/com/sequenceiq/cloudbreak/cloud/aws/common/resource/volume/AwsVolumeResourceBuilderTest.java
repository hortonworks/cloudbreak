package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.util.Pair;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUtilService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.model.VolumeType;

@ExtendWith({ MockitoExtension.class })
class AwsVolumeResourceBuilderTest {

    private static final long PRIVATE_ID = 1234L;

    private static final String FLAVOR = "medium";

    private static final String GROUP_NAME = "master";

    private static final long TEMPLATE_ID = 567L;

    private static final String IMAGE_ID = "imageId";

    private static final int ROOT_VOLUME_SIZE = 30;

    private static final String INSTANCE_ID = "SOME_ID";

    private static final String REGION_NAME = "region";

    private static final String MOUNT_PREFIX = "/hadoop/volume_";

    private static final int VOLUME_SIZE = 78;

    private static final String TYPE_GP2 = AwsDiskType.Gp2.value();

    private static final String TYPE_EPHEMERAL = AwsDiskType.Ephemeral.value();

    private static final String TYPE_STANDARD = AwsDiskType.Standard.value();

    private static final String VOLUME_SET_NAME = "volumeName";

    private static final String VOLUME_ID = "volumeId";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final Map<String, String> TAGS = Map.ofEntries(entry("key1", "value1"), entry("key2", "value2"));

    private static final Collection<Tag> EC2_TAGS = List.of(Tag.builder().key("ec2_key").value("ec2_value").build());

    private static final TagSpecification TAG_SPECIFICATION = TagSpecification.builder()
            .resourceType(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME).tags(EC2_TAGS).build();

    private static final String ENCRYPTION_KEY_ARN = "encryptionKeyArn";

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsCommonDiskUtilService awsCommonDiskUtilService;

    @Mock
    private CommonAwsClient commonAwsClient;

    @InjectMocks
    private AwsVolumeResourceBuilder underTest;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private VolumeResourceCollector volumeResourceCollector;

    @Spy
    private AwsMethodExecutor awsMethodExecutor;

    @Spy
    private AwsInstanceFinder awsInstanceFinder;

    @Captor
    private ArgumentCaptor<CreateVolumeRequest> createVolumeRequestCaptor;

    @Captor
    private ArgumentCaptor<ModifyInstanceAttributeRequest> modifyInstanceAttributeRequestCaptor;

    @BeforeEach
    void setUp() {
        CloudResource cloudResource = mock(CloudResource.class);
        lenient().when(cloudResource.getType()).thenReturn(ResourceType.AWS_INSTANCE);
        lenient().when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        lenient().when(awsContext.getComputeResources(PRIVATE_ID)).thenReturn(List.of(cloudResource));
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getLocation()).thenReturn(location);
        lenient().when(location.getRegion()).thenReturn(region);
        lenient().when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));
        lenient().when(region.value()).thenReturn(REGION_NAME);
        lenient().when(cloudStack.getTags()).thenReturn(TAGS);
        lenient().when(awsTaggingService.prepareEc2Tags(TAGS)).thenReturn(EC2_TAGS);
        lenient().when(awsCommonDiskUtilService.isEncryptedVolumeRequested(any())).thenCallRealMethod();
        lenient().when(awsCommonDiskUtilService.getVolumeEncryptionKey(any(), anyBoolean())).thenCallRealMethod();
        lenient().when(awsCommonDiskUtilService.getTagSpecification(any())).thenReturn(TAG_SPECIFICATION);
        lenient().when(commonAwsClient.createEc2Client(isA(AuthenticatedContext.class))).thenReturn(amazonEC2Client);
    }

    @Test
    void buildTestWhenNoVolumesAtAll() throws Exception {
        Group group = createGroup(emptyList(), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group, emptyList(), cloudStack);

        assertTrue(result.isEmpty());
    }

    @Test
    void buildTestWhenEphemeralVolumesOnly() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(emptyList())), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        assertTrue(volumes.isEmpty());
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndNoEncryption() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(volume, null, false));
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenEphemeralAndAttachedVolumesAndNoEncryption() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL), createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 1L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(volume, null, false));
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdc");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndIneffectiveEncryptionWithDefaultKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())), 0L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
            .thenReturn(createVolumeRequest(volume, null, false));
        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndEffectiveEncryptionWithDefaultKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())), 0L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(null), eq(true), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(volume, null, true));
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, true, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndEncryptionWithIneffectiveCustomKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name()),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN)), 0L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(null), eq(true), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(volume, null, true));
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, true, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndEncryptionWithEffectiveCustomKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name()),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN)), 0L);
        VolumeSetAttributes.Volume volume = createVolumeForVolumeSet(TYPE_GP2);
        when(awsCommonDiskUtilService.createVolumeRequest(eq(volume), eq(TAG_SPECIFICATION), eq(ENCRYPTION_KEY_ARN), eq(true), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(volume, ENCRYPTION_KEY_ARN, true));
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(volume))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, true, ENCRYPTION_KEY_ARN);
    }

    @Test
    void testBuildOffsetsDeviceNamesOfDetachedVolumeSetByTheNumberOfTemporaryStorageVolumes() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2), createVolume(TYPE_STANDARD)), Map.of(), 2L);
        CloudResource cloudResource = createVolumeSet(List.of(
                createVolumeForVolumeSet(TYPE_GP2, "/dev/xvdb"),
                createVolumeForVolumeSet(TYPE_STANDARD, "/dev/xvdc")));
        cloudResource.setStatus(CommonStatus.DETACHED);

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group, List.of(cloudResource), cloudStack);

        assertTrue(result.isEmpty());
        List<VolumeSetAttributes.Volume> volumes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes();
        assertThat(volumes).extracting(VolumeSetAttributes.Volume::getDevice).containsExactlyInAnyOrder("/dev/xvdd", "/dev/xvde");
        verify(resourceNotifier).notifyUpdate(cloudResource, authenticatedContext.getCloudContext());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void fetchCloudResourceFromDBIfAvailableWhenNotAvailableInTheDB() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("groupName");
        CloudResource instanceResource = createAwsInstance();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"))).thenReturn(List.of());
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.REQUESTED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"))).thenReturn(List.of());
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(instanceResource.getInstanceId(), authenticatedContext, group);
        assertTrue(actual.isEmpty());
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.REQUESTED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void fetchCloudResourceFromDBIfAvailableWhenAvailableAsRequested() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("groupName");
        CloudResource instanceResource = createAwsInstance();
        CloudResource volumeSet = CloudResource.builder().cloudResource(createVolumeSet(emptyList())).withInstanceId("instanceId").build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"))).thenReturn(List.of());
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.REQUESTED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"))).thenReturn(List.of(volumeSet));
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(instanceResource.getInstanceId(), authenticatedContext, group);
        assertFalse(actual.isEmpty());
        assertEquals(volumeSet, actual.get());
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.REQUESTED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void fetchCloudResourceFromDBIfAvailableWhenAvailableAsCreated() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("groupName");
        CloudResource instanceResource = createAwsInstance();
        CloudResource volumeSet = CloudResource.builder().cloudResource(createVolumeSet(emptyList())).withInstanceId("instanceId").build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"))).thenReturn(List.of(volumeSet));
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(instanceResource.getInstanceId(), authenticatedContext, group);
        assertFalse(actual.isEmpty());
        assertEquals(volumeSet, actual.get());
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.CREATED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStackAndInstanceGroup(eq(CommonStatus.REQUESTED), eq(ResourceType.AWS_VOLUMESET),
                any(), eq("groupName"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteTurnOnDeleteOntermination() throws PreserveResourceException {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        when(amazonEC2Client.describeVolumes(any())).thenReturn(describeVolumesResult(VolumeState.IN_USE));
        when(cloudResource.getParameter(any(), any())).thenReturn(volumeSetAttributes);
        when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(volumeSetAttributes.getDeleteOnTermination()).thenReturn(Boolean.TRUE);

        underTest.delete(awsContext, authenticatedContext, cloudResource);

        verify(amazonEC2Client).modifyInstanceAttribute(modifyInstanceAttributeRequestCaptor.capture());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = modifyInstanceAttributeRequestCaptor.getValue();

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().getFirst().ebs().deleteOnTermination());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteTurnOnDeleteOnterminationEvenIfOneOfTheVolumesAreDetached() throws PreserveResourceException {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);

        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        when(amazonEC2Client.describeVolumes(any())).thenReturn(describeVolumesResult(VolumeState.AVAILABLE));
        when(cloudResource.getParameter(any(), any())).thenReturn(volumeSetAttributes);
        when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(volumeSetAttributes.getDeleteOnTermination()).thenReturn(Boolean.TRUE);

        underTest.delete(awsContext, authenticatedContext, cloudResource);

        verify(amazonEC2Client).modifyInstanceAttribute(modifyInstanceAttributeRequestCaptor.capture());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = modifyInstanceAttributeRequestCaptor.getValue();

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().getFirst().ebs().deleteOnTermination());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteTurnOnDeleteOnterminationEvenIfOneOfTheVolumesAreDeleted() throws PreserveResourceException {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);

        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        AwsServiceException deleted = Ec2Exception.builder()
                .message("")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("InvalidVolume.NotFound").build())
                .build();
        when(amazonEC2Client.describeVolumes(any())).thenThrow(deleted);
        when(cloudResource.getParameter(any(), any())).thenReturn(volumeSetAttributes);
        when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(volumeSetAttributes.getDeleteOnTermination()).thenReturn(Boolean.TRUE);

        underTest.delete(awsContext, authenticatedContext, cloudResource);

        verify(amazonEC2Client).modifyInstanceAttribute(modifyInstanceAttributeRequestCaptor.capture());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = modifyInstanceAttributeRequestCaptor.getValue();

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().getFirst().ebs().deleteOnTermination());
    }

    @Test()
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteTurnOffDeleteOntermination() {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        when(amazonEC2Client.describeVolumes(any())).thenReturn(describeVolumesResult(VolumeState.IN_USE));
        when(cloudResource.getParameter(any(), any())).thenReturn(volumeSetAttributes);
        when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(volumeSetAttributes.getDeleteOnTermination()).thenReturn(Boolean.FALSE);

        assertThrows(PreserveResourceException.class, () -> underTest.delete(awsContext, authenticatedContext, cloudResource));

        verify(amazonEC2Client).modifyInstanceAttribute(modifyInstanceAttributeRequestCaptor.capture());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = modifyInstanceAttributeRequestCaptor.getValue();

        assertFalse(modifyInstanceAttributeRequest.blockDeviceMappings().getFirst().ebs().deleteOnTermination());
    }

    @Test()
    @MockitoSettings(strictness = Strictness.LENIENT)
    void skipModifyInstanceAttributeDuringDeleteWhenNoAttachedResourcesFound() {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of()));
        when(amazonEC2Client.describeVolumes(any())).thenReturn(describeVolumesResult(VolumeState.IN_USE));
        when(cloudResource.getParameter(any(), any())).thenReturn(volumeSetAttributes);
        when(cloudResource.getInstanceId()).thenReturn(INSTANCE_ID);
        when(volumeSetAttributes.getDeleteOnTermination()).thenReturn(Boolean.FALSE);

        assertThrows(PreserveResourceException.class, () -> underTest.delete(awsContext, authenticatedContext, cloudResource));

        verify(amazonEC2Client, never()).modifyInstanceAttribute(any());
    }

    @Test
    void testCreateReturnsEmptyWhenNoVolumes() {
        List<CloudResource> result = underTest.create(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, createGroup(List.of(), Map.of(), 0L), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateReturnsEmptyWhenAllVolumesEphemeral() {
        Volume volume1 = new Volume("/vol1", AwsDiskType.Ephemeral.value(), 5, CloudVolumeUsageType.GENERAL);
        Volume volume2 = new Volume("/vol2", AwsDiskType.Ephemeral.value(), 5, CloudVolumeUsageType.GENERAL);
        List<CloudResource> result = underTest.create(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext,
                createGroup(List.of(volume1, volume2), Map.of(), 0L), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckResourcesShouldReturnTheResources() {
        List<CloudResource> resources = new ArrayList<>();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder()
                .volumes(software.amazon.awssdk.services.ec2.model.Volume.builder().state(VolumeState.AVAILABLE).build()).build();
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        when(amazonEC2Client.describeVolumes(any())).thenReturn(response);

        List<CloudResourceStatus> actual = underTest.checkResources(ResourceType.AWS_VOLUMESET, awsContext, authenticatedContext, resources);

        assertEquals(ResourceStatus.CREATED, actual.getFirst().getStatus());
        verify(volumeResourceCollector).getVolumeIdsByVolumeResources(any(), any(), any());
        verify(amazonEC2Client).describeVolumes(any());
    }

    @Test
    void testCheckResourcesShouldReturnTheResourcesWithDeletedStatusWhenAwsReturnsNotFoundError() {
        List<CloudResource> resources = new ArrayList<>();
        Ec2Exception ec2Exception = (Ec2Exception) Ec2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("InvalidVolume.NotFound").build()).build();
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(List.of(VOLUME_ID), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        when(amazonEC2Client.describeVolumes(any())).thenThrow(ec2Exception);

        List<CloudResourceStatus> actual = underTest.checkResources(ResourceType.AWS_VOLUMESET, awsContext, authenticatedContext, resources);

        assertEquals(ResourceStatus.DELETED, actual.getFirst().getStatus());
        verify(volumeResourceCollector).getVolumeIdsByVolumeResources(any(), any(), any());
        verify(amazonEC2Client).describeVolumes(any());
    }

    @Test
    void testCheckResourcesShouldReturnTheResourcesWithDeletedStatusWhenThereAreNoVolumeIdsFound() {
        List<CloudResource> resources = new ArrayList<>();
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(any(), any(), any()))
                .thenReturn(Pair.of(Collections.emptyList(), List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2))))));
        List<CloudResourceStatus> actual = underTest.checkResources(ResourceType.AWS_VOLUMESET, awsContext, authenticatedContext, resources);

        assertEquals(ResourceStatus.DELETED, actual.getFirst().getStatus());
        verify(volumeResourceCollector).getVolumeIdsByVolumeResources(any(), any(), any());
        verifyNoInteractions(amazonEC2Client);
    }

    @SuppressWarnings("unchecked")
    private void setUpTaskExecutors() {
        when(intermediateBuilderExecutor.submit(isA(Runnable.class))).thenAnswer(invocation -> {
            FutureTask<Void> futureTask = new FutureTask<>(invocation.getArgument(0, Runnable.class), null);
            futureTask.run();
            return futureTask;
        });
    }

    private Volume createVolume(String type) {
        return new Volume(MOUNT_PREFIX + type, type, VOLUME_SIZE, CloudVolumeUsageType.GENERAL);
    }

    private Group createGroup(List<Volume> volumes, Map<String, Object> templateParameters, long temporaryStorageCount) {
        InstanceTemplate template = new InstanceTemplate(FLAVOR, GROUP_NAME, PRIVATE_ID, volumes, InstanceStatus.CREATE_REQUESTED, templateParameters,
                TEMPLATE_ID, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, temporaryStorageCount);
        CloudInstance instance = new CloudInstance(INSTANCE_ID, template, null, "subnet-1", "az1");
        return Group.builder()
                .withName(GROUP_NAME)
                .withInstances(singletonList(instance))
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .build();
    }

    private VolumeSetAttributes.Volume createVolumeForVolumeSet(String type) {
        return new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, type, CloudVolumeUsageType.GENERAL);
    }

    private VolumeSetAttributes.Volume createVolumeForVolumeSet(String type, String deviceName) {
        return new VolumeSetAttributes.Volume(null, deviceName, VOLUME_SIZE, type, CloudVolumeUsageType.GENERAL);
    }

    private CloudResource createVolumeSet(List<VolumeSetAttributes.Volume> volumes) {
        return CloudResource.builder()
                .withType(ResourceType.AWS_VOLUMESET)
                .withName(VOLUME_SET_NAME)
                .withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                        .withAvailabilityZone(AVAILABILITY_ZONE)
                        .withVolumes(volumes)
                        .build()))
                .build();
    }

    private CloudResource createAwsInstance() {
        return CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withName("name")
                .withInstanceId("instanceId")
                .withStatus(CommonStatus.CREATED)
                .build();
    }

    private CreateVolumeResponse createCreateVolumeResult() {
        return CreateVolumeResponse.builder().volumeId(VOLUME_ID).build();
    }

    private DescribeVolumesResponse describeVolumesResult(VolumeState state) {
        software.amazon.awssdk.services.ec2.model.Volume volume = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .state(state)
                .build();
        return DescribeVolumesResponse.builder().volumes(List.of(volume)).build();
    }

    private List<VolumeSetAttributes.Volume> verifyResultAndGetVolumes(List<CloudResource> result) {
        assertThat(result).hasSize(1);
        CloudResource cloudResource = result.getFirst();
        assertThat(cloudResource.getStatus()).isEqualTo(CommonStatus.CREATED);
        VolumeSetAttributes volumeSet = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        assertThat(volumeSet).isNotNull();
        List<VolumeSetAttributes.Volume> volumes = volumeSet.getVolumes();
        assertThat(volumes).isNotNull();
        return volumes;
    }

    private void verifyVolumes(List<VolumeSetAttributes.Volume> volumes, String expectedDevice) {
        assertThat(volumes).hasSize(1);
        VolumeSetAttributes.Volume volume = volumes.getFirst();
        assertThat(volume).isNotNull();
        assertThat(volume.getId()).isEqualTo(VOLUME_ID);
        assertThat(volume.getType()).isEqualTo(TYPE_GP2);
        assertThat(volume.getSize()).isEqualTo(VOLUME_SIZE);
        assertThat(volume.getDevice()).isEqualTo(expectedDevice);
    }

    private void verifyCreateVolumeRequest(String expectedSnapshotId, Boolean expectedEncrypted, String expectedKmsKeyId) {
        verify(amazonEC2Client).createVolume(createVolumeRequestCaptor.capture());
        CreateVolumeRequest createVolumeRequest = createVolumeRequestCaptor.getValue();
        assertThat(createVolumeRequest).isNotNull();
        assertThat(createVolumeRequest.availabilityZone()).isEqualTo(AVAILABILITY_ZONE);
        assertThat(createVolumeRequest.size()).isEqualTo(VOLUME_SIZE);
        assertThat(createVolumeRequest.snapshotId()).isEqualTo(expectedSnapshotId);
        assertThat(createVolumeRequest.volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(createVolumeRequest.encrypted()).isEqualTo(expectedEncrypted);
        assertThat(createVolumeRequest.kmsKeyId()).isEqualTo(expectedKmsKeyId);

        List<TagSpecification> tagSpecifications = createVolumeRequest.tagSpecifications();
        assertThat(tagSpecifications).isNotNull();
        assertThat(tagSpecifications).hasSize(1);
        TagSpecification tagSpecification = tagSpecifications.getFirst();
        assertThat(tagSpecification).isNotNull();
        assertThat(tagSpecification.resourceType()).isEqualTo(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME);
        assertThat(tagSpecification.tags()).isEqualTo(EC2_TAGS);
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

    private CreateVolumeRequest createVolumeRequest(VolumeSetAttributes.Volume volume, String encryptionKey, boolean encryptedVolume) {
        return CreateVolumeRequest.builder()
                .availabilityZone(AVAILABILITY_ZONE)
                .size(volume.getSize())
                .snapshotId(null)
                .tagSpecifications(TAG_SPECIFICATION)
                .volumeType(volume.getType())
                .encrypted(encryptedVolume)
                .kmsKeyId(encryptionKey)
                .build();
    }

    @Test
    void buildTestWhenMultipleAttachedVolumesPreservesOrderWithoutEphemeral() throws Exception {
        // two attached volumes in a specific order
        VolumeSetAttributes.Volume vol1 = createVolumeForVolumeSet(TYPE_GP2);
        VolumeSetAttributes.Volume vol2 = createVolumeForVolumeSet(TYPE_STANDARD);
        CloudResource cloudResource = createVolumeSet(List.of(vol1, vol2));

        // group with no ephemeral volumes -> device names start at /dev/xvdb
        Group group = createGroup(List.of(createVolume(TYPE_GP2), createVolume(TYPE_STANDARD)),
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        // run async tasks inline and stub AWS calls
        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol1), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol1, null, false));
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol2), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol2, null, false));

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(cloudResource), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        assertThat(volumes).hasSize(2);
        // order must match input order: vol1 then vol2
        assertThat(volumes.getFirst().getType()).isEqualTo(TYPE_GP2);
        assertThat(volumes.get(1).getType()).isEqualTo(TYPE_STANDARD);
        // device names must be deterministic without ephemeral offset
        assertThat(volumes.getFirst().getDevice()).isEqualTo("/dev/xvdb");
        assertThat(volumes.get(1).getDevice()).isEqualTo("/dev/xvdc");
    }

    @Test
    void buildTestWhenMultipleAttachedVolumesPreservesOrderWithEphemeralOffset() throws Exception {
        // two attached volumes in a specific order
        VolumeSetAttributes.Volume vol1 = createVolumeForVolumeSet(TYPE_GP2);
        VolumeSetAttributes.Volume vol2 = createVolumeForVolumeSet(TYPE_STANDARD);
        CloudResource cloudResource = createVolumeSet(List.of(vol1, vol2));

        // group with one ephemeral volume -> device names start at /dev/xvdc
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL), createVolume(TYPE_GP2)),
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 1L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol1), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol1, null, false));
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol2), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol2, null, false));

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(cloudResource), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        assertThat(volumes).hasSize(2);
        // ordering preserved
        assertThat(volumes.getFirst().getType()).isEqualTo(TYPE_GP2);
        assertThat(volumes.get(1).getType()).isEqualTo(TYPE_STANDARD);
        // device names deterministically offset by ephemeral count (1)
        assertThat(volumes.getFirst().getDevice()).isEqualTo("/dev/xvdc");
        assertThat(volumes.get(1).getDevice()).isEqualTo("/dev/xvdd");
    }

    @Test
    void buildTestOrderIsPreservedAcrossRepeatedBuildCalls() throws Exception {
        // prepare two volumes
        VolumeSetAttributes.Volume vol1 = createVolumeForVolumeSet(TYPE_GP2);
        VolumeSetAttributes.Volume vol2 = createVolumeForVolumeSet(TYPE_STANDARD);
        CloudResource requested = createVolumeSet(List.of(vol1, vol2));
        Group group = createGroup(List.of(createVolume(TYPE_GP2), createVolume(TYPE_STANDARD)),
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol1), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol1, null, false));
        when(awsCommonDiskUtilService.createVolumeRequest(eq(vol2), eq(TAG_SPECIFICATION), eq(null), eq(false), eq(AVAILABILITY_ZONE)))
                .thenReturn(createVolumeRequest(vol2, null, false));

        // first build creates volumes and sets order
        List<CloudResource> first = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(requested), cloudStack);
        List<VolumeSetAttributes.Volume> firstVolumes = verifyResultAndGetVolumes(first);
        assertThat(firstVolumes).extracting(VolumeSetAttributes.Volume::getDevice)
                .containsExactly("/dev/xvdb", "/dev/xvdc");

        // second build with the previously created resource should keep the same ordering
        CloudResource created = first.getFirst();
        List<CloudResource> second = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(created), cloudStack);
        // build should return the same created resource without altering volume order
        List<VolumeSetAttributes.Volume> secondVolumes = second.getFirst().getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes();
        assertThat(secondVolumes).extracting(VolumeSetAttributes.Volume::getDevice)
                .containsExactly("/dev/xvdb", "/dev/xvdc");
        assertThat(secondVolumes).extracting(VolumeSetAttributes.Volume::getType)
                .containsExactly(TYPE_GP2, TYPE_STANDARD);
    }
}
