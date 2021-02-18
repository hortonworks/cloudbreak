package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@ExtendWith(MockitoExtension.class)
class AwsMockAccountMappingServiceTest {

    private static final String REGION = "region";

    private static final String ADMIN_GROUP_NAME = "adminGroupName";

    private static final String ACCOUNT_ID = "accountId";

    private static final String IAM_ROLE = "arn:aws:iam::" + ACCOUNT_ID + ":role/mock-idbroker-admin-role";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private AwsMockAccountMappingService underTest;

    private Credential credential;

    @BeforeEach
    void setUp() {
        credential = Credential.builder().cloudPlatform(CloudPlatform.AWS.name()).build();
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(any(Platform.class), any(Variant.class))).thenReturn(cloudConnector);
        when(cloudConnector.identityService()).thenReturn(identityService);
        when(identityService.getAccountId(REGION, cloudCredential)).thenReturn(ACCOUNT_ID);
    }

    @Test
    void testGetUserMappings() {
        Map<String, String> userMappings = underTest.getUserMappings(REGION, credential);
        assertThat(userMappings).isNotNull();
        AccountMappingSubject.ALL_SPECIAL_USERS.forEach(user -> assertThat(userMappings).contains(Map.entry(user, IAM_ROLE)));
        assertThat(userMappings).hasSize(AccountMappingSubject.ALL_SPECIAL_USERS.size());
    }

    @Test
    void testGetGroupMappingsWhenSuccess() {
        Map<String, String> groupMappings = underTest.getGroupMappings(REGION, credential, ADMIN_GROUP_NAME);
        assertThat(groupMappings).isNotNull();
        assertThat(groupMappings).containsOnly(Map.entry(ADMIN_GROUP_NAME, IAM_ROLE));
    }

    @Test
    void testGetGroupMappingsWhenAdminGroupIsMissing() {
        assertThrows(CloudbreakServiceException.class, () -> underTest.getGroupMappings(REGION, credential, null));
    }

}