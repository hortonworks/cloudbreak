package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.SecurityRuleService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;

public class StackToCloudStackConverterTest {

    private static final Long TEST_STACK_ID = 1L;

    private static final Integer VOLUME_COUNT = 0;

    private static final String INSTANCE_ID = "instance-id";

    private static final String GROUP_NAME = "group-name";

    private static final String IMAGE_NAME = "image-name";

    private static final String ENV_CRN = "env-crn";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private StackToCloudStackConverter underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private SecurityRuleService securityRuleService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    public void testBuildInstance() throws Exception {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        ImageEntity imageEntity = mock(ImageEntity.class);
        when(imageService.getByStack(any())).thenReturn(imageEntity);
        when(imageEntity.getImageName()).thenReturn(IMAGE_NAME);
        when(instanceMetaData.getInstanceId()).thenReturn(INSTANCE_ID);
        when(instanceGroup.getGroupName()).thenReturn(GROUP_NAME);
        Template template = mock(Template.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(template.getVolumeCount()).thenReturn(VOLUME_COUNT);
        CloudInstance cloudInstance = underTest.buildInstance(stack, instanceMetaData, instanceGroup, stackAuthentication, 0L, InstanceStatus.CREATED);
        assertEquals(INSTANCE_ID, cloudInstance.getInstanceId());
    }

    @Test
    public void testUpdateWithVerticalScaleRequest() throws Exception {
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

        CloudStack cloudStack = new CloudStack(
                Set.of(group),
                null,
                null,
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );


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
                .iterator()
                .next()
                .getInstances()
                .iterator()
                .next()
                .getTemplate()
                .getFlavor();

        assertEquals("ec2big", resultFlavor);
        assertEquals(100, result.getGroups().get(0).getRootVolumeSize());
    }

    @Test
    public void testBuildFileSystemViewDifferentAWSInstanceProfile() throws Exception {
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

        expectedException.expect(BadRequestException.class);
        underTest.buildFileSystemView(stack);
    }

    @Test
    public void testBuildFileSystemViewDifferentAzureManagedIdentity() throws Exception {
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

        expectedException.expect(BadRequestException.class);
        underTest.buildFileSystemView(stack);
    }

    @Test
    public void testBuildFileSystemViewDifferentGCPEmail() throws Exception {
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

        expectedException.expect(BadRequestException.class);
        underTest.buildFileSystemView(stack);
    }

    @Test
    public void testBuildFileSystemViewSameAWSInstanceProfile() throws Exception {
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
    public void testBuildFileSystemViewSameAzureManagedIdentity() throws Exception {
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
    public void testBuildFileSystemViewSameGCPEmail() throws Exception {
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
