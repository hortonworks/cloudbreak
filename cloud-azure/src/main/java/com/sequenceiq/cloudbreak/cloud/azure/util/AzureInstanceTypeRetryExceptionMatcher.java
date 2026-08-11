package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.azure.resourcemanager.resources.models.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDeploymentCapacityError;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Component
public class AzureInstanceTypeRetryExceptionMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceTypeRetryExceptionMatcher.class);

    private static final String VIRTUAL_MACHINES_RESOURCE_TYPE = "Microsoft.Compute/virtualMachines";

    private static final Set<String> CAPACITY_ERROR_CODES = Arrays.stream(AzureDeploymentCapacityError.values())
            .map(AzureDeploymentCapacityError::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> QUOTA_CODES = Set.of(
            AzureDeploymentCapacityError.QUOTA_EXCEEDED.getCode(),
            AzureDeploymentCapacityError.SUBSCRIPTION_QUOTA_REACHED.getCode(),
            AzureDeploymentCapacityError.OPERATION_NOT_ALLOWED.getCode());

    private static final Pattern SKU_FAMILY_PATTERN = Pattern.compile("\\b([Ss]tandard[A-Za-z0-9]+Family)\\b");

    public boolean isInstanceTypeNotSupported(ManagementException managementException) {
        if (managementException == null || managementException.getValue() == null) {
            return false;
        }
        return anyErrorMatches(managementException.getValue(), this::matchesCapacityCode);
    }

    public boolean isQuotaCodePresent(ManagementException managementException) {
        if (managementException == null || managementException.getValue() == null) {
            return false;
        }
        return anyErrorMatches(managementException.getValue(), this::matchesQuotaCode);
    }

    public Set<String> findGroupsWithCapacityFailure(String resourceGroupName, String deploymentName, String stackName,
            List<String> groupNames, AzureClient client) {
        try {
            return client.getTemplateDeploymentOperations(resourceGroupName, deploymentName).getAll().stream()
                    .filter(operation -> "Failed".equalsIgnoreCase(operation.provisioningState()))
                    .filter(operation -> isVirtualMachineResource(operation.targetResource()))
                    .filter(this::operationStatusMessageIsCapacityError)
                    .map(operation -> matchGroupForVm(operation.targetResource(), stackName, groupShortNameToGroupName(groupNames)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (Exception ex) {
            LOGGER.warn("Failed to inspect deployment operations of {}/{} to find groups with capacity failures.",
                    resourceGroupName, deploymentName, ex);
            return Collections.emptySet();
        }
    }

    public Set<String> findGroupsWithQuotaFailure(ManagementException e, Map<String, String> vmToSkuFamily, List<Group> groups) {
        if (!hasQuotaInputs(e, vmToSkuFamily, groups)) {
            return Collections.emptySet();
        }
        Set<String> families = extractFamiliesFromException(e);
        if (families.isEmpty()) {
            return Collections.emptySet();
        }
        LOGGER.info("findGroupsWithQuotaFailure: quota messages reference SKU families {}.", families);
        return matchGroupsByFamily(groups, vmToSkuFamily, families);
    }

    private boolean hasQuotaInputs(ManagementException managementException, Map<String, String> flavorToFamily, List<Group> groups) {
        return managementException != null && managementException.getValue() != null
                && flavorToFamily != null && !flavorToFamily.isEmpty()
                && groups != null && !groups.isEmpty();
    }

    private Set<String> extractFamiliesFromException(ManagementException managementException) {
        List<String> quotaMessages = flattenErrors(managementException.getValue()).stream()
                .filter(error -> matchesQuotaCode(error.getCode()))
                .map(ManagementError::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (quotaMessages.isEmpty()) {
            LOGGER.warn("findGroupsWithQuotaFailure: no quota detail messages found in the exception tree.");
            return Collections.emptySet();
        }
        Set<String> families = parseSkuFamilies(quotaMessages);
        if (families.isEmpty()) {
            LOGGER.warn("findGroupsWithQuotaFailure: no SKU family names could be parsed from quota messages: {}", quotaMessages);
        }
        return families;
    }

    private Set<String> matchGroupsByFamily(List<Group> groups, Map<String, String> vmToSkuFamily, Set<String> families) {
        Set<String> matched = new HashSet<>();
        for (Group group : groups) {
            String flavor = flavorOf(group);
            if (flavor == null) {
                continue;
            }
            String family = vmToSkuFamily.get(flavor);
            if (family != null && containsIgnoreCase(families, family)) {
                LOGGER.info("findGroupsWithQuotaFailure: group '{}' flavor '{}' belongs to quota-failed family '{}'.",
                        group.getName(), flavor, family);
                matched.add(group.getName());
            }
        }
        return Collections.unmodifiableSet(matched);
    }

    private static String flavorOf(Group group) {
        return group.getReferenceInstanceTemplate() != null ? group.getReferenceInstanceTemplate().getFlavor() : null;
    }

    private static boolean containsIgnoreCase(Set<String> families, String family) {
        return families.stream().anyMatch(family::equalsIgnoreCase);
    }

    private List<ManagementError> flattenErrors(ManagementError managementError) {
        if (managementError == null) {
            return Collections.emptyList();
        }
        List<ManagementError> result = new ArrayList<>();
        Deque<ManagementError> stack = new ArrayDeque<>();
        stack.push(managementError);
        while (!stack.isEmpty()) {
            ManagementError current = stack.pop();
            result.add(current);
            List<? extends ManagementError> children = Optional.ofNullable(current.getDetails()).orElse(Collections.emptyList());
            for (ManagementError child : children.reversed()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
        return result;
    }

    private boolean anyErrorMatches(ManagementError managementError, Predicate<String> codeMatcher) {
        return flattenErrors(managementError).stream().anyMatch(error -> codeMatcher.test(error.getCode()));
    }

    private Set<String> parseSkuFamilies(List<String> errorMessages) {
        Set<String> families = new HashSet<>();
        for (String errorMessage : errorMessages) {
            if (errorMessage == null) {
                continue;
            }
            Matcher skuFamilyPatternMatcher = SKU_FAMILY_PATTERN.matcher(errorMessage);
            while (skuFamilyPatternMatcher.find()) {
                families.add(skuFamilyPatternMatcher.group(1));
            }
        }
        return families;
    }

    private boolean matchesCapacityCode(String code) {
        return code != null && CAPACITY_ERROR_CODES.stream().anyMatch(capacityErrorCode -> capacityErrorCode.equalsIgnoreCase(code));
    }

    private boolean matchesQuotaCode(String code) {
        return code != null && QUOTA_CODES.stream().anyMatch(quotaErrorCode -> quotaErrorCode.equalsIgnoreCase(code));
    }

    private boolean isVirtualMachineResource(TargetResource targetResource) {
        return targetResource != null && VIRTUAL_MACHINES_RESOURCE_TYPE.equalsIgnoreCase(targetResource.resourceType());
    }

    private boolean operationStatusMessageIsCapacityError(DeploymentOperation deploymentOperation) {
        Object statusMessage = deploymentOperation.statusMessage();
        if (statusMessage == null) {
            return false;
        }
        String errorMessageText = statusMessage.toString();
        for (String capacityErrorCode : CAPACITY_ERROR_CODES) {
            if (errorMessageText.contains(capacityErrorCode)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> matchGroupForVm(TargetResource targetResource, String stackName, Map<String, String> shortNameToGroup) {
        String resourceName = targetResource.resourceName();
        if (resourceName == null) {
            return Optional.empty();
        }
        String suffix = stackName != null && resourceName.startsWith(stackName) ? resourceName.substring(stackName.length()) : resourceName;
        return shortNameToGroup.entrySet().stream()
                .filter(entry -> suffix.startsWith(entry.getKey()))
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue);
    }

    private Map<String, String> groupShortNameToGroupName(List<String> groupNames) {
        Map<String, String> map = new HashMap<>();
        for (String name : groupNames) {
            map.put(AzureUtils.getGroupName(name), name);
        }
        return map;
    }
}
