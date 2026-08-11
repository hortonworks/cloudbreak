package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureInstanceTypeRetryExceptionMatcher;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureFallbackAwareDeploymentServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String RG = "rg-1";

    private static final String STACK_NAME = "stack-1";

    private static final String INITIAL_TEMPLATE = "{\"initial\":true}";

    private static final String REBUILT_TEMPLATE = "{\"rebuilt\":true}";

    private static final String PARAMETERS = "{\"params\":true}";

    @Mock
    private AzureTemplateBuilder azureTemplateBuilder;

    @Mock
    private AzureInstanceTypeRetryExceptionMatcher retryExceptionMatcher;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureStackView azureStackView;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AzureCredentialView credentialView;

    @Mock
    private AzureMarketplaceImage marketplaceImage;

    @Mock
    private Deployment deployment;

    @InjectMocks
    private AzureFallbackAwareDeploymentService underTest;

    @BeforeEach
    void setUp() {
        when(azureStackView.getInstanceGroupNames()).thenReturn(List.of("master", "worker"));
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("westus2")));
        when(cloudContext.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void submitsOnceWhenEntitlementIsDisabled() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(false);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenReturn(deployment);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        verify(azureClient, times(1)).createTemplateDeployment(anyString(), anyString(), anyString(), anyString());
        verify(azureTemplateBuilder, never()).build(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitsOnceWhenStackHasNoFallbackTypes() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", null), group("worker", null));
        when(cloudStack.getGroups()).thenReturn(groups);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenReturn(deployment);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        verify(azureClient, times(1)).createTemplateDeployment(anyString(), anyString(), anyString(), anyString());
        verify(azureTemplateBuilder, never()).build(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void retriesWithFallbackFlavorAndSucceedsOnSecondAttempt() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        ManagementException capacityException = capacityException("SkuNotAvailable");
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(capacityException);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenReturn(deployment);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(capacityException)).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of("master"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        ArgumentCaptor<Map<String, String>> overridesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(azureStackView).applyFlavorOverrides(overridesCaptor.capture());
        assertEquals("Standard_D8s_v5", overridesCaptor.getValue().get("master"));
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS);
    }

    @Test
    void rethrowsOriginalExceptionWhenFallbackChainIsExhausted() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        ManagementException firstFailure = capacityException("SkuNotAvailable");
        ManagementException secondFailure = capacityException("AllocationFailed");
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(firstFailure);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenThrow(secondFailure);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(any(ManagementException.class))).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of("master"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        // After exhausting the master group's single fallback we rethrow the most recent capacity failure.
        assertSame(secondFailure, thrown);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS);
    }

    @Test
    void rethrowsImmediatelyForNonCapacityException() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        ManagementException unrelated = capacityException("AuthorizationFailed");
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(unrelated);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(unrelated)).thenReturn(false);

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        assertSame(unrelated, thrown);
        verify(azureClient, times(1)).createTemplateDeployment(anyString(), anyString(), anyString(), anyString());
        verify(azureStackView, never()).applyFlavorOverrides(any());
    }

    @Test
    void rethrowsWhenNoFailingGroupCanBeAttributed() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        ManagementException capacityException = capacityException("AllocationFailed");
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(capacityException);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(capacityException)).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of());

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        assertSame(capacityException, thrown);
        verify(azureStackView, never()).applyFlavorOverrides(any());
    }

    @Test
    void retriesWithFallbackFlavorForQuotaExceededWithFamilyAttribution() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        Group gpu = group("gpu", List.of("Standard_NV4as_v4"));
        when(cloudStack.getGroups()).thenReturn(List.of(gpu));
        ManagementException preflight = quotaException();
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(preflight);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenReturn(deployment);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(preflight)).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of());
        when(retryExceptionMatcher.isQuotaCodePresent(preflight)).thenReturn(true);
        Map<String, String> flavorMap = Map.of("Standard_NV12s_v3", "standardNVSv3Family");
        when(azureClient.getVmToSkuFamilies("westus2")).thenReturn(flavorMap);
        when(retryExceptionMatcher.findGroupsWithQuotaFailure(preflight, flavorMap, List.of(gpu))).thenReturn(Set.of("gpu"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        ArgumentCaptor<Map<String, String>> overridesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(azureStackView).applyFlavorOverrides(overridesCaptor.capture());
        assertEquals("Standard_NV4as_v4", overridesCaptor.getValue().get("gpu"));
        verify(azureClient, times(1)).getVmToSkuFamilies("westus2");
    }

    @Test
    void bumpsAllGroupsWithFallbackWhenFamilyAttributionYieldsEmpty() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        Group gpu = group("gpu", List.of("Standard_NV4as_v4"));
        Group worker = group("worker", List.of("Standard_D8s_v5"));
        when(cloudStack.getGroups()).thenReturn(List.of(gpu, worker));
        ManagementException preflight = quotaException();
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(preflight);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenReturn(deployment);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(preflight)).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of());
        when(retryExceptionMatcher.isQuotaCodePresent(preflight)).thenReturn(true);
        when(azureClient.getVmToSkuFamilies("westus2")).thenReturn(Map.of());
        when(retryExceptionMatcher.findGroupsWithQuotaFailure(eq(preflight), any(), anyList())).thenReturn(Set.of());
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        ArgumentCaptor<Map<String, String>> overridesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(azureStackView).applyFlavorOverrides(overridesCaptor.capture());
        assertEquals("Standard_NV4as_v4", overridesCaptor.getValue().get("gpu"));
        assertEquals("Standard_D8s_v5", overridesCaptor.getValue().get("worker"));
    }

    @Test
    void fetchesSkuFamilyMapAtMostOncePerFallbackLoop() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        Group gpu = group("gpu", List.of("Standard_NV4as_v4", "Standard_NC6s_v3"));
        when(cloudStack.getGroups()).thenReturn(List.of(gpu));
        ManagementException first = quotaException();
        ManagementException second = quotaException();
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(first);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenThrow(second).thenReturn(deployment);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(any(ManagementException.class))).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of());
        when(retryExceptionMatcher.isQuotaCodePresent(any(ManagementException.class))).thenReturn(true);
        Map<String, String> flavorMap = Map.of("Standard_NV12s_v3", "standardNVSv3Family");
        when(azureClient.getVmToSkuFamilies("westus2")).thenReturn(flavorMap);
        when(retryExceptionMatcher.findGroupsWithQuotaFailure(any(ManagementException.class), eq(flavorMap), anyList())).thenReturn(Set.of("gpu"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        verify(azureClient, times(1)).getVmToSkuFamilies("westus2");
    }

    private ManagementException quotaException() {
        ManagementError leaf = new ManagementError("QuotaExceeded",
                "Operation could not be completed as it results in exceeding approved standardNVSv3Family Cores quota.");
        ManagementError top = new ManagementError("InvalidTemplateDeployment", "template invalid");
        AzureTestUtils.setDetails(top, List.of(leaf));
        return new ManagementException("Preflight failed", null, top);
    }

    private AzureTemplateDeploymentRequest request() {
        return new AzureTemplateDeploymentRequest(azureClient, RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS, azureStackView,
                cloudContext, cloudStack, credentialView, null, AzureInstanceTemplateOperation.PROVISION, marketplaceImage);
    }

    private Group group(String name, List<String> fallbackTypes) {
        Group group = org.mockito.Mockito.mock(Group.class);
        InstanceTemplate template = org.mockito.Mockito.mock(InstanceTemplate.class);
        org.mockito.Mockito.doReturn(name).when(group).getName();
        org.mockito.Mockito.doReturn(template).when(group).getReferenceInstanceTemplate();
        org.mockito.Mockito.doReturn(fallbackTypes).when(template).getFallbackInstanceTypes();
        return group;
    }

    private ManagementException capacityException(String code) {
        ManagementError topLevel = new ManagementError(code, "boom");
        return new ManagementException("Provisioning failed", null, topLevel);
    }
}
