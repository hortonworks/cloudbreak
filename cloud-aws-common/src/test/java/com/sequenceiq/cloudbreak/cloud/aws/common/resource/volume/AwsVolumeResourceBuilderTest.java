package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
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
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
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

    private static final Collection<Tag> EC2_TAGS = List.of(new Tag("ec2_key", "ec2_value"));

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

    @Captor
    private ArgumentCaptor<CreateVolumeRequest> createVolumeRequestCaptor;

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
        Group group = createGroup(emptyList(), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false));

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group, emptyList(), cloudStack);

        assertThat(result).isEmpty();
    }

    @Test
    void buildTestWhenEphemeralVolumesOnly() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false));

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(emptyList())), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        assertThat(volumes).isEmpty();
    }

    @Test
    void buildTestWhenAttachedVolumesOnlyAndNoEncryption() throws Exception {
        Group group = createGroup(List.of(createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false));

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
        Group group = createGroup(List.of(createVolume(TYPE_EPHEMERAL), createVolume(TYPE_GP2)), Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false));

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
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())));

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
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name())));

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
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN)));

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
                        entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN)));

        setUpTaskExecutors();
        when(amazonEC2Client.createVolume(isA(CreateVolumeRequest.class))).thenReturn(createCreateVolumeResult());

        List<CloudResource> result = underTest.build(awsContext, cloudInstance, PRIVATE_ID, authenticatedContext, group,
                List.of(createVolumeSet(List.of(createVolumeForVolumeSet(TYPE_GP2)))), cloudStack);

        List<VolumeSetAttributes.Volume> volumes = verifyResultAndGetVolumes(result);
        verifyVolumes(volumes, "/dev/xvdb");
        verifyCreateVolumeRequest(null, true, ENCRYPTION_KEY_ARN);
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

    private Group createGroup(List<Volume> volumes, Map<String, Object> templateParameters) {
        InstanceTemplate template = new InstanceTemplate(FLAVOR, GROUP_NAME, PRIVATE_ID, volumes, InstanceStatus.CREATE_REQUESTED, templateParameters,
                TEMPLATE_ID, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance instance = new CloudInstance(INSTANCE_ID, template, null, "subnet-1", "az1");
        return new Group(GROUP_NAME, InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null,
                null, ROOT_VOLUME_SIZE, null, createGroupNetwork());
    }

    private VolumeSetAttributes.Volume createVolumeForVolumeSet(String type) {
        return new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, type, CloudVolumeUsageType.GENERAL);
    }

    private CloudResource createVolumeSet(List<VolumeSetAttributes.Volume> volumes) {
        return CloudResource.builder()
                .type(ResourceType.AWS_VOLUMESET)
                .name(VOLUME_SET_NAME)
                .status(CommonStatus.REQUESTED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                        .withAvailabilityZone(AVAILABILITY_ZONE)
                        .withVolumes(volumes)
                        .build()))
                .build();
    }

    private CreateVolumeResult createCreateVolumeResult() {
        return new CreateVolumeResult().withVolume(new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID));
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
        assertThat(createVolumeRequest.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
        assertThat(createVolumeRequest.getSize()).isEqualTo(VOLUME_SIZE);
        assertThat(createVolumeRequest.getSnapshotId()).isEqualTo(expectedSnapshotId);
        assertThat(createVolumeRequest.getVolumeType()).isEqualTo(TYPE_GP2);
        assertThat(createVolumeRequest.getEncrypted()).isEqualTo(expectedEncrypted);
        assertThat(createVolumeRequest.getKmsKeyId()).isEqualTo(expectedKmsKeyId);

        List<TagSpecification> tagSpecifications = createVolumeRequest.getTagSpecifications();
        assertThat(tagSpecifications).isNotNull();
        assertThat(tagSpecifications).hasSize(1);
        TagSpecification tagSpecification = tagSpecifications.get(0);
        assertThat(tagSpecification).isNotNull();
        assertThat(tagSpecification.getResourceType()).isEqualTo(com.amazonaws.services.ec2.model.ResourceType.Volume.toString());
        assertThat(tagSpecification.getTags()).isEqualTo(EC2_TAGS);
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

}