package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
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
        when(stack.isStopped()).thenReturn(true);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Vertical scaling is not supported on OPENSTACK cloud platform",
                badRequestException.getMessage());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any(), any());
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

        assertEquals("Define an existing instance type to vertically scale the AWS FreeIPA 'null'.",
                badRequestException.getMessage());
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any(), any());
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
        verify(verticalScaleInstanceProvider, never()).validateInstanceTypeForVerticalScaling(any(), any(), any(), any(), any());
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
                .when(verticalScaleInstanceProvider).validateInstanceTypeForVerticalScaling(anyString(),
                        any(List.class), any(List.class), isNull(), any(), any());

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
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), eq(Map.of("architecture", "ALL")));
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, times(1))
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class), isNull(), any(), any());
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
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), eq(Map.of("architecture", "ALL")));
        verify(multiAzCalculatorService, times(0)).getAvailabilityZoneConnector(stack);
        verify(verticalScaleInstanceProvider, times(1))
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class), isNull(), anyMap(), any());
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
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), eq(Map.of("architecture", "ALL")));
        verify(verticalScaleInstanceProvider, times(1)).validateInstanceTypeForVerticalScaling(any(), any(), any(), eq(Set.of("1", "2")), any(), any());
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
                .when(verticalScaleInstanceProvider).validateInstanceTypeForVerticalScaling(anyString(),
                        any(List.class), any(List.class),
                        eq(Set.of("1", "2", "3")), anyMap(), any(CdpResourceType.class));

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Stack is MultiAz enabled but requested instance type is not supported in existing " +
                        "Availability Zones for Instance Group. Supported Availability Zones for Instance type Standard_D16d_v4 : 1,2." +
                        "Existing Availability Zones for Instance Group : 1,2,3",
                badRequestException.getMessage());

        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), eq(Map.of("architecture", "ALL")));
        verify(verticalScaleInstanceProvider, times(1))
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class),
                        eq(Set.of("1", "2", "3")), anyMap(), any(CdpResourceType.class));
    }

    @Test
    public void testRequestForMultiAzWithNullAvailabilityZoneResolvesFromInstanceGroupAzs() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInStack = "master";
        String instanceGroupNameInRequest = "master";
        String instanceTypeNameInStack = "e2-standard-2";
        String instanceTypeNameInRequest = "e2-standard-4";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "us-west2-a",
                vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                ),
                vmType(
                        instanceTypeNameInRequest,
                        2,
                        2,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                )
        );

        when(stack.isMultiAz()).thenReturn(true);
        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("us-west2");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn(null);
        when(stack.getPlatformvariant()).thenReturn("awsvariant");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack,
                Set.of("us-west2-a", "us-west2-b"))));
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes);
        when(multiAzCalculatorService.getAvailabilityZoneConnector(stack)).thenReturn(new AzureAvailabilityZoneConnector());
        when(availabilityZoneService.findAllByInstanceGroupId(anyLong())).thenReturn(Set.of("us-west2-a", "us-west2-b").stream().map(s -> {
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

        verify(verticalScaleInstanceProvider, times(1))
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class),
                        eq(Set.of("us-west2-a", "us-west2-b")), anyMap(), any());
    }

    @Test
    public void testRequestForMultiAzAzureWithNullAvailabilityZoneFallsBackToRegion() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS, AZURE));
        String instanceGroupNameInStack = "master";
        String instanceGroupNameInRequest = "master";
        String instanceTypeNameInStack = "Standard_D16d_v4";
        String instanceTypeNameInRequest = "Standard_D16d_v4";
        Credential credential = credential();
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredential();
        CloudVmTypes cloudVmTypes = cloudVmTypes(
                "westeurope",
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
        when(stack.getRegion()).thenReturn("westeurope");
        when(stack.isStopped()).thenReturn(true);
        when(stack.getAvailabilityZone()).thenReturn(null);
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

        underTest.validateRequest(stack, verticalScaleRequest);

        verify(verticalScaleInstanceProvider, times(1))
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class),
                        eq(Set.of("1", "2", "3")), anyMap(), any());
    }

    @Test
    public void testRequestWhenTheClusterDidNotStoppedShouldDropException() {
        ReflectionTestUtils.setField(underTest, "verticalScalingSupported", Set.of(AWS));
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInRequest = "m3.xlarge";

        when(stack.isStopped()).thenReturn(false);

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("Stack 'null' must be stopped to be able to vertically scale it.",
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

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "STOPPED"})
    public void testRollingScaleOneByOneOnSingleNodeShouldThrowBadRequestWhateverTheState(Status status) {
        lenient().when(stack.getStackStatus()).thenReturn(createStackStatus(status));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(new InstanceMetaData()));
        VerticalScaleRequest verticalScaleRequest = createRollingRequest(OrchestratorType.ONE_BY_ONE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRequestForRollingScale(stack, verticalScaleRequest));

        assertTrue(badRequestException.getMessage().contains("Rolling (ONE_BY_ONE) vertical scale is reserved for multi-node FreeIPA deployments"));
        assertTrue(badRequestException.getMessage().contains("use ALL_AT_ONCE or upscale FreeIPA to HA first"));
    }

    @Test
    public void testRollingScaleAllAtOnceOnSingleNodeShouldPassStateChecks() {
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getCloudPlatform()).thenReturn(AWS);
        VerticalScaleRequest verticalScaleRequest = createRollingRequest(OrchestratorType.ALL_AT_ONCE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRequestForRollingScale(stack, verticalScaleRequest));

        assertTrue(badRequestException.getMessage().contains("instance type"), badRequestException.getMessage());
    }

    @Test
    public void testRollingScaleOneByOneOnHaShouldPassStateChecks() {
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(new InstanceMetaData(), new InstanceMetaData()));
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getCloudPlatform()).thenReturn(AWS);
        VerticalScaleRequest verticalScaleRequest = createRollingRequest(OrchestratorType.ONE_BY_ONE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRequestForRollingScale(stack, verticalScaleRequest));

        assertTrue(badRequestException.getMessage().contains("instance type"), badRequestException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"STOPPED", "UPDATE_IN_PROGRESS", "VERTICAL_SCALE_FAILED", "STOP_IN_PROGRESS", "START_IN_PROGRESS",
            "DELETE_IN_PROGRESS", "UNKNOWN", "UNHEALTHY", "UNREACHABLE"})
    public void testRollingScaleWhenStackStateIsNotAllowedShouldThrowBadRequest(Status status) {
        when(stack.getStackStatus()).thenReturn(createStackStatus(status));
        when(stack.getName()).thenReturn("freeipa-name");
        VerticalScaleRequest verticalScaleRequest = createRollingRequest(OrchestratorType.ALL_AT_ONCE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRequestForRollingScale(stack, verticalScaleRequest));

        assertEquals(String.format("FreeIPA 'freeipa-name' must be running to perform rolling vertical scale. "
                + "Current state is %s.", status.name().toLowerCase(Locale.ROOT).replace('_', ' ')), badRequestException.getMessage());
    }

    @Test
    public void testRollingScaleWhenStackIsAvailableShouldNotThrowForBasicPlatformCheck() {
        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(new InstanceMetaData(), new InstanceMetaData()));

        VerticalScaleRequest verticalScaleRequest = createRollingRequest(OrchestratorType.ONE_BY_ONE);

        // Even though the template is null (which triggers a later check), the platform check passes.
        // We verify this by ensuring the thrown exception is about the template, not the platform.
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequestForRollingScale(stack, verticalScaleRequest);
        });

        assertFalse(badRequestException.getMessage().contains("not supported on AWS cloud platform"));
    }

    /**
     * Acceptance criteria rows of CB-35287 (entitled account) that must pass the rolling validation.
     * S4/S7/S8 (stopped) are excluded: stopped FreeIPA is routed to the existing vertical scale flow
     * before reaching the rolling validator, so STOPPED is rejected if the validator is called directly.
     */
    static Stream<Arguments> acceptedRollingScaleScenarios() {
        return Stream.of(
                Arguments.of("S3 single node, ALL_AT_ONCE, running", 1, OrchestratorType.ALL_AT_ONCE, Status.AVAILABLE),
                Arguments.of("S6 HA, ONE_BY_ONE, running", 2, OrchestratorType.ONE_BY_ONE, Status.AVAILABLE)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptedRollingScaleScenarios")
    public void testRollingScaleAcceptanceScenarioPassesValidation(String scenario, int nodeCount, OrchestratorType orchestratorType, Status status) {
        mockValidRollingScaleStack(nodeCount, status);

        underTest.validateRequestForRollingScale(stack, createValidRollingRequest(orchestratorType));

        verify(verticalScaleInstanceProvider)
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class), isNull(), anyMap(), any());
    }

    /**
     * Acceptance criteria rows of CB-35287 (entitled account) that must be rejected by the rolling validation.
     */
    static Stream<Arguments> rejectedRollingScaleScenarios() {
        return Stream.of(
                Arguments.of("S1 single node, ONE_BY_ONE, running", Status.AVAILABLE),
                Arguments.of("S2 single node, ONE_BY_ONE, stopped", Status.STOPPED)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedRollingScaleScenarios")
    public void testRollingScaleAcceptanceScenarioIsRejected(String scenario, Status status) {
        lenient().when(stack.getStackStatus()).thenReturn(createStackStatus(status));
        when(stack.getName()).thenReturn("freeipa-name");
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(new InstanceMetaData()));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRequestForRollingScale(stack, createValidRollingRequest(OrchestratorType.ONE_BY_ONE)));

        assertEquals("Rolling (ONE_BY_ONE) vertical scale is reserved for multi-node FreeIPA deployments. "
                + "For single-node FreeIPA 'freeipa-name', use ALL_AT_ONCE or upscale FreeIPA to HA first.", badRequestException.getMessage());
        verify(verticalScaleInstanceProvider, never())
                .validateInstanceTypeForVerticalScaling(anyString(), any(List.class), any(List.class), any(), anyMap(), any());
    }

    private void mockValidRollingScaleStack(int nodeCount, Status status) {
        Set<InstanceMetaData> instances = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            instances.add(new InstanceMetaData());
        }
        lenient().when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instances);
        when(stack.getStackStatus()).thenReturn(createStackStatus(status));
        when(stack.isAvailable()).thenReturn(Status.AVAILABLE.equals(status));
        when(stack.getCloudPlatform()).thenReturn(AWS);
        when(stack.getEnvironmentCrn()).thenReturn("crn");
        when(stack.getRegion()).thenReturn("eu");
        when(stack.getAvailabilityZone()).thenReturn("eu1");
        when(stack.getPlatformvariant()).thenReturn("awsvariant");
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup("master", "m5.xlarge")));
        Credential credential = credential();
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential());
        when(cloudParameterService.getVmTypesV2(any(), anyString(), anyString(), any(), any())).thenReturn(cloudVmTypes("eu1",
                vmType("m5.2xlarge", 1, 1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1))));
    }

    private VerticalScaleRequest createValidRollingRequest(OrchestratorType orchestratorType) {
        VerticalScaleRequest verticalScaleRequest = createRollingRequest(orchestratorType);
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("m5.2xlarge");
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup("master");
        return verticalScaleRequest;
    }

    private VerticalScaleRequest createRollingRequest(OrchestratorType orchestratorType) {
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        verticalScaleRequest.setOrchestratorType(orchestratorType.name());
        return verticalScaleRequest;
    }

    private StackStatus createStackStatus(Status status) {
        return new StackStatus(stack, status, "", DetailedStackStatus.UNKNOWN);
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