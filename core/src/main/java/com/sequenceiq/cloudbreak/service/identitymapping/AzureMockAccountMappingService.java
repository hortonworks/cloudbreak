package com.sequenceiq.cloudbreak.service.identitymapping;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class AzureMockAccountMappingService {

    public static final String MSI_RESOURCE_GROUP_NAME = "msi";

    private static final String SUBSCRIPTION_ID_KEY = "subscriptionId";

    private static final String SUBSCRIPTION_ID_PLACEHOLDER = "${subscriptionId}";

    private static final String RESOURCEGROUP_PLACEHOLDER = "${resourceGroupId}";

    private static final String FIXED_MANAGED_IDENTITY = "/subscriptions/${subscriptionId}/resourceGroups/${resourceGroupId}/" +
            "providers/Microsoft.ManagedIdentity/userAssignedIdentities/mock-idbroker-admin-identity";

    private static final Map<String, String> MOCK_IDBROKER_USER_MAPPINGS = AccountMappingSubject.ALL_SPECIAL_USERS
            .stream()
            .map(user -> Map.entry(user, FIXED_MANAGED_IDENTITY))
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

    public Map<String, String> getGroupMappings(String resourceGroup, CloudCredential credential, String adminGroupName) {
        String subscriptionId = getSubscriptionId(credential);
        if (StringUtils.isNotEmpty(adminGroupName)) {
            return replacePlaceholders(getGroupMappings(adminGroupName), resourceGroup, subscriptionId);
        } else {
            throw new CloudbreakServiceException("Failed to get group mappings because of missing adminGroupName");
        }
    }

    private String getSubscriptionId(CloudCredential credential) {
        String subscriptionId = (String) credential
                .getParameter(AZURE.toLowerCase(), Map.class)
                .get(SUBSCRIPTION_ID_KEY);
        return subscriptionId;
    }

    public Map<String, String> getUserMappings(String resourceGroup, CloudCredential credential) {
        String subscriptionId = getSubscriptionId(credential);
        return replacePlaceholders(MOCK_IDBROKER_USER_MAPPINGS, resourceGroup, subscriptionId);
    }

    private Map<String, String> replacePlaceholders(Map<String, String> mappings, String resourceGroup, String subscriptionId) {
        return mappings.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().
                        replace(SUBSCRIPTION_ID_PLACEHOLDER, subscriptionId).
                        replace(RESOURCEGROUP_PLACEHOLDER, resourceGroup)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> getGroupMappings(String adminGroupName) {
        return Map.ofEntries(
                Map.entry(adminGroupName, FIXED_MANAGED_IDENTITY)
        );
    }

}
