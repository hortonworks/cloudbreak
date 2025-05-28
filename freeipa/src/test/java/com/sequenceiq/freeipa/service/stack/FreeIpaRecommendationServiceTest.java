package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.FreeIpaRecommendationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.instance.VmTypeToVmTypeResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@ExtendWith(MockitoExtension.class)
class FreeIpaRecommendationServiceTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Spy
    private VmTypeToVmTypeResponseConverter vmTypeConverter;

    @InjectMocks
    private FreeIpaRecommendationService underTest;

    @Test
    public void testGetRecommendation() {
        when(credentialService.getCredentialByCredCrn(anyString())).thenReturn(new Credential("AWS", "", "", "", ""));
        when(cloudParameterService.getVmTypesV2(any(), eq("eu-central-1"), eq("AWS"), eq(CdpResourceType.DEFAULT), any())).thenReturn(initCloudVmTypes());
        when(defaultInstanceTypeProvider.getForPlatform(eq("AWS"), eq(Architecture.X86_64))).thenReturn("medium");

        FreeIpaRecommendationResponse recommendation = underTest.getRecommendation("cred", "eu-central-1", null, null);
        assertEquals("medium", recommendation.getDefaultInstanceType());
        Set<VmTypeResponse> vmTypes = recommendation.getVmTypes();
        assertEquals(2, vmTypes.size());
        assertThat(vmTypes.stream().map(VmTypeResponse::getValue).collect(Collectors.toSet())).containsExactly("large", "medium");
    }

    @Test
    public void testValidateCustomInstanceTypeWhenCustomInstanceTypeIsSmaller() {
        when(defaultInstanceTypeProvider.getForPlatform(eq("AWS"), any())).thenReturn("medium");
        when(cloudParameterService.getVmTypesV2(any(), eq("eu-central-1"), eq("AWS"), eq(CdpResourceType.DEFAULT), any())).thenReturn(initCloudVmTypes());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateCustomInstanceType(createStack("small"), new Credential("AWS", "Cred", null, "crn", "account")));
        assertEquals("Invalid custom instance type for FreeIPA: master - small", badRequestException.getMessage());
    }

    @Test
    public void testValidateCustomInstanceTypeWhenCustomInstanceTypeIsLarger() {
        when(defaultInstanceTypeProvider.getForPlatform(eq("AWS"), any())).thenReturn("medium");
        when(cloudParameterService.getVmTypesV2(any(), eq("eu-central-1"), eq("AWS"), eq(CdpResourceType.DEFAULT), any())).thenReturn(initCloudVmTypes());

        assertDoesNotThrow(() -> underTest.validateCustomInstanceType(createStack("large"), new Credential("AWS", "Cred", null, "crn", "account")));
    }

    private Stack createStack(String instanceType) {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("eu-central-1");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Template template = new Template();
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        stack.getInstanceGroups().add(instanceGroup);
        return stack;
    }

    private CloudVmTypes initCloudVmTypes() {
        Map<String, Set<VmType>> cloudVmResponses = Map.of("eu-central-1a",
                Set.of(VmType.vmTypeWithMeta("large", vmTypeMeta(10, 1000.0F), false),
                        VmType.vmTypeWithMeta("medium", vmTypeMeta(8, 800.0F), false),
                        VmType.vmTypeWithMeta("small", vmTypeMeta(1, 1.0F), false)));
        return new CloudVmTypes(cloudVmResponses, Map.of());
    }

    private VmTypeMeta vmTypeMeta(int cpu, float memory) {
        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        Map<String, Object> properties = new HashMap<>();
        properties.put(VmTypeMeta.CPU, cpu);
        properties.put(VmTypeMeta.MEMORY, memory);
        vmTypeMeta.setProperties(properties);
        return vmTypeMeta;
    }
}