package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

class AzureMockAccountMappingServiceTest {

    private static final String ADMIN_GROUP_NAME = "adminGroupName";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private static final String RESOURCE_GROUP = "resourceGroup";

    private static final String MANAGED_IDENTITY = "/subscriptions/" + SUBSCRIPTION_ID + "/resourceGroups/" + RESOURCE_GROUP +
            "/providers/Microsoft.ManagedIdentity/userAssignedIdentities/mock-idbroker-admin-identity";

    private AzureMockAccountMappingService underTest;

    private CloudCredential credential;

    @BeforeEach
    void setUp() {
        underTest = new AzureMockAccountMappingService();
        Map<String, Object> attributes = Map.of("azure", Map.of("subscriptionId", SUBSCRIPTION_ID));
        credential = new CloudCredential("id", "name", attributes, false);
    }

    @Test
    void testGetUserMappings() {
        Map<String, String> userMappings = underTest.getUserMappings(RESOURCE_GROUP, credential);
        assertThat(userMappings).isNotNull();
        AccountMappingSubject.ALL_SPECIAL_USERS.forEach(user -> assertThat(userMappings).contains(Map.entry(user, MANAGED_IDENTITY)));
        assertThat(userMappings).hasSize(AccountMappingSubject.ALL_SPECIAL_USERS.size());
    }

    @Test
    void testGetGroupMappingsWhenSuccess() {
        Map<String, String> groupMappings = underTest.getGroupMappings(RESOURCE_GROUP, credential, ADMIN_GROUP_NAME);
        assertThat(groupMappings).isNotNull();
        assertThat(groupMappings).containsOnly(Map.entry(ADMIN_GROUP_NAME, MANAGED_IDENTITY));
    }

    @Test
    void testGetGroupMappingsWhenAdminGroupIsMissing() {
        assertThrows(CloudbreakServiceException.class, () -> underTest.getGroupMappings(RESOURCE_GROUP, credential, null));
    }

}