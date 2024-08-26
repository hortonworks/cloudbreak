package com.sequenceiq.freeipa.converter.cloud;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
public class StackToCloudStackConverterTest {

    private static final String TEST_SUBNET_ID = "subnetId";

    private static final Long TEST_STACK_ID = 1L;

    private static final String TEST_INSTANCE_NAME = "instanceName";

    private static final String TEST_HOST_NAME = "hostName";

    private static final Long TEST_RESOURCE_ID = 1L;

    private static final String TEST_RESOURCE_REFERENCE = "resourceReference";

    private static final String TEST_AZURE_RG_NAME = "azureRgName";

    private static final Integer VOLUME_COUNT = 0;

    private static final String INSTANCE_ID = "instance-id";

    private static final String GROUP_NAME = "group-name";

    private static final String IMAGE_NAME = "image-name";

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private StackToCloudStackConverter underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private ImageService imageService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private ResourceService resourceService;

    @Mock
    private LoadBalancerToCloudLoadBalancerConverter loadBalancerToCloudLoadBalancerConverter;

    @Mock
    private ImageConverter imageConverter;

    @Test
    void testBuildInstance() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        ImageEntity imageEntity = mock(ImageEntity.class);
        when(imageService.getByStack(any())).thenReturn(imageEntity);
        when(imageEntity.getImageName()).thenReturn(IMAGE_NAME);
        when(instanceMetaData.getInstanceId()).thenReturn(INSTANCE_ID);
        when(instanceGroup.getGroupName()).thenReturn(GROUP_NAME);
        Template template = mock(Template.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(template.getVolumeCount()).thenReturn(VOLUME_COUNT);
        Resource resource = mock(Resource.class);
        when(resourceService.findResourceById(anyLong())).thenReturn(Optional.of(resource));
        CloudInstance cloudInstance = underTest.buildInstance(stack, instanceMetaData, instanceGroup, stackAuthentication, 0L, InstanceStatus.CREATED);
        assertEquals(INSTANCE_ID, cloudInstance.getInstanceId());
    }

    @Test
    void testConvert() {
        ImageEntity imageEntity = mock(ImageEntity.class);
        Image image = mock(Image.class);
        when(image.getImageName()).thenReturn(IMAGE_NAME);
        when(imageService.getByStack(stack)).thenReturn(imageEntity);
        when(imageConverter.convert(imageEntity)).thenReturn(image);
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = mock(Template.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        List<String> az = List.of("us-west2-a", "us-west2-b");
        Json attributes = mock(Json.class);
        when(attributes.getMap()).thenReturn(Map.of(NetworkConstants.AVAILABILITY_ZONES, az));
        InstanceGroupNetwork instanceGroupNetwork = mock(InstanceGroupNetwork.class);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        Network network = mock(Network.class);
        when(stack.getNetwork()).thenReturn(network);
        CloudStack cloudStack = underTest.convert(stack);
        assertEquals(new HashSet<>(az), cloudStack.getGroups().get(0).getNetwork().getAvailabilityZones());
    }

    @Test
    void testBuildCloudInstanceParameters() {
        Resource resource = new Resource();
        resource.setId(TEST_RESOURCE_ID);
        resource.setResourceReference(TEST_RESOURCE_REFERENCE);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withAzure(AzureEnvironmentParameters.builder()
                        .withAzureResourceGroup(AzureResourceGroup.builder()
                                .withName(TEST_AZURE_RG_NAME)
                                .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                                .build())
                        .build())
                .build();
        when(instanceMetaData.getSubnetId()).thenReturn(TEST_SUBNET_ID);
        when(instanceMetaData.getId()).thenReturn(TEST_STACK_ID);
        when(instanceMetaData.getInstanceName()).thenReturn(TEST_INSTANCE_NAME);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(TEST_HOST_NAME);
        when(instanceMetaData.getUserdataSecretResourceId()).thenReturn(TEST_RESOURCE_ID);
        when(resourceService.findResourceById(TEST_RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);

        Map<String, Object> result = underTest.buildCloudInstanceParameters(ENV_CRN, instanceMetaData);

        assertEquals(TEST_SUBNET_ID, result.get(SUBNET_ID));
        assertEquals(TEST_STACK_ID, result.get(CloudInstance.ID));
        assertEquals(TEST_INSTANCE_NAME, result.get(CloudInstance.INSTANCE_NAME));
        assertEquals(TEST_HOST_NAME, result.get(CloudInstance.DISCOVERY_NAME));
        assertEquals(TEST_RESOURCE_REFERENCE, result.get(CloudInstance.USERDATA_SECRET_ID));
        assertEquals(TEST_AZURE_RG_NAME, result.get(RESOURCE_GROUP_NAME_PARAMETER));
        assertEquals(ResourceGroupUsage.SINGLE.name(), result.get(RESOURCE_GROUP_USAGE_PARAMETER));
    }

    @Test
    void testUpdateWithVerticalScaleRequest() {
        InstanceTemplate instanceTemplate = new InstanceTemplate(
                null,
                GROUP_NAME,
                1L,
                Collections.emptyList(),
                null,
                Collections.emptyMap(),
                null,
                null,
                TemporaryStorage.ATTACHED_VOLUMES,
                0L);


        CloudInstance cloudInstance = new CloudInstance(
                "i1",
                instanceTemplate,
                new InstanceAuthentication("key", "id", "cb"),
                "subnet",
                "az");

        Group group = new Group(
                GROUP_NAME,
                InstanceGroupType.CORE,
                Set.of(cloudInstance),
                new Security(Set.of(), Set.of()),
                cloudInstance,
                new InstanceAuthentication("publicKey", "publicKeyId", "loginuser"),
                "cb",
                "ssh",
                80,
                Optional.empty(),
                new GroupNetwork(OutboundInternetTraffic.DISABLED, Set.of(), Map.of()),
                Map.of());

        CloudStack cloudStack = CloudStack.builder()
                .groups(Set.of(group))
                .build();

        VerticalScaleRequest freeIPAVerticalScaleRequest = new VerticalScaleRequest();
        freeIPAVerticalScaleRequest.setGroup(GROUP_NAME);
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("ec2big");
        VolumeRequest rootVolume = new VolumeRequest();
        rootVolume.setSize(100);
        instanceTemplateRequest.setRootVolume(rootVolume);
        freeIPAVerticalScaleRequest.setTemplate(instanceTemplateRequest);

        CloudStack result = underTest.updateWithVerticalScaleRequest(cloudStack, freeIPAVerticalScaleRequest);
        String resultFlavor = result.getGroups()
                .getFirst()
                .getInstances()
                .getFirst()
                .getTemplate()
                .getFlavor();

        assertEquals("ec2big", resultFlavor);
        assertEquals(100, result.getGroups().getFirst().getRootVolumeSize());
    }

    @Test
    void testUpdateWithVerticalScaleRequestWithZeroInstanceGroup() {
        InstanceTemplate skeletonTemplate = new InstanceTemplate("small", null, 0L, Set.of(), null, null, 0L, null, null, 0L);
        CloudInstance skeleton = new CloudInstance("skeleton", skeletonTemplate, null, null, null);
        Group group1 = new Group("group1", null, Set.of(), null, skeleton, null, null, null, 100, null, null, Map.of());
        Group group2 = new Group("group2", null, Set.of(), null, null, null, null, null, 100, null, null, Map.of());
        CloudStack cloudStack = CloudStack.builder()
                .groups(Set.of(group1, group2))
                .build();
        VerticalScaleRequest verticalScaleRequest = getVerticalScaleRequest();

        CloudStack result = underTest.updateWithVerticalScaleRequest(cloudStack, verticalScaleRequest);

        assertEquals("very_large", skeletonTemplate.getFlavor());
        assertEquals(200, group1.getRootVolumeSize());
        assertEquals(100, group2.getRootVolumeSize());
    }

    private static VerticalScaleRequest getVerticalScaleRequest() {
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        verticalScaleRequest.setGroup("group1");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("very_large");
        VolumeRequest rootVolumeRequest = new VolumeRequest();
        rootVolumeRequest.setSize(200);
        instanceTemplateRequest.setRootVolume(rootVolumeRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }

    @Test
    void testBuildFileSystemViewDifferentAWSInstanceProfile() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        S3CloudStorageV1Parameters s3Logging = new S3CloudStorageV1Parameters();
        s3Logging.setInstanceProfile("arn:aws:iam::id:instance-profile/role1");
        S3CloudStorageV1Parameters s3Backup = new S3CloudStorageV1Parameters();
        s3Backup.setInstanceProfile("arn:aws:iam::id:instance-profile/role2");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getS3()).thenReturn(s3Backup);
        when(logging.getS3()).thenReturn(s3Logging);

        assertThrows(BadRequestException.class, () -> underTest.buildFileSystemView(stack));
    }

    @Test
    void testBuildFileSystemViewDifferentAzureManagedIdentity() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        AdlsGen2CloudStorageV1Parameters adlsGen2Logging = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2Logging.setManagedIdentity("/subscriptions/id/resourceGroups/rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/identity1");
        AdlsGen2CloudStorageV1Parameters adlsGen2Backup = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2Backup.setManagedIdentity("/subscriptions/id/resourceGroups/rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/identity2");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getAdlsGen2()).thenReturn(adlsGen2Logging);
        when(logging.getAdlsGen2()).thenReturn(adlsGen2Backup);

        assertThrows(BadRequestException.class, () -> underTest.buildFileSystemView(stack));
    }

    @Test
    void testBuildFileSystemViewDifferentGCPEmail() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        GcsCloudStorageV1Parameters gcsLogging = new GcsCloudStorageV1Parameters();
        gcsLogging.setServiceAccountEmail("myaccount1@myprojectid.iam.gserviceaccount.com");
        GcsCloudStorageV1Parameters gcsBackup = new GcsCloudStorageV1Parameters();
        gcsBackup.setServiceAccountEmail("myaccount2@myprojectid.iam.gserviceaccount.com");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getGcs()).thenReturn(gcsBackup);
        when(logging.getGcs()).thenReturn(gcsLogging);

        assertThrows(BadRequestException.class, () -> underTest.buildFileSystemView(stack));
    }

    @Test
    void testBuildFileSystemViewSameAWSInstanceProfile() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        S3CloudStorageV1Parameters s3Logging = new S3CloudStorageV1Parameters();
        s3Logging.setInstanceProfile("arn:aws:iam::id:instance-profile/role");
        S3CloudStorageV1Parameters s3Backup = new S3CloudStorageV1Parameters();
        s3Backup.setInstanceProfile("arn:aws:iam::id:instance-profile/role");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getS3()).thenReturn(s3Backup);
        when(logging.getS3()).thenReturn(s3Logging);

        Optional<CloudFileSystemView> fileSystemView = underTest.buildFileSystemView(stack);
        assertEquals(Optional.empty(), fileSystemView);
    }

    @Test
    void testBuildFileSystemViewSameAzureManagedIdentity() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        AdlsGen2CloudStorageV1Parameters adlsGen2Logging = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2Logging.setManagedIdentity("/subscriptions/id/resourceGroups/rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/identity");
        AdlsGen2CloudStorageV1Parameters adlsGen2Backup = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2Backup.setManagedIdentity("/subscriptions/id/resourceGroups/rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/identity");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getAdlsGen2()).thenReturn(adlsGen2Logging);
        when(logging.getAdlsGen2()).thenReturn(adlsGen2Backup);

        Optional<CloudFileSystemView> fileSystemView = underTest.buildFileSystemView(stack);
        assertEquals(Optional.empty(), fileSystemView);
    }

    @Test
    void testBuildFileSystemViewSameGCPEmail() {
        Telemetry telemetry = mock(Telemetry.class);
        Backup backup = mock(Backup.class);
        Logging logging = mock(Logging.class);
        GcsCloudStorageV1Parameters gcsLogging = new GcsCloudStorageV1Parameters();
        gcsLogging.setServiceAccountEmail("myaccount@myprojectid.iam.gserviceaccount.com");
        GcsCloudStorageV1Parameters gcsBackup = new GcsCloudStorageV1Parameters();
        gcsBackup.setServiceAccountEmail("myaccount@myprojectid.iam.gserviceaccount.com");
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(stack.getBackup()).thenReturn(backup);
        when(backup.getGcs()).thenReturn(gcsBackup);
        when(logging.getGcs()).thenReturn(gcsLogging);

        Optional<CloudFileSystemView> fileSystemView = underTest.buildFileSystemView(stack);
        assertEquals(Optional.empty(), fileSystemView);
    }
}
