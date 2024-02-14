package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.azure.AzureAvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupAvailabilityZoneService;

@ExtendWith(MockitoExtension.class)
public class VerticalScalingValidatorServiceTest {

    private static final String AWS = CloudPlatform.AWS.name();

    private static final String OPENSTACK = CloudPlatform.OPENSTACK.name();

    private static final String AZURE = CloudPlatform.AZURE.name();

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @InjectMocks
    private VerticalScalingValidatorService underTest;

    @Mock
    private Stack stack;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private InstanceGroupAvailabilityZoneService availabilityZoneService;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
    }

    @Test
    public void testRequestWhenPlatformIsNotSupportShouldThrowBadRequest() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        when(stack.getCloudPlatform()).thenReturn(OPENSTACK);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Vertical scaling is not supported on OPENSTACK cloud platform",
                badRequestException.getMessage());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any());
    }

    @Test
    public void testRequestWhenTemplateNotSpecifiedInTheRequestShouldThrowBadRequest() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.isStopped()).thenReturn(true);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Define an exiting instancetype to vertically scale the AWS FreeIpa.",
                badRequestException.getMessage());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any());
    }

    @Test
    public void testRequestWhenTheGroupDoesNotExistInTheRequestShouldThrowBadRequest() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInStack = "master2";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack)));
        when(stack.isStopped()).thenReturn(true);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Define a group which exists in FreeIpa. It can be [master2].",
                badRequestException.getMessage());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any());
    }

    @Test
    public void testRequestWhenTheValidationFailedShouldDropBadRequest() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                ),
                vmType(
                        instanceTypeNameInRequest,
                        0,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                )
        );

        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformvariant()).thenReturn("awsvariant");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack)));
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        doThrow(new BadRequestException("The current instancetype m3.xlarge has more Memory then the requested m2.xlarge."))
                .when(verticalScaleInstanceProvider).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("The current instancetype m3.xlarge has more Memory then the requested m2.xlarge.",
                badRequestException.getMessage());
        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());
    }

    @Test
    public void testRequestWhenTheGroupExistInTheRequestShouldValidateInstanceTypeAndEverythingGoesWell() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                )
        );

        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformvariant()).thenReturn("awsvariant");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack)));
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        underTest.validateRequest(stack, verticalScaleRequest);

        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), isNull(), any());
    }

    @Test
    public void testRequestForMultiAzInstanceTypeSupportsExistingZones() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS, AZURE));
        String instanceGroupNameInStack = "master";
        String instanceGroupNameInRequest = "master";
        String instanceTypeNameInStack = "Standard_D16d_v4";
        String instanceTypeNameInRequest = "Standard_D16d_v4";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
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
        when(stack.getCloudPlatform()).thenReturn(AZURE);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, Set.of("1", "2"))));
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        when(multiAzCalculatorService.getAvailabilityZoneConnector(stack)).thenReturn(new AzureAvailabilityZoneConnector());
        when(availabilityZoneService.findAllByInstanceGroupId(anyLong())).thenReturn(Set.of("1", "2").stream().map(s -> {
            InstanceGroupAvailabilityZone availabilityZone = new InstanceGroupAvailabilityZone();
            availabilityZone.setAvailabilityZone(s);
            return availabilityZone;
        }).collect(Collectors.toSet()));
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        underTest.validateRequest(stack, verticalScaleRequest);

        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), eq(Set.of("1", "2")), any());
    }

    @Test
    public void testRequestForMultiAzInstanceTypeDoesNotSupportExistingZones() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS, AZURE));
        String instanceGroupNameInStack = "master";
        String instanceGroupNameInRequest = "master";
        String instanceTypeNameInStack = "Standard_D16d_v4";
        String instanceTypeNameInRequest = "Standard_D16d_v4";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "eu1",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                        List.of("1", "2")
                )
        );
        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getCloudPlatform()).thenReturn(AZURE);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, Set.of("1", "2", "3"))));
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        when(multiAzCalculatorService.getAvailabilityZoneConnector(stack)).thenReturn(new AzureAvailabilityZoneConnector());
        when(availabilityZoneService.findAllByInstanceGroupId(anyLong())).thenReturn(Set.of("1", "2", "3").stream().map(s -> {
            InstanceGroupAvailabilityZone availabilityZone = new InstanceGroupAvailabilityZone();
            availabilityZone.setAvailabilityZone(s);
            return availabilityZone;
        }).collect(Collectors.toSet()));
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);
        doThrow(new BadRequestException("Stack is MultiAz enabled but requested instance type is not supported in existing " +
                "Availability Zones for Instance Group. Supported Availability Zones for Instance type Standard_D16d_v4 : 1,2." +
                "Existing Availability Zones for Instance Group : 1,2,3"))
                .when(verticalScaleInstanceProvider).validateInstanceTypeForVerticalScaling(any(), any(), isNotNull(), any());

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Stack is MultiAz enabled but requested instance type is not supported in existing " +
                        "Availability Zones for Instance Group. Supported Availability Zones for Instance type Standard_D16d_v4 : 1,2." +
                        "Existing Availability Zones for Instance Group : 1,2,3",
                badRequestException.getMessage());

        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), eq(Set.of("1", "2", "3")), any());
    }

    @Test
    public void testRequestWhenTheClusterDidNotStoppedShouldDropException() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInRequest = "m3.xlarge";

        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.isStopped()).thenReturn(false);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("You must stop FreeIPA to be able to vertically scale it.",
                badRequestException.getMessage());
    }

    private CloudVmTypes cloudVmTypes(String zone, VmType... vmTypes) {
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> map = new HashMap<>();
        map.put(zone, Arrays.stream(vmTypes).collect(Collectors.toSet()));
        cloudVmTypes.setCloudVmResponses(map);
        return cloudVmTypes;
    }

    public VmType vmType(String name, int memory, int cpu, VolumeParameterConfig autoAttached, VolumeParameterConfig ephemeral) {
        return vmType(name, memory, cpu, autoAttached, ephemeral, List.of());
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

    private InstanceGroup instanceGroup(String name, String instanceType) {
        return instanceGroup(name, instanceType, Set.of());
    }

    private InstanceGroup instanceGroup(String name, String instanceType, Set<String> availabilityZones) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        instanceGroup.setId(1L);
        Template template = new Template();
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        Set<InstanceGroupAvailabilityZone> instanceGroupAvailabilityZones = availabilityZones.stream().map(s -> {
            InstanceGroupAvailabilityZone instanceGroupAvailabilityZone = new InstanceGroupAvailabilityZone();
            instanceGroupAvailabilityZone.setAvailabilityZone(s);
            instanceGroupAvailabilityZone.setInstanceGroup(instanceGroup);
            return instanceGroupAvailabilityZone;
        }).collect(Collectors.toSet());
        instanceGroup.setAvailabilityZones(instanceGroupAvailabilityZones);
        return instanceGroup;
    }

    private Credential credential() {
        return new Credential(
                "aws",
                "name",
                "",
                "crn",
                "acountId");
    }

    private ExtendedCloudCredential extendedCloudCredential() {
        return new ExtendedCloudCredential(
                new CloudCredential(),
                "aws",
                "",
                "accountId",
                List.of());
    }
}