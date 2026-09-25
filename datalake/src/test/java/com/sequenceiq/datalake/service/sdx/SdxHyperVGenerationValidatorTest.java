package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;

@ExtendWith(MockitoExtension.class)
class SdxHyperVGenerationValidatorTest {

    private static final String CREDENTIAL_CRN = "crn:cdp:environments:us-west-1:acc:credential:1234";

    private static final String REGION = "westus2";

    private static final String ACCOUNT_ID = "acc";

    private static final String GEN1_ONLY_VM = "Standard_D2_v2";

    private static final String GEN2_ONLY_VM = "Standard_D2_v6";

    private static final String DUAL_GENERATION_VM = "Standard_D2s_v5";

    private static final String UNKNOWN_VM = "Standard_Whatever";

    @Mock
    private SdxRecommendationService sdxRecommendationService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxHyperVGenerationValidator underTest;

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyInstanceTypeThenBadRequest() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), image("2"), null));

        assertTrue(exception.getMessage().contains("generation V2"), exception.getMessage());
        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndDualGenerationInstanceTypeThenAccepted() {
        setUpVmTypes();

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequest(DUAL_GENERATION_VM), image("2"), null));
    }

    @Test
    void testValidateWhenGen1ImageAndGen2OnlyInstanceTypeThenBadRequest() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(azureEnvironment(), stackRequest(GEN2_ONLY_VM), image("1"), null));

        assertTrue(exception.getMessage().contains("generation V1"), exception.getMessage());
        assertTrue(exception.getMessage().contains(GEN2_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenOnlyOneOfSeveralInstanceGroupsIsIncompatibleThenOnlyThatOneIsReported() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(azureEnvironment(), stackRequest(DUAL_GENERATION_VM, GEN1_ONLY_VM), image("2"), null));

        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM), exception.getMessage());
        assertTrue(!exception.getMessage().contains(DUAL_GENERATION_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenImageHasNoGenerationTagThenSkippedWithoutFetchingVmTypes() {
        underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), image(null), null);

        verify(sdxRecommendationService, never()).getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateWhenNotAzureThenSkippedWithoutFetchingVmTypes() {
        DetailedEnvironmentResponse environment = azureEnvironment();
        environment.setCloudPlatform("AWS");

        underTest.validate(environment, stackRequest(GEN1_ONLY_VM), image("2"), null);

        verify(sdxRecommendationService, never()).getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateWhenImageIsNullThenSkippedWithoutFetchingVmTypes() {
        underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), null, null);

        verify(sdxRecommendationService, never()).getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateWhenInstanceTypeGenerationsAreNotReportedThenAccepted() {
        setUpVmTypes();

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequest(UNKNOWN_VM), image("2"), null));
    }

    @Test
    void testValidateWhenVmTypeFetchFailsThenAccepted() {
        when(sdxRecommendationService.getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Could not reach the environment service"));

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), image("2"), null));
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackEnabledThenBadRequest() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(azureEnvironment(), stackRequestWithFallbacks(DUAL_GENERATION_VM, GEN1_ONLY_VM), image("2"), null));

        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM + " (group-" + DUAL_GENERATION_VM + " fallback"), exception.getMessage());
        assertTrue(!exception.getMessage().contains(DUAL_GENERATION_VM + " ("), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackDisabledThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequestWithFallbacks(DUAL_GENERATION_VM, GEN1_ONLY_VM), image("2"), null));
    }

    @Test
    void testValidateWhenGen2ImageAndCompatibleFallbacksAndFallbackEnabledThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(),
                stackRequestWithFallbacks(DUAL_GENERATION_VM, GEN2_ONLY_VM, DUAL_GENERATION_VM), image("2"), null));
    }

    @Test
    void testValidateWhenFallbackGenerationsAreNotReportedThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequestWithFallbacks(DUAL_GENERATION_VM, UNKNOWN_VM), image("2"), null));
    }

    @Test
    void testValidateWhenAvailableVmTypesAreProvidedThenTheyAreUsedWithoutFetching() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), image("2"), vmTypes()));

        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM), exception.getMessage());
        verify(sdxRecommendationService, never()).getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateWhenProvidedAvailableVmTypesAreEmptyThenAcceptedWithoutFetching() {
        assertDoesNotThrow(() -> underTest.validate(azureEnvironment(), stackRequest(GEN1_ONLY_VM), image("2"), List.of()));

        verify(sdxRecommendationService, never()).getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString());
    }

    private StackV4Request stackRequestWithFallbacks(String instanceType, String... fallbacks) {
        StackV4Request stackRequest = stackRequest(instanceType);
        stackRequest.getInstanceGroups().getFirst().getTemplate().setFallbackInstanceTypes(List.of(fallbacks));
        return stackRequest;
    }

    private void setUpVmTypes() {
        when(sdxRecommendationService.getAvailableVmTypes(anyString(), anyString(), any(), any(), anyString())).thenReturn(vmTypes());
    }

    private List<VmTypeResponse> vmTypes() {
        return List.of(
                vmType(GEN1_ONLY_VM, "V1"),
                vmType(GEN2_ONLY_VM, "V2"),
                vmType(DUAL_GENERATION_VM, "V1,V2"),
                vmType(UNKNOWN_VM, null));
    }

    private VmTypeResponse vmType(String name, String hyperVGenerations) {
        VmTypeMetaJson metaJson = new VmTypeMetaJson();
        if (hyperVGenerations != null) {
            metaJson.setProperties(Map.of(VmTypeMeta.HYPER_V_GENERATIONS, hyperVGenerations));
        }
        return new VmTypeResponse(name, metaJson);
    }

    private ImageV4Response image(String generationTag) {
        ImageV4Response image = new ImageV4Response();
        image.setTags(generationTag == null ? Map.of("release-version", "7.3.1") : Map.of(HYPER_V_GENERATION_TAG, generationTag));
        return image;
    }

    private StackV4Request stackRequest(String... instanceTypes) {
        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        for (String instanceType : instanceTypes) {
            InstanceTemplateV4Request template = new InstanceTemplateV4Request();
            template.setInstanceType(instanceType);
            InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
            instanceGroup.setName("group-" + instanceType);
            instanceGroup.setTemplate(template);
            instanceGroups.add(instanceGroup);
        }
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setInstanceGroups(instanceGroups);
        return stackRequest;
    }

    private DetailedEnvironmentResponse azureEnvironment() {
        CredentialResponse credential = new CredentialResponse();
        credential.setCrn(CREDENTIAL_CRN);
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.setNames(List.of(REGION));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AZURE");
        environment.setAccountId(ACCOUNT_ID);
        environment.setCredential(credential);
        environment.setRegions(regions);
        return environment;
    }
}
