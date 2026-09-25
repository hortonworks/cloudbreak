package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageHyperVGenerationResolver;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AzureImageVmTypeCompatibilityValidatorTest {

    private static final String REGION = "westus2";

    private static final String ACCOUNT_ID = "accountId";

    private static final String GEN1_ONLY_VM = "Standard_D2_v2";

    private static final String GEN2_ONLY_VM = "Standard_D2_v6";

    private static final String DUAL_GENERATION_VM = "Standard_D2s_v5";

    private static final String UNKNOWN_VM = "Standard_Whatever";

    @Mock
    private AzureImageHyperVGenerationResolver azureImageHyperVGenerationResolver;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private Image image;

    @InjectMocks
    private AzureImageVmTypeCompatibilityValidator underTest;

    @BeforeEach
    void setUp() {
        when(cloudStack.getImage()).thenReturn(image);
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyInstanceTypeThenRejected() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroups(GEN1_ONLY_VM);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));

        assertTrue(exception.getMessage().contains("is Generation 2"), exception.getMessage());
        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndDualGenerationInstanceTypeThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroups(DUAL_GENERATION_VM);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenGen1ImageAndGen2OnlyInstanceTypeThenRejected() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V1);
        setUpGroups(GEN2_ONLY_VM);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));

        assertTrue(exception.getMessage().contains("is Generation 1"), exception.getMessage());
        assertTrue(exception.getMessage().contains(GEN2_ONLY_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenGen1ImageAndDualGenerationInstanceTypeThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V1);
        setUpGroups(DUAL_GENERATION_VM);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenOnlyOneOfSeveralGroupsIsIncompatibleThenOnlyThatOneIsReported() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroups(DUAL_GENERATION_VM, GEN1_ONLY_VM);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));

        assertTrue(exception.getMessage().contains(GEN1_ONLY_VM), exception.getMessage());
        assertTrue(!exception.getMessage().contains(DUAL_GENERATION_VM), exception.getMessage());
    }

    @Test
    void testValidateWhenImageHasNoGenerationTagThenSkippedWithoutCallingAzure() {
        when(azureImageHyperVGenerationResolver.resolve(image)).thenReturn(null);

        underTest.validate(authenticatedContext, cloudStack);

        verify(azureClient, never()).getHostCapabilities(anyString());
    }

    @Test
    void testValidateWhenInstanceTypeCapabilitiesAreNotReportedThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroups(UNKNOWN_VM);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenCapabilityFetchFailsThenAccepted() {
        setUpCloudContext();
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getHostCapabilities(REGION)).thenThrow(new RuntimeException("Azure is having a bad day"));
        setUpImageGeneration(HyperVGenerationTypes.V2);
        when(cloudStack.getGroups()).thenReturn(List.of(mock(Group.class)));

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenCapabilitiesAreNullThenAccepted() {
        setUpCloudContext();
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getHostCapabilities(REGION)).thenReturn(null);
        setUpImageGeneration(HyperVGenerationTypes.V2);
        when(cloudStack.getGroups()).thenReturn(List.of(mock(Group.class)));

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));

        verify(entitlementService, never()).isFallbackInstanceTypeEnabled(anyString());
    }

    @Test
    void testValidateWhenStackHasNoGroupsThenSkippedWithoutCallingAzure() {
        setUpImageGeneration(HyperVGenerationTypes.V2);
        when(cloudStack.getGroups()).thenReturn(List.of());

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));

        verify(azureClient, never()).getHostCapabilities(anyString());
    }

    @Test
    void testValidateWhenCloudContextHasNoLocationThenSkippedWithoutCallingAzure() {
        setUpImageGeneration(HyperVGenerationTypes.V2);
        when(cloudStack.getGroups()).thenReturn(List.of(mock(Group.class)));
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));

        verify(azureClient, never()).getHostCapabilities(anyString());
    }

    @Test
    void testValidateWhenCloudContextHasNoRegionThenSkippedWithoutCallingAzure() {
        setUpImageGeneration(HyperVGenerationTypes.V2);
        when(cloudStack.getGroups()).thenReturn(List.of(mock(Group.class)));
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getLocation()).thenReturn(Location.location(null));
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));

        verify(azureClient, never()).getHostCapabilities(anyString());
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackEnabledThenRejected() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroupWithFallbacks(DUAL_GENERATION_VM, GEN1_ONLY_VM);
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));

        assertTrue(exception.getMessage().contains("(" + GEN1_ONLY_VM + ")"), exception.getMessage());
        assertTrue(!exception.getMessage().contains("(" + DUAL_GENERATION_VM + ")"), exception.getMessage());
    }

    @Test
    void testValidateWhenGen2ImageAndGen1OnlyFallbackAndFallbackDisabledThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroupWithFallbacks(DUAL_GENERATION_VM, GEN1_ONLY_VM);
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenGen2ImageAndCompatibleFallbacksAndFallbackEnabledThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroupWithFallbacks(DUAL_GENERATION_VM, GEN2_ONLY_VM, DUAL_GENERATION_VM);
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenFallbackCapabilitiesAreNotReportedThenAccepted() {
        setUpAzureClient();
        setUpImageGeneration(HyperVGenerationTypes.V2);
        setUpGroupWithFallbacks(DUAL_GENERATION_VM, UNKNOWN_VM);
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validate(authenticatedContext, cloudStack));
    }

    private void setUpGroupWithFallbacks(String flavor, String... fallbacks) {
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(template.getFlavor()).thenReturn(flavor);
        lenient().when(template.getFallbackInstanceTypes()).thenReturn(List.of(fallbacks));
        Group group = mock(Group.class);
        when(group.getReferenceInstanceTemplate()).thenReturn(template);
        lenient().when(group.getName()).thenReturn("group-" + flavor);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
    }

    private void setUpImageGeneration(HyperVGenerationTypes generation) {
        when(azureImageHyperVGenerationResolver.resolve(image)).thenReturn(generation);
    }

    private void setUpAzureClient() {
        setUpCloudContext();
        Map<String, AzureVmCapabilities> capabilities = Map.of(
                GEN1_ONLY_VM, capabilitiesWithGenerations("V1"),
                GEN2_ONLY_VM, capabilitiesWithGenerations("V2"),
                DUAL_GENERATION_VM, capabilitiesWithGenerations("V1,V2"));
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getHostCapabilities(REGION)).thenReturn(capabilities);
    }

    private void setUpGroups(String... flavors) {
        List<Group> groups = List.of(flavors).stream().map(flavor -> {
            InstanceTemplate template = mock(InstanceTemplate.class);
            when(template.getFlavor()).thenReturn(flavor);
            Group group = mock(Group.class);
            when(group.getReferenceInstanceTemplate()).thenReturn(template);
            // Only queried for the groups that turn out to be incompatible.
            lenient().when(group.getName()).thenReturn("group-" + flavor);
            return group;
        }).toList();
        when(cloudStack.getGroups()).thenReturn(groups);
    }

    private AzureVmCapabilities capabilitiesWithGenerations(String hyperVGenerations) {
        ResourceSkuCapabilities skuCapability = mock(ResourceSkuCapabilities.class);
        when(skuCapability.name()).thenReturn("HyperVGenerations");
        when(skuCapability.value()).thenReturn(hyperVGenerations);
        return new AzureVmCapabilities(null, List.of(skuCapability));
    }

    private void setUpCloudContext() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION)));
        lenient().when(cloudContext.getAccountId()).thenReturn(ACCOUNT_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
    }
}
