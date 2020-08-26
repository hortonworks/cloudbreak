package com.sequenceiq.cloudbreak.service.identitymapping;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class AzureMockAccountMappingService {

    public static final String MSI_RESOURCE_GROUP_NAME = "msi";

    private static final String FIXED_MANAGED_IDENTITY = "/subscriptions/${subscriptionId}/resourceGroups/${resourceGroupId}/" +
            "providers/Microsoft.ManagedIdentity/userAssignedIdentities/mock-idbroker-admin-identity";

    private static final Map<String, String> MOCK_IDBROKER_USER_MAPPINGS = AccountMappingSubject.ALL_SPECIAL_USERS
            .stream()
            .map(user -> Map.entry(user, FIXED_MANAGED_IDENTITY))
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

    public Map<String, String> getGroupMappings(String resourceGroup, Credential credential, String adminGroupName) {
        String subscriptionId = credential.getAzure().getSubscriptionId();
        if (StringUtils.isNotEmpty(adminGroupName)) {
            return replacePlaceholders(getGroupMappings(adminGroupName), resourceGroup, subscriptionId);
        } else {
            throw new CloudbreakServiceException("Failed to get group mappings because of missing adminGroupName");
        }
    }

    public Map<String, String> getUserMappings(String resourceGroup, Credential credential) {
        String subscriptionId = credential.getAzure().getSubscriptionId();
        return replacePlaceholders(MOCK_IDBROKER_USER_MAPPINGS, resourceGroup, subscriptionId);
    }

    private Map<String, String> replacePlaceholders(Map<String, String> mappings, String resourceGroup, String subscriptionId) {
        return mappings.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().
                        replace("${subscriptionId}", subscriptionId).
                        replace("${resourceGroupId}", resourceGroup)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> getGroupMappings(String adminGroupName) {
        return Map.ofEntries(
                Map.entry(adminGroupName, FIXED_MANAGED_IDENTITY)
        );
    }

}
