package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class DistroXHyperVGenerationValidatorTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:env";

    private static final String REGION = "westus2";

    private static final String GEN2_ONLY = "Standard_E8as_v5_gen2only";

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private LocationService locationService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StatedImage statedImage;

    @Mock
    private Image image;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private CredentialResponse credentialResponse;

    @InjectMocks
    private DistroXHyperVGenerationValidator underTest;

    @BeforeEach
    void setUp() {
        lenient().when(statedImage.getImage()).thenReturn(image);
        lenient().when(image.getTags()).thenReturn(Map.of(HYPER_V_GENERATION_TAG, "2"));
        Credential credential = Credential.builder().account(ACCOUNT_ID).build();
        lenient().when(environment.getCredential()).thenReturn(credentialResponse);
        lenient().when(environment.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(credentialConverter.convert(credentialResponse)).thenReturn(credential);
        lenient().when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        lenient().when(locationService.location(REGION, null)).thenReturn(REGION);
        lenient().when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(vmTypes(
                vmType("Standard_D8s_v5", "V1,V2"),
                vmType("Standard_E8ds_v5", "V1,V2"),
                vmType("Standard_D4_v2", "V1"),
                vmType("Standard_D8s_v3", "V1"),
                vmType(GEN2_ONLY, "V2"),
                vmType("Standard_Unreported", null)));
    }

    @Test
    void testValidateWhenGen2ImageAndAllV5InstanceTypesThenPasses() {
        StackV4Request request = request(group("master", "Standard_D8s_v5"), group("worker", "Standard_E8ds_v5"));

        assertDoesNotThrow(() -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));

        verify(cloudParameterService).getVmTypesV2(eq(extendedCloudCredential), eq(REGION), eq("AZURE"), eq(CdpResourceType.DATAHUB),
                eq(Map.of("architecture", "x86_64")));
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyInstanceTypeThenRejected() {
        StackV4Request request = request(group("master", "Standard_D8s_v5"), group("worker", "Standard_D4_v2"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));

        assertEquals("The selected image is Hyper-V generation V2, but the following instance types do not support it: "
                        + "Standard_D4_v2 (worker, supports V1). Choose a V2 compatible instance type or an image of the matching generation.",
                exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndMultipleGen1OnlyInstanceTypesThenAllListed() {
        StackV4Request request = request(group("master", "Standard_D8s_v3"), group("worker", "Standard_D4_v2"), group("compute", "Standard_D8s_v5"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));

        assertEquals("The selected image is Hyper-V generation V2, but the following instance types do not support it: "
                + "Standard_D8s_v3 (master, supports V1), Standard_D4_v2 (worker, supports V1). "
                + "Choose a V2 compatible instance type or an image of the matching generation.", exception.getMessage());
    }

    @Test
    void testValidateWhenGen1ImageAndGen2OnlyInstanceTypeThenRejected() {
        when(image.getTags()).thenReturn(Map.of(HYPER_V_GENERATION_TAG, "1"));
        StackV4Request request = request(group("master", "Standard_D8s_v5"), group("worker", GEN2_ONLY));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));

        assertEquals("The selected image is Hyper-V generation V1, but the following instance types do not support it: "
                        + GEN2_ONLY + " (worker, supports V2). Choose a V1 compatible instance type or an image of the matching generation.",
                exception.getMessage());
    }

    @Test
    void testValidateWhenGen1OnlyFallbackAndFallbackEnabledThenRejected() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        StackV4Request request = request(group("worker", "Standard_D8s_v5", "Standard_E8ds_v5", "Standard_D4_v2", "Standard_D8s_v5"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));

        assertEquals("The selected image is Hyper-V generation V2, but the following instance types do not support it: "
                        + "Standard_D4_v2 (worker fallback, supports V1). Choose a V2 compatible instance type or an image of the matching generation.",
                exception.getMessage());
    }

    @Test
    void testValidateWhenGen1OnlyFallbackAndFallbackDisabledThenPasses() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(false);
        StackV4Request request = request(group("worker", "Standard_D8s_v5", "Standard_D4_v2"));

        assertDoesNotThrow(() -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));
    }

    @Test
    void testValidateWhenGenerationsNotReportedOrInstanceTypeUnknownThenPasses() {
        StackV4Request request = request(group("master", "Standard_Unreported"), group("worker", "Standard_NotInRegion"));

        assertDoesNotThrow(() -> underTest.validate(request, azureStack(), environment, statedImage, StackType.WORKLOAD));
    }

    @Test
    void testValidateWhenImageIsNotTaggedThenNoLookup() {
        when(image.getTags()).thenReturn(Map.of("release-version", "7.3.1"));

        underTest.validate(request(group("worker", "Standard_D4_v2")), azureStack(), environment, statedImage, StackType.WORKLOAD);

        verifyNoInteractions(credentialConverter, cloudParameterService, entitlementService);
    }

    @Test
    void testValidateWhenImageTagIsUnrecognisedThenNoLookup() {
        when(image.getTags()).thenReturn(Map.of(HYPER_V_GENERATION_TAG, "gen2"));

        underTest.validate(request(group("worker", "Standard_D4_v2")), azureStack(), environment, statedImage, StackType.WORKLOAD);

        verifyNoInteractions(credentialConverter, cloudParameterService, entitlementService);
    }

    @Test
    void testValidateWhenNotAzureThenNoLookup() {
        Stack stack = azureStack();
        stack.setCloudPlatform("AWS");

        underTest.validate(request(group("worker", "Standard_D4_v2")), stack, environment, statedImage, StackType.WORKLOAD);

        verifyNoInteractions(credentialConverter, cloudParameterService, entitlementService);
    }

    @ParameterizedTest
    @EnumSource(value = StackType.class, names = "WORKLOAD", mode = EnumSource.Mode.EXCLUDE)
    void testValidateWhenNotDataHubThenNoLookup(StackType stackType) {
        underTest.validate(request(group("worker", "Standard_D4_v2")), azureStack(), environment, statedImage, stackType);

        verifyNoInteractions(credentialConverter, cloudParameterService, entitlementService);
    }

    @Test
    void testValidateWhenNoImageThenNoLookup() {
        underTest.validate(request(group("worker", "Standard_D4_v2")), azureStack(), environment, null, StackType.WORKLOAD);

        verifyNoInteractions(credentialConverter, cloudParameterService, entitlementService);
    }

    @Test
    void testValidateWhenVmTypeLookupFailsThenFailsOpen() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenThrow(new CloudbreakServiceException("Azure is down"));

        assertDoesNotThrow(() -> underTest.validate(request(group("worker", "Standard_D4_v2")), azureStack(), environment, statedImage, StackType.WORKLOAD));
    }

    private Stack azureStack() {
        Stack stack = new Stack();
        stack.setName("dh");
        stack.setCloudPlatform("AZURE");
        stack.setPlatformVariant("AZURE");
        stack.setRegion(REGION);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setArchitecture(Architecture.X86_64);
        return stack;
    }

    private StackV4Request request(InstanceGroupV4Request... groups) {
        StackV4Request request = new StackV4Request();
        request.setInstanceGroups(new ArrayList<>(List.of(groups)));
        return request;
    }

    private InstanceGroupV4Request group(String name, String instanceType, String... fallbackInstanceTypes) {
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType(instanceType);
        template.setFallbackInstanceTypes(List.of(fallbackInstanceTypes));
        InstanceGroupV4Request group = new InstanceGroupV4Request();
        group.setName(name);
        group.setTemplate(template);
        return group;
    }

    private CloudVmTypes vmTypes(VmType... vmTypes) {
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> responses = new HashMap<>();
        responses.put(REGION, Set.of(vmTypes));
        cloudVmTypes.setCloudVmResponses(responses);
        return cloudVmTypes;
    }

    private VmType vmType(String name, String hyperVGenerations) {
        return VmType.vmTypeWithMeta(name, VmTypeMeta.VmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.X86_64)
                .withHyperVGenerations(hyperVGenerations)
                .create(), true);
    }
}
