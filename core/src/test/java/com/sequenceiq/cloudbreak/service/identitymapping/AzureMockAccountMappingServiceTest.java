package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.dto.credential.azure.AzureCredentialAttributes;

class AzureMockAccountMappingServiceTest {

    private static final String ADMIN_GROUP_NAME = "adminGroupName";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private static final String RESOURCE_GROUP = "resourceGroup";

    private static final String MANAGED_IDENTITY = "/subscriptions/" + SUBSCRIPTION_ID + "/resourceGroups/" + RESOURCE_GROUP +
            "/providers/Microsoft.ManagedIdentity/userAssignedIdentities/mock-idbroker-admin-identity";

    private AzureMockAccountMappingService underTest;

    private Credential credential;

    @BeforeEach
    void setUp() {
        underTest = new AzureMockAccountMappingService();
        credential = Credential.builder()
                .cloudPlatform(CloudPlatform.AZURE.name())
                .azure(AzureCredentialAttributes.builder().subscriptionId(SUBSCRIPTION_ID).build())
                .build();
    }

    @Test
    void testGetUserMappings() {
        Map<String, String> userMappings = underTest.getUserMappings(RESOURCE_GROUP, credential);
        assertThat(userMappings).isNotNull();
        AccountMappingSubject.DATA_ACCESS_AND_RANGER_AUDIT_USERS.forEach(user -> assertThat(userMappings).contains(Map.entry(user, MANAGED_IDENTITY)));
        assertThat(userMappings).hasSize(AccountMappingSubject.DATA_ACCESS_AND_RANGER_AUDIT_USERS.size());
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