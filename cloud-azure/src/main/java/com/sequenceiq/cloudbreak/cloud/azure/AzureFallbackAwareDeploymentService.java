package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureInstanceTypeRetryExceptionMatcher;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.notification.InstanceTypeFallbackReporter;

@Service
public class AzureFallbackAwareDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFallbackAwareDeploymentService.class);

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureInstanceTypeRetryExceptionMatcher retryExceptionMatcher;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private InstanceTypeFallbackReporter instanceTypeFallbackReporter;

    public Deployment createTemplateDeploymentWithFallback(AzureTemplateDeploymentRequest request) {
        String accountId = request.cloudContext().getAccountId();
        String resourceGroupName = request.resourceGroupName();
        String stackName = request.stackName();
        String parameters = request.parameters();
        if (!entitlementService.isFallbackInstanceTypeEnabled(accountId)) {
            LOGGER.debug("Instance type fallback is disabled for account {}, submitting template deployment as-is.", accountId);
            return request.client().createTemplateDeployment(resourceGroupName, stackName, request.initialTemplate(), parameters);
        }
        Map<String, List<String>> fallbackChains = collectFallbackChains(request.cloudStack());
        if (fallbackChains.isEmpty()) {
            LOGGER.debug("No fallback instance types configured on stack {}, submitting template deployment as-is.", stackName);
            return request.client().createTemplateDeployment(resourceGroupName, stackName, request.initialTemplate(), parameters);
        }
        Map<String, String> originalFlavors = collectOriginalFlavors(request.cloudStack());
        Map<String, Integer> nextFallbackIndex = new HashMap<>();
        String template = request.initialTemplate();
        RuntimeException lastException = null;
        Map<String, String> vmToSkuFamily = getVmToSkuFamilies(request);
        for (int attempt = 0; attempt < getMaxAttemptFromFallbackQueueLength(fallbackChains); attempt++) {
            try {
                Deployment deployment = request.client().createTemplateDeployment(resourceGroupName, stackName, template, parameters);
                if (attempt > 0) {
                    LOGGER.info("Template deployment {}/{} succeeded on fallback attempt {}.", resourceGroupName, stackName, attempt);
                }
                return deployment;
            } catch (ManagementException e) {
                lastException = e;
                template = handleDeploymentFailureAndRebuildTemplate(e, e, request, fallbackChains, nextFallbackIndex, vmToSkuFamily, originalFlavors);
            } catch (InsufficientCapacityException ice) {
                if (!(ice.getCause() instanceof ManagementException underlying)) {
                    throw ice;
                }
                lastException = ice;
                template = handleDeploymentFailureAndRebuildTemplate(underlying, ice, request, fallbackChains, nextFallbackIndex, vmToSkuFamily,
                        originalFlavors);
            }
        }
        if (lastException != null) {
            LOGGER.info("Azure fallback deployment loop terminated with exception.", lastException);
            ManagementException underlyingForNotification = underlyingManagementException(lastException);
            String reasonSummary = retryExceptionMatcher.getAzureErrorCodeForNotification(underlyingForNotification);
            fallbackChains.keySet().forEach(groupName -> instanceTypeFallbackReporter.reportFallbackExhausted(request.cloudContext(), groupName,
                    originalFlavors.get(groupName), reasonSummary));
            throw lastException;
        }
        throw new IllegalStateException("Azure fallback deployment loop terminated without a result");
    }

    private String handleDeploymentFailureAndRebuildTemplate(ManagementException underlying, RuntimeException toRethrow,
            AzureTemplateDeploymentRequest request, Map<String, List<String>> fallbackChains, Map<String, Integer> nextFallbackIndex,
            Map<String, String> vmToSkuFamily, Map<String, String> originalFlavors) {
        Map<String, String> nextFlavors = resolveNextFallbackFlavorsOrThrow(underlying, toRethrow, request, fallbackChains, nextFallbackIndex,
                vmToSkuFamily, originalFlavors);
        LOGGER.info("Template deployment {}/{} failed with capacity-style error; retrying with fallback flavors {}.",
                request.resourceGroupName(), request.stackName(), nextFlavors);
        String reasonSummary = retryExceptionMatcher.getAzureErrorCodeForNotification(underlying);
        nextFlavors.forEach((groupName, newFlavor) -> instanceTypeFallbackReporter.reportFallback(request.cloudContext(), groupName,
                originalFlavors.get(groupName), newFlavor, reasonSummary));
        request.azureStackView().applyFlavorOverrides(nextFlavors);
        return azureTemplateBuilder.build(request.stackName(), request.customImageId(), request.credentialView(), request.azureStackView(),
                request.cloudContext(), request.cloudStack(), request.operation(), request.azureMarketplaceImage());
    }

    private ManagementException underlyingManagementException(RuntimeException exception) {
        if (exception instanceof ManagementException me) {
            return me;
        }
        return exception.getCause() instanceof ManagementException cause ? cause : null;
    }

    private Map<String, String> collectOriginalFlavors(CloudStack cloudStack) {
        Map<String, String> flavors = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            flavors.put(group.getName(), group.getReferenceInstanceTemplate().getFlavor());
        }
        return flavors;
    }

    private String getRegion(AzureTemplateDeploymentRequest request) {
        CloudContext cloudContext = request.cloudContext();
        return cloudContext.getLocation() != null && cloudContext.getLocation().getRegion() != null
                ? cloudContext.getLocation().getRegion().value()
                : null;
    }

    private int getMaxAttemptFromFallbackQueueLength(Map<String, List<String>> fallbackChains) {
        return 1 + fallbackChains.values().stream().mapToInt(List::size).sum();
    }

    private Map<String, String> resolveNextFallbackFlavorsOrThrow(ManagementException underlying, RuntimeException toRethrow,
            AzureTemplateDeploymentRequest request, Map<String, List<String>> fallbackChains, Map<String, Integer> nextFallbackIndex,
            Map<String, String> vmToSkuFamily, Map<String, String> originalFlavors) {
        String resourceGroupName = request.resourceGroupName();
        String stackName = request.stackName();
        if (!retryExceptionMatcher.isInstanceTypeNotSupported(underlying)) {
            LOGGER.debug("Template deployment {}/{} failed with non-capacity error, no fallback retry.", resourceGroupName, stackName);
            throw toRethrow;
        }
        Set<String> failingGroups = retryExceptionMatcher.findGroupsWithCapacityFailure(resourceGroupName, stackName, stackName,
                request.azureStackView().getInstanceGroupNames(), request.client());
        if (failingGroups.isEmpty()) {
            failingGroups = attributeQuotaFailure(underlying, request, fallbackChains, vmToSkuFamily);
        }
        if (failingGroups.isEmpty()) {
            LOGGER.warn("Template deployment {}/{} failed with a capacity-style error but no failing VM operation was attributable to a group; "
                    + "rethrowing.", resourceGroupName, stackName);
            throw toRethrow;
        }
        Map<String, String> nextFlavors = pickNextFallbackFlavors(failingGroups, fallbackChains, nextFallbackIndex);
        if (nextFlavors.isEmpty()) {
            LOGGER.warn("Template deployment {}/{} failed and all fallback instance types are exhausted for failing groups {}; rethrowing.",
                    resourceGroupName, stackName, failingGroups);
            String reasonSummary = retryExceptionMatcher.getAzureErrorCodeForNotification(underlying);
            failingGroups.forEach(groupName -> instanceTypeFallbackReporter.reportFallbackExhausted(request.cloudContext(), groupName,
                    originalFlavors.get(groupName), reasonSummary));
            throw toRethrow;
        }
        return nextFlavors;
    }

    private Map<String, List<String>> collectFallbackChains(CloudStack cloudStack) {
        Map<String, List<String>> chains = new LinkedHashMap<>();
        for (Group group : cloudStack.getGroups()) {
            List<String> fallbacks = group.getReferenceInstanceTemplate().getFallbackInstanceTypes();
            if (fallbacks != null && !fallbacks.isEmpty()) {
                chains.put(group.getName(), fallbacks);
            }
        }
        return chains;
    }

    private Set<String> attributeQuotaFailure(ManagementException underlying, AzureTemplateDeploymentRequest request,
            Map<String, List<String>> fallbackChains, Map<String, String> vmToSkuFamily) {
        if (!retryExceptionMatcher.isQuotaCodePresent(underlying)) {
            return Set.of();
        }
        LOGGER.info("Template deployment {}/{} failed with quota error; no failed VM operations — attempting family-based group attribution for region {}.",
                request.resourceGroupName(), request.stackName(), getRegion(request));
        Set<String> groupsByFamily = retryExceptionMatcher.findGroupsWithQuotaFailure(underlying, vmToSkuFamily, request.cloudStack().getGroups());
        if (!groupsByFamily.isEmpty()) {
            return groupsByFamily;
        }
        if (!fallbackChains.isEmpty()) {
            LOGGER.warn("Template deployment {}/{} failed with a quota-style error but no group attribution succeeded; "
                    + "bumping every group with a fallback configured: {}.",
                    request.resourceGroupName(), request.stackName(), fallbackChains.keySet());
            return fallbackChains.keySet();
        }
        return Set.of();
    }

    private Map<String, String> pickNextFallbackFlavors(Set<String> failingGroups, Map<String, List<String>> fallbackChains,
            Map<String, Integer> nextFallbackIndex) {
        Map<String, String> next = new HashMap<>();
        for (String groupName : failingGroups) {
            List<String> chain = fallbackChains.get(groupName);
            if (chain == null || chain.isEmpty()) {
                continue;
            }
            int index = nextFallbackIndex.getOrDefault(groupName, 0);
            if (index >= chain.size()) {
                continue;
            }
            next.put(groupName, chain.get(index));
            nextFallbackIndex.put(groupName, index + 1);
        }
        return next;
    }

    Map<String, String> getVmToSkuFamilies(AzureTemplateDeploymentRequest request) {
        String region = getRegion(request);
        try {
            return request.client().getVmToSkuFamilies(region);
        } catch (Exception ex) {
            LOGGER.warn("Could not fetch VM SKU family map for region {}; family-based attribution disabled.", region, ex);
        }
        return Map.of();
    }
}
