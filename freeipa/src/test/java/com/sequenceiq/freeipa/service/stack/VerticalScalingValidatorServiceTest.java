package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;

@ExtendWith(MockitoExtension.class)
public class VerticalScalingValidatorServiceTest {

    private static final String AWS = CloudPlatform.AWS.name();

    private static final String OPENSTACK = CloudPlatform.OPENSTACK.name();

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
    }

    @Test
    public void testRequestWhenWeAreRequestedSmallerMemoryInstancesShouldDropBadRequest() {
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
                .when(verticalScaleInstanceProvider).validInstanceTypeForVerticalScaling(any(), any());

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
    }

    @Test
    public void testRequestWhenWeAreRequestedSmallerCpuInstancesShouldDropBadRequest() {
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
                        1,
                        0,
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
        doThrow(new BadRequestException("The current instancetype m3.xlarge has more CPU then the requested m2.xlarge."))
                .when(verticalScaleInstanceProvider).validInstanceTypeForVerticalScaling(any(), any());

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("The current instancetype m3.xlarge has more CPU then the requested m2.xlarge.",
                badRequestException.getMessage());
        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessEphemeralShouldDropBadRequest() {
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
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 0, 0, 0, 0)
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
        doThrow(new BadRequestException("The current instancetype m3.xlarge has more Ephemeral Disk then the requested m2.xlarge."))
                .when(verticalScaleInstanceProvider).validInstanceTypeForVerticalScaling(any(), any());

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("The current instancetype m3.xlarge has more Ephemeral Disk then the requested m2.xlarge.",
                badRequestException.getMessage());
        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessAutoAttachedShouldDropBadRequest() {
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
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 0, 0, 0, 0),
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
        doThrow(new BadRequestException("The current instancetype m3.xlarge has more Auto Attached Disk then the requested m2.xlarge."))
                .when(verticalScaleInstanceProvider).validInstanceTypeForVerticalScaling(any(), any());

        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceTypeNameInRequest);
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        verticalScaleRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateRequest(stack, verticalScaleRequest);
        });

        assertEquals("The current instancetype m3.xlarge has more Auto Attached Disk then the requested m2.xlarge.",
                badRequestException.getMessage());
        verify(credentialService, times(1)).getCredentialByEnvCrn(anyString());
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), anyString(), anyString(), any(), any());
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
        return vmTypeWithMeta(name,
                VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withAutoAttachedConfig(autoAttached)
                        .withCpuAndMemory(cpu, memory)
                        .withEphemeralConfig(ephemeral)
                        .create(),
                false);
    }

    private InstanceGroup instanceGroup(String name, String instanceType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        Template template = new Template();
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
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
                "user",
                "accountId",
                List.of());
    }
}