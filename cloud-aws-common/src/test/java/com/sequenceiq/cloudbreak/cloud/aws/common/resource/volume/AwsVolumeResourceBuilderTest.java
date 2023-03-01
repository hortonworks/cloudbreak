package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
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
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
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

@ExtendWith({MockitoExtension.class})
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

    private static final String VOLUME_SET_NAME = "volumeName";

    private static final String VOLUME_ID = "volumeId";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final Map<String, String> TAGS = Map.ofEntries(entry("key1", "value1"), entry("key2", "value2"));

    private static final Collection<Tag> EC2_TAGS = List.of(Tag.builder().key("ec2_key").value("ec2_value").build());

    private static final String SNAPSHOT_ID = "snapshotId";

    private static final String ENCRYPTION_KEY_ARN = "encryptionKeyArn";

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private CommonAwsClient awsClient;

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

    @Captor
    private ArgumentCaptor<CreateVolumeRequest> createVolumeRequestCaptor;

    @Captor
    private ArgumentCaptor<ModifyInstanceAttributeRequest> modifyInstanceAttributeRequestCaptor;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));
        when(region.value()).thenReturn(REGION_NAME);
        when(awsClient.createEc2Client(isA(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(amazonEC2Client);
        when(cloudStack.getTags()).thenReturn(TAGS);
        when(awsTaggingService.prepareEc2Tags(TAGS)).thenReturn(EC2_TAGS);
    }

    @Test
    void buildTestWhenNoVolumesAtAll() throws Exception {
        Group group = createGroup(emptyList(), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group, emptyList(), cloudStack);

        assertThat(result).isEmpty();
    }

    @Test
    void buildTestWhenEphemeralVolumesOnly() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(emptyList())), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        assertThat(volumes).isEmpty();
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndNoEncryption() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenEphemeralAndAttachedVolumesAndNoEncryption() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL), createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 1L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdc");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndIneffectiveEncryptionWithDefaultKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())), 0L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, false, null);
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndEffectiveEncryptionWithDefaultKey() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)),
                Map.ofEntries(entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true),
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())), 0L);

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

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

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

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

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, true, ENCRYPTION_KEY_ARN);
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
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(0L, authenticatedContext, group, List.of(instanceResource));
        assertThat(actual.isEmpty()).isTrue();
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
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(0L, authenticatedContext, group, List.of(instanceResource));
        assertThat(actual.isEmpty()).isFalse();
        assertThat(actual.get()).isEqualTo(volumeSet);
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
        Optional<CloudResource> actual = underTest.fetchCloudResourceFromDBIfAvailable(0L, authenticatedContext, group, List.of(instanceResource));
        assertThat(actual.isEmpty()).isFalse();
        assertThat(actual.get()).isEqualTo(volumeSet);
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

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().get(0).ebs().deleteOnTermination());
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

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().get(0).ebs().deleteOnTermination());
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

        assertTrue(modifyInstanceAttributeRequest.blockDeviceMappings().get(0).ebs().deleteOnTermination());
    }

    @Test()
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteTurnOffDeleteOntermination() throws PreserveResourceException {
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

        assertTrue(!modifyInstanceAttributeRequest.blockDeviceMappings().get(0).ebs().deleteOnTermination());
    }

    @Test()
    @MockitoSettings(strictness = Strictness.LENIENT)
    void skipModifyInstanceAttributeDuringDeleteWhenNoAttachedResourcesFound() throws PreserveResourceException {
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
        return new Group(GROUP_NAME, InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null,
                null, ROOT_VOLUME_SIZE, null, createGroupNetwork(), emptyMap());
    }

    private VolumeSetAttributes.Volume createVolumeForVolumeSet(String type) {
        return new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, type, CloudVolumeUsageType.GENERAL);
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
        CloudResource cloudResource = result.get(0);
        assertThat(cloudResource.getStatus()).isEqualTo(CommonStatus.CREATED);
        VolumeSetAttributes volumeSet = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        assertThat(volumeSet).isNotNull();
        List<VolumeSetAttributes.Volume> volumes = volumeSet.getVolumes();
        assertThat(volumes).isNotNull();
        return volumes;
    }

    private void verifyVolumes(List<VolumeSetAttributes.Volume> volumes, String expectedDevice) {
        assertThat(volumes).hasSize(1);
        VolumeSetAttributes.Volume volume = volumes.get(0);
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
        TagSpecification tagSpecification = tagSpecifications.get(0);
        assertThat(tagSpecification).isNotNull();
        assertThat(tagSpecification.resourceType()).isEqualTo(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME);
        assertThat(tagSpecification.tags()).isEqualTo(EC2_TAGS);
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

}
