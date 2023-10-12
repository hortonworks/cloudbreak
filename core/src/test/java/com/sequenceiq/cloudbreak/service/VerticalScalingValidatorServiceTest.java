package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureAvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;

@ExtendWith(MockitoExtension.class)
public class VerticalScalingValidatorServiceTest {

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

    @InjectMocks
    private VerticalScalingValidatorService underTest;

    @Mock
    private Stack stack;

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
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        CloudConnector connector = mock(CloudConnector.class);
        when(connector.availabilityZoneConnector()).thenReturn(new AzureAvailabilityZoneConnector());
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzStackNotMultiAz() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInRequest = "m3.xlarge";
        String instanceTypeNameInStack = "m3.xlarge";
        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInRequest, instanceTypeNameInStack, template)));
        when(stack.isMultiAz()).thenReturn(false);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);

        verify(credentialClientService, times(0)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(0)).convert(any());
        verify(cloudParameterService, times(0)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzConnectorNotImplemented() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInRequest, instanceTypeNameInStack, template)));
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stack.getPlatformVariant()).thenReturn("AWS");
        CloudConnector connector = mock(CloudConnector.class);
        when(connector.availabilityZoneConnector()).thenReturn(null);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInRequest);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);

        verify(credentialClientService, times(0)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(0)).convert(any());
        verify(cloudParameterService, times(0)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzException() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "Standard_D16d_v4";

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
                        List.of("1")
                )
        );
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        CloudConnector connector = mock(CloudConnector.class);
        when(connector.availabilityZoneConnector()).thenReturn(new AzureAvailabilityZoneConnector());
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInStack);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);
        });

        assertEquals("Stack is MultiAz enabled but requested instance type is not supported in existing Availability Zones for Instance Group. " +
                        "Supported Availability Zones for Instance type Standard_D16d_v4 : 1. Existing Availability Zones for Instance Group : 1,2",
                badRequestException.getMessage());

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testValidateInstanceTypeForMultiAzNoAzForRequestedInstanceException() {
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "Standard_D16d_v4";

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
                        null
                )
        );
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        CloudConnector connector = mock(CloudConnector.class);
        when(connector.availabilityZoneConnector()).thenReturn(new AzureAvailabilityZoneConnector());
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);

        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(instanceTypeNameInStack);

        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(1L);
        stackVerticalScaleV4Request.setGroup(instanceGroupNameInRequest);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);
        });

        assertEquals("Stack is MultiAz enabled but requested instance type is not supported in existing Availability Zones for Instance Group. " +
                        "Supported Availability Zones for Instance type Standard_D16d_v4 : . Existing Availability Zones for Instance Group : 1,2",
                badRequestException.getMessage());

        verify(credentialClientService, times(1)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
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
            underTest.validateInstanceTypeForMultiAz(stack, stackVerticalScaleV4Request);
        });

        assertEquals("Define a group which exists in Cluster. It can be [].", badRequestException.getMessage());

        verify(credentialClientService, times(0)).getByEnvironmentCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(0)).convert(any());
        verify(cloudParameterService, times(0)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
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
