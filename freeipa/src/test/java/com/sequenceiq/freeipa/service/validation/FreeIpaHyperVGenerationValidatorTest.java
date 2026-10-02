package com.sequenceiq.freeipa.service.validation;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.FreeIpaRecommendationService;

@ExtendWith(MockitoExtension.class)
class FreeIpaHyperVGenerationValidatorTest {

    private static final String STACK_NAME = "freeipa-stack";

    private static final String ACCOUNT_ID = "accountId";

    private static final String DEFAULT_GEN1_ONLY_VM = "Standard_DS3_v2";

    private static final String GEN2_ONLY_VM = "Standard_D2_v6";

    private static final String DUAL_GENERATION_VM = "Standard_D2s_v5";

    private static final String UNKNOWN_VM = "Standard_Whatever";

    @Mock
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Credential credential;

    @InjectMocks
    private FreeIpaHyperVGenerationValidator underTest;

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyDefaultInstanceTypeThenBadRequest() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(stack("AZURE", DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of()));

        assertTrue(exception.getMessage().contains("generation V2"), exception.getMessage());
        assertTrue(exception.getMessage().contains(DEFAULT_GEN1_ONLY_VM), exception.getMessage());
        assertTrue(exception.getMessage().contains("group-" + DEFAULT_GEN1_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndDualGenerationInstanceTypeThenAccepted() {
        setUpVmTypes();

        assertDoesNotThrow(() -> underTest.validate(stack("AZURE", DUAL_GENERATION_VM), credential, image("2"), Set.of()));
    }

    @Test
    void testValidateWhenGen1ImageAndGen2OnlyInstanceTypeThenBadRequest() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(stack("AZURE", GEN2_ONLY_VM), credential, image("1"), Set.of()));

        assertTrue(exception.getMessage().contains("generation V1"), exception.getMessage());
        assertTrue(exception.getMessage().contains(GEN2_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenOnlyOneOfSeveralInstanceGroupsIsIncompatibleThenOnlyThatOneIsReported() {
        setUpVmTypes();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(stack("AZURE", DUAL_GENERATION_VM, DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of()));

        assertTrue(exception.getMessage().contains(DEFAULT_GEN1_ONLY_VM), exception.getMessage());
        assertFalse(exception.getMessage().contains(DUAL_GENERATION_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenImageHasNoGenerationTagThenSkippedWithoutFetchingVmTypes() {
        underTest.validate(stack("AZURE", DEFAULT_GEN1_ONLY_VM), credential, image(null), Set.of());

        verify(freeIpaRecommendationService, never()).getAvailableVmTypes(any(), any());
    }

    @Test
    void testValidateWhenNotAzureThenSkippedWithoutFetchingVmTypes() {
        underTest.validate(stack("AWS", DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of());

        verify(freeIpaRecommendationService, never()).getAvailableVmTypes(any(), any());
    }

    @Test
    void testValidateWhenImageIsNullThenSkippedWithoutFetchingVmTypes() {
        underTest.validate(stack("AZURE", DEFAULT_GEN1_ONLY_VM), credential, null, Set.of());

        verify(freeIpaRecommendationService, never()).getAvailableVmTypes(any(), any());
    }

    @Test
    void testValidateWhenInstanceTypeGenerationsAreNotReportedThenAccepted() {
        setUpVmTypes();

        assertDoesNotThrow(() -> underTest.validate(stack("AZURE", UNKNOWN_VM), credential, image("2"), Set.of()));
    }

    @Test
    void testValidateWhenVmTypeFetchFailsThenAccepted() {
        when(freeIpaRecommendationService.getAvailableVmTypes(any(), any())).thenThrow(new RuntimeException("Could not reach the cloud provider"));

        assertDoesNotThrow(() -> underTest.validate(stack("AZURE", DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of()));
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackEnabledThenBadRequest() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(stackWithFallbacks(DUAL_GENERATION_VM, DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of()));

        assertTrue(exception.getMessage().contains(DEFAULT_GEN1_ONLY_VM + " (group-" + DUAL_GENERATION_VM + " fallback"), exception.getMessage());
        assertFalse(exception.getMessage().contains(DUAL_GENERATION_VM + " ("), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackDisabledThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> underTest.validate(stackWithFallbacks(DUAL_GENERATION_VM, DEFAULT_GEN1_ONLY_VM), credential, image("2"), Set.of()));
    }

    @Test
    void testValidateWhenGen2ImageAndCompatibleFallbacksAndFallbackEnabledThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(stackWithFallbacks(DUAL_GENERATION_VM, GEN2_ONLY_VM, DUAL_GENERATION_VM), credential, image("2"),
                Set.of()));
    }

    @Test
    void testValidateWhenFallbackGenerationsAreNotReportedThenAccepted() {
        setUpVmTypes();
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(stackWithFallbacks(DUAL_GENERATION_VM, UNKNOWN_VM), credential, image("2"), Set.of()));
    }

    @Test
    void testValidateWhenAvailableVmTypesAreProvidedThenTheyAreUsedWithoutFetching() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(stack("AZURE", DEFAULT_GEN1_ONLY_VM), credential, image("2"), vmTypes()));

        assertTrue(exception.getMessage().contains(DEFAULT_GEN1_ONLY_VM), exception.getMessage());
        verify(freeIpaRecommendationService, never()).getAvailableVmTypes(any(), any());
    }

    @Test
    void testValidateWhenProvidedAvailableVmTypesAreEmptyThenTheyAreFetched() {
        setUpVmTypes();
        Stack stack = stack("AZURE", DEFAULT_GEN1_ONLY_VM);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(stack, credential, image("2"), Set.of()));

        assertTrue(exception.getMessage().contains(DEFAULT_GEN1_ONLY_VM), exception.getMessage());
        verify(freeIpaRecommendationService).getAvailableVmTypes(stack, credential);
    }

    private void setUpVmTypes() {
        when(freeIpaRecommendationService.getAvailableVmTypes(any(), any())).thenReturn(vmTypes());
    }

    private Set<VmType> vmTypes() {
        return Set.of(
                vmType(DEFAULT_GEN1_ONLY_VM, "V1"),
                vmType(GEN2_ONLY_VM, "V2"),
                vmType(DUAL_GENERATION_VM, "V1,V2"),
                vmType(UNKNOWN_VM, null));
    }

    private VmType vmType(String name, String hyperVGenerations) {
        return VmType.vmTypeWithMeta(name, VmTypeMeta.VmTypeMetaBuilder.builder().withHyperVGenerations(hyperVGenerations).create(), true);
    }

    private Image image(String generationTag) {
        return new Image(1L, "date", "description", "redhat8", "uuid", Map.of(), "osType", Map.of(), true, "x86_64",
                generationTag == null ? Map.of("release-version", "7.3.1") : Map.of(HYPER_V_GENERATION_TAG, generationTag));
    }

    private Stack stackWithFallbacks(String instanceType, String... fallbacks) {
        Stack stack = stack("AZURE", instanceType);
        stack.getInstanceGroups().iterator().next().getTemplate().setFallbackInstanceTypes(new Json(List.of(fallbacks)));
        return stack;
    }

    private Stack stack(String cloudPlatform, String... instanceTypes) {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(cloudPlatform);
        stack.setRegion("westus2");
        Set<InstanceGroup> instanceGroups = new LinkedHashSet<>();
        for (String instanceType : instanceTypes) {
            Template template = new Template();
            template.setInstanceType(instanceType);
            InstanceGroup instanceGroup = new InstanceGroup();
            instanceGroup.setGroupName("group-" + instanceType);
            instanceGroup.setTemplate(template);
            instanceGroups.add(instanceGroup);
        }
        stack.setInstanceGroups(instanceGroups);
        return stack;
    }
}
