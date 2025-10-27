package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.AzureAvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidatorAndUpdater;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.multiaz.ProviderBasedMultiAzSetupValidator;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class VerticalScalingValidatorServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:default:cluster:b30acd9c-ef27-4ef5-9adf-205e87bd61f6";

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformParameters platformParameters;

    @Mock
    private ProviderBasedMultiAzSetupValidator providerBasedMultiAzSetupValidator;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private VerticalScalingValidatorService underTest;

    @Mock
    private Stack stack;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateValidatorAndUpdater templateValidatorAndUpdater;

    @Test
    public void testRequestValidateInstanceTypeForDeleteVolumesSuccess() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(1);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest);
    }

    @Test
    public void testInstanceTypeForDeleteVolumesBadRequestNoEphemeralVolume() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest));
        assertEquals("Deleting disks is only supported on instances with instance storage", badRequestException.getMessage());
    }

    @Test
    public void testInstanceTypeForDeleteVolumesBadRequestInvalidInstanceGroup() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest));
        assertEquals("Define a group which exists in Cluster. It can be [master1].", badRequestException.getMessage());
    }

    private InstanceGroup instanceGroup(String name, String instanceType, Template template) {
        AvailabilityZone az1 = new AvailabilityZone();
        az1.setAvailabilityZone("1");
        AvailabilityZone az2 = new AvailabilityZone();
        az2.setAvailabilityZone("2");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        instanceGroup.setAvailabilityZones(Set.of(az1, az2));
        return instanceGroup;
    }

    @Test
    public void testValidateIfInstanceAvailableForDhVerticalScaling() {
        String requestedInstanceType = "m7.4xlarge";
        String cloudPlatform = "AWS";
        String cloudPlatformVariant = "AWS";
        when(cloudPlatformConnectors.get(platform(cloudPlatform), Variant.variant(cloudPlatformVariant))).thenReturn(cloudConnector);
        lenient().when(cloudConnector.parameters()).thenReturn(platformParameters);
        when(platformParameters.getDistroxEnabledInstanceTypes(Architecture.ARM64)).thenReturn(Set.of("m7.xlarge"));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateIfInstanceAvailable(requestedInstanceType, Architecture.ARM64, cloudPlatformVariant, cloudPlatform);
        });
        assertEquals("The requested instancetype: m7.4xlarge is not enabled for vertical scaling.", badRequestException.getMessage());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzSuccess() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInRequest, instanceTypeNameInStack, template)));
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        Credential credential = credential();
        ExtendedCloudCredential cloudCredential = extendedCloudCredential();
        when(credentialClientService.getByEnvironmentCrn(eq("crn"))).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(eq(credential))).thenReturn(cloudCredential);
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                        List.of("1", "2", "3")
                )
        );
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        when(instanceGroupService.findAvailabilityZonesByStackIdAndGroupId(any())).thenReturn(Set.of("1", "2"));
        when(providerBasedMultiAzSetupValidator.getAvailabilityZoneConnector(any())).thenReturn(new AzureAvailabilityZoneConnector());

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceType(stack, stackVerticalScaleV4Request);

        verify(instanceGroupService, times(1)).findAvailabilityZonesByStackIdAndGroupId(any());
        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), eq(Map.of("architecture", "ALL")));
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), eq(Set.of("1", "2")), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzStackNotMultiAz() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInRequest, instanceTypeNameInStack, template)));
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        Credential credential = credential();
        ExtendedCloudCredential cloudCredential = extendedCloudCredential();
        when(credentialClientService.getByEnvironmentCrn(eq("crn"))).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(eq(credential))).thenReturn(cloudCredential);
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                        List.of("1", "2", "3")
                )
        );
        when(stack.isMultiAz()).thenReturn(false);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceType(stack, stackVerticalScaleV4Request);

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzConnectorNotImplemented() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInRequest, instanceTypeNameInStack, template)));
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        Credential credential = credential();
        ExtendedCloudCredential cloudCredential = extendedCloudCredential();
        when(credentialClientService.getByEnvironmentCrn(eq("crn"))).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(eq(credential))).thenReturn(cloudCredential);
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                        List.of("1", "2", "3")
                )
        );
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        when(providerBasedMultiAzSetupValidator.getAvailabilityZoneConnector(any())).thenReturn(null);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceType(stack, stackVerticalScaleV4Request);

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzInstanceGroupNotPresent() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInRequest = "m3.xlarge";

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceType(stack, stackVerticalScaleV4Request);
        });

        assertEquals("Define a group which exists in Cluster. It can be [].", badRequestException.getMessage());

        verify(credentialClientService, times(0)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(0)).convert(any());
        verify(cloudParameterService, times(0)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());
    }

    @ParameterizedTest()
    @EnumSource(CloudPlatform.class)
    public void testValidateEntitlementForDelete(CloudPlatform cloudPlatform) {
        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        if (cloudPlatform == CloudPlatform.AZURE) {
            when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
            when(entitlementService.azureDeleteDiskEnabled("default")).thenReturn(true);
        }
        underTest.validateEntitlementForDelete(stack);
    }

    @Test
    public void testValidateEntitlementForDeleteThrowsException() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(entitlementService.azureDeleteDiskEnabled("default")).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateEntitlementForDelete(stack);
        });
        assertEquals("Deleting Disk for Azure is not enabled for this account", badRequestException.getMessage());
    }

    @ParameterizedTest()
    @EnumSource(CloudPlatform.class)
    public void testValidateEntitlementForAddDisk(CloudPlatform cloudPlatform) {
        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        if (cloudPlatform == CloudPlatform.AZURE) {
            when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
            when(entitlementService.azureAddDiskEnabled("default")).thenReturn(true);
        }
        underTest.validateEntitlementForAddVolumes(stack);
    }

    @Test
    public void testValidateEntitlementForAddDiskThrowsException() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(entitlementService.azureAddDiskEnabled("default")).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateEntitlementForAddVolumes(stack);
        });
        assertEquals("Adding Disk for Azure is not enabled for this account", badRequestException.getMessage());
    }

    @Test
    public void testValidateAddVolumesRequestForDiskUpdate() {
        String instanceGroupName = "compute";
        String volumeType = "gp3";
        int newSize = 200;

        // Setup instance group with volume templates
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType("gp2");
        volumeTemplate.setVolumeSize(100);
        volumeTemplate.setVolumeCount(2);
        volumeTemplate.setUsageType(VolumeUsageType.GENERAL);
        template.setVolumeTemplates(Set.of(volumeTemplate));

        InstanceGroup instanceGroup = instanceGroup(instanceGroupName, "m5.xlarge", template);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(stack.getEnvironmentCrn()).thenReturn("crn");

        Credential credential = credential();
        when(credentialClientService.getByEnvironmentCrn(eq("crn"))).thenReturn(credential);

        // Setup cloud connector and disk types
        when(cloudPlatformConnectors.get(platform("AWS"), Variant.variant("AWS"))).thenReturn(cloudConnector);
        when(cloudConnector.parameters()).thenReturn(platformParameters);

        DiskTypes diskTypes = new DiskTypes(
                List.of(com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("gp2"), com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("gp3")),
                com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("gp2"),
                Map.of("gp2", VolumeParameterType.AUTO_ATTACHED, "gp3", VolumeParameterType.AUTO_ATTACHED),
                Map.of()
        );
        when(platformParameters.diskTypes()).thenReturn(diskTypes);

        // Create disk update event
        DistroXDiskUpdateEvent diskUpdateEvent = DistroXDiskUpdateEvent.builder()
                .withResourceId(1L)
                .withGroup(instanceGroupName)
                .withDiskType(DiskType.ADDITIONAL_DISK.name())
                .withSize(newSize)
                .withVolumeType(volumeType)
                .build();

        List<Volume> volumesToBeUpdated = List.of(
                new Volume("/dev/xvdb", "gp2", 100, null)
        );

        ValidationResult result = underTest.validateAddVolumesRequest(stack, volumesToBeUpdated, diskUpdateEvent);

        assertFalse(result.hasError());

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(templateValidatorAndUpdater, times(1)).validateGroupForVerticalScale(
                any(Credential.class),
                eq(instanceGroup),
                eq(stack),
                eq(CdpResourceType.DATAHUB),
                any()
        );
    }

    @Test
    public void testValidateAddVolumesRequestForDatabaseDiskUpdate() {
        String instanceGroupName = "master";
        String volumeType = "Premium_LRS";
        int newSize = 1000;

        // Setup instance group with database volume template
        Template template = new Template();
        VolumeTemplate databaseVolumeTemplate = new VolumeTemplate();
        databaseVolumeTemplate.setVolumeType("Standard_LRS");
        databaseVolumeTemplate.setVolumeSize(500);
        databaseVolumeTemplate.setVolumeCount(1);
        databaseVolumeTemplate.setUsageType(VolumeUsageType.DATABASE);
        template.setVolumeTemplates(Set.of(databaseVolumeTemplate));

        InstanceGroup instanceGroup = instanceGroup(instanceGroupName, "Standard_D4s_v3", template);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        when(stack.getEnvironmentCrn()).thenReturn("crn");

        Credential credential = credential();
        when(credentialClientService.getByEnvironmentCrn(eq("crn"))).thenReturn(credential);

        // Setup cloud connector and disk types
        when(cloudPlatformConnectors.get(platform("AZURE"), Variant.variant("AZURE"))).thenReturn(cloudConnector);
        when(cloudConnector.parameters()).thenReturn(platformParameters);

        DiskTypes diskTypes = new DiskTypes(
                List.of(com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("Standard_LRS"),
                        com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("Premium_LRS")),
                com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType("Standard_LRS"),
                Map.of("Standard_LRS", VolumeParameterType.AUTO_ATTACHED, "Premium_LRS", VolumeParameterType.AUTO_ATTACHED),
                Map.of()
        );
        when(platformParameters.diskTypes()).thenReturn(diskTypes);

        // Create disk update event for DATABASE_DISK
        DistroXDiskUpdateEvent diskUpdateEvent = DistroXDiskUpdateEvent.builder()
                .withResourceId(1L)
                .withGroup(instanceGroupName)
                .withDiskType(DiskType.DATABASE_DISK.name())
                .withSize(newSize)
                .withVolumeType(volumeType)
                .build();

        List<Volume> volumesToBeUpdated = List.of(
                new Volume("/dev/sdc", "Standard_LRS", 500, null)
        );

        ValidationResult result = underTest.validateAddVolumesRequest(stack, volumesToBeUpdated, diskUpdateEvent);

        assertFalse(result.hasError());

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(templateValidatorAndUpdater, times(1)).validateGroupForVerticalScale(
                any(Credential.class),
                eq(instanceGroup),
                eq(stack),
                eq(CdpResourceType.DATAHUB),
                any()
        );
    }

    private Credential credential() {
        return Credential.builder().crn("crn").name("name").cloudPlatform("aws").account("accountId").attributes(new Json("")).build();
    }

    private ExtendedCloudCredential extendedCloudCredential() {
        return new ExtendedCloudCredential(
                new CloudCredential(),
                "aws",
                "",
                "accountId",
                List.of());
    }

    private CloudVmTypes cloudVmTypes(String zone, VmType... vmTypes) {
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> map = new HashMap<>();
        map.put(zone, Arrays.stream(vmTypes).collect(Collectors.toSet()));
        cloudVmTypes.setCloudVmResponses(map);
        return cloudVmTypes;
    }

    public VmType vmType(String name, int memory, int cpu, VolumeParameterConfig autoAttached, VolumeParameterConfig ephemeral, List<String> availabilityZones) {
        return vmTypeWithMeta(name,
                VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withAutoAttachedConfig(autoAttached)
                        .withCpuAndMemory(cpu, memory)
                        .withEphemeralConfig(ephemeral)
                        .withAvailabilityZones(availabilityZones)
                        .create(),
                false);
    }
}
