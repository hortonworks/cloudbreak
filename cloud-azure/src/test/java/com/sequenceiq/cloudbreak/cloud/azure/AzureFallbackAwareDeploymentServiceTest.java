package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
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

    @Mock
    private com.sequenceiq.cloudbreak.cloud.notification.InstanceTypeFallbackReporter instanceTypeFallbackReporter;

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

        when(retryExceptionMatcher.getAzureErrorCodeForNotification(capacityException)).thenReturn("SkuNotAvailable");

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        ArgumentCaptor<Map<String, String>> overridesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(azureStackView).applyFlavorOverrides(overridesCaptor.capture());
        assertEquals("Standard_D8s_v5", overridesCaptor.getValue().get("master"));
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS);
        verify(instanceTypeFallbackReporter).reportFallback(cloudContext, "master", "master-orig-flavor", "Standard_D8s_v5", "SkuNotAvailable");
        verify(instanceTypeFallbackReporter, never()).reportFallbackExhausted(any(), any(), any(), any());
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

        when(retryExceptionMatcher.getAzureErrorCodeForNotification(any(ManagementException.class))).thenReturn("SkuNotAvailable");

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        // After exhausting the master group's single fallback we rethrow the most recent capacity failure.
        assertSame(secondFailure, thrown);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS);
        verify(azureClient).createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS);
        // First attempt fell back from orig -> Standard_D8s_v5; second attempt exhausted the chain.
        verify(instanceTypeFallbackReporter).reportFallback(cloudContext, "master", "master-orig-flavor", "Standard_D8s_v5", "SkuNotAvailable");
        verify(instanceTypeFallbackReporter).reportFallbackExhausted(cloudContext, "master", "master-orig-flavor", "SkuNotAvailable");
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

    @Test
    void retriesWithFallbackFlavorWhenCapacityFailureIsWrappedInInsufficientCapacityException() {
        // AzureExceptionHandler wraps ManagementExceptions with capacity codes (e.g. QuotaExceeded) in InsufficientCapacityException
        // before they surface from AzureClient. The fallback loop must still recognise and act on the underlying cause.
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        Group gpu = group("gpu", List.of("Standard_NV4as_v4"));
        when(cloudStack.getGroups()).thenReturn(List.of(gpu));
        ManagementException underlying = quotaException();
        InsufficientCapacityException wrapper = new InsufficientCapacityException(underlying);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(wrapper);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenReturn(deployment);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(underlying)).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of("gpu"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);
        when(retryExceptionMatcher.getAzureErrorCodeForNotification(underlying)).thenReturn("QuotaExceeded");

        Deployment result = underTest.createTemplateDeploymentWithFallback(request());

        assertSame(deployment, result);
        ArgumentCaptor<Map<String, String>> overridesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(azureStackView).applyFlavorOverrides(overridesCaptor.capture());
        assertEquals("Standard_NV4as_v4", overridesCaptor.getValue().get("gpu"));
        verify(instanceTypeFallbackReporter).reportFallback(cloudContext, "gpu", "gpu-orig-flavor", "Standard_NV4as_v4", "QuotaExceeded");
        verify(instanceTypeFallbackReporter, never()).reportFallbackExhausted(any(), any(), any(), any());
    }

    @Test
    void rethrowsInsufficientCapacityWrapperWhenFallbackChainIsExhausted() {
        // When the whole chain is exhausted after receiving wrapped capacity errors, the wrapper (not the underlying ManagementException)
        // is what bubbles up — matching how AzureExceptionHandler-routed callers see the exception in the fallback-disabled path.
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        Group gpu = group("gpu", List.of("Standard_NV4as_v4"));
        when(cloudStack.getGroups()).thenReturn(List.of(gpu));
        InsufficientCapacityException firstWrapper = new InsufficientCapacityException(quotaException());
        InsufficientCapacityException secondWrapper = new InsufficientCapacityException(quotaException());
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(firstWrapper);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, REBUILT_TEMPLATE, PARAMETERS)).thenThrow(secondWrapper);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(any(ManagementException.class))).thenReturn(true);
        when(retryExceptionMatcher.findGroupsWithCapacityFailure(eq(RG), eq(STACK_NAME), eq(STACK_NAME), anyList(), eq(azureClient)))
                .thenReturn(Set.of("gpu"));
        when(azureTemplateBuilder.build(eq(STACK_NAME), any(), eq(credentialView), eq(azureStackView), eq(cloudContext), eq(cloudStack),
                eq(AzureInstanceTemplateOperation.PROVISION), eq(marketplaceImage))).thenReturn(REBUILT_TEMPLATE);
        when(retryExceptionMatcher.getAzureErrorCodeForNotification(any(ManagementException.class))).thenReturn("QuotaExceeded");

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        assertSame(secondWrapper, thrown);
        verify(instanceTypeFallbackReporter).reportFallback(cloudContext, "gpu", "gpu-orig-flavor", "Standard_NV4as_v4", "QuotaExceeded");
        verify(instanceTypeFallbackReporter).reportFallbackExhausted(cloudContext, "gpu", "gpu-orig-flavor", "QuotaExceeded");
    }

    @Test
    void rethrowsWrapperImmediatelyWhenUnderlyingIsNotCapacityException() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        ManagementException unrelated = capacityException("AuthorizationFailed");
        InsufficientCapacityException wrapper = new InsufficientCapacityException(unrelated);
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(wrapper);
        when(retryExceptionMatcher.isInstanceTypeNotSupported(unrelated)).thenReturn(false);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        assertSame(wrapper, thrown);
        verify(azureClient, times(1)).createTemplateDeployment(anyString(), anyString(), anyString(), anyString());
        verify(azureStackView, never()).applyFlavorOverrides(any());
    }

    @Test
    void rethrowsInsufficientCapacityWithoutUnwrappingWhenCauseIsNotManagementException() {
        when(entitlementService.isFallbackInstanceTypeEnabled(ACCOUNT_ID)).thenReturn(true);
        List<Group> groups = List.of(group("master", List.of("Standard_D8s_v5")));
        when(cloudStack.getGroups()).thenReturn(groups);
        InsufficientCapacityException wrapper = new InsufficientCapacityException(new RuntimeException("something else"));
        when(azureClient.createTemplateDeployment(RG, STACK_NAME, INITIAL_TEMPLATE, PARAMETERS)).thenThrow(wrapper);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.createTemplateDeploymentWithFallback(request()));

        assertSame(wrapper, thrown);
        verify(azureClient, times(1)).createTemplateDeployment(anyString(), anyString(), anyString(), anyString());
        verify(azureStackView, never()).applyFlavorOverrides(any());
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
        return group(name, name + "-orig-flavor", fallbackTypes);
    }

    private Group group(String name, String originalFlavor, List<String> fallbackTypes) {
        Group group = mock(Group.class);
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(group.getName()).thenReturn(name);
        when(group.getReferenceInstanceTemplate()).thenReturn(template);
        when(template.getFallbackInstanceTypes()).thenReturn(fallbackTypes);
        when(template.getFlavor()).thenReturn(originalFlavor);
        return group;
    }

    private ManagementException capacityException(String code) {
        ManagementError topLevel = new ManagementError(code, "boom");
        return new ManagementException("Provisioning failed", null, topLevel);
    }
}
