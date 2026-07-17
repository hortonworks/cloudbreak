package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;

@ExtendWith(MockitoExtension.class)
class MockAccountMappingHelperTest {
    private static final String REGION = "region-1";

    private static final String ADMIN_GROUP_NAME = "mockAdmins";

    private static final Map<String, String> MOCK_GROUP_MAPPINGS = Map.of(ADMIN_GROUP_NAME, "mockGroupRole");

    private static final Map<String, String> MOCK_USER_MAPPINGS = Map.of("mockUser", "mockUserRole");

    @Mock
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Mock
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Mock
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private MockAccountMappingHelper underTest;

    @Test
    void test() {
        Credential credential = Credential.builder()
                .crn("aCredentialCRN")
                .attributes(new Json(""))
                .build();

        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        lenient().when(awsMockAccountMappingService.getGroupMappings(REGION, cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(awsMockAccountMappingService.getUserMappings(REGION, cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        lenient().when(azureMockAccountMappingService.getGroupMappings("msi", cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(azureMockAccountMappingService.getUserMappings("msi", cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        lenient().when(gcpMockAccountMappingService.getGroupMappings(REGION, cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(gcpMockAccountMappingService.getUserMappings(REGION, cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);

        AccountMappingView actualMappingAws = underTest.getMockAccountMapping(CloudConstants.AWS, REGION, credential, ADMIN_GROUP_NAME);
        AccountMappingView actualMappingAzure = underTest.getMockAccountMapping(CloudConstants.AZURE, "msi", credential, ADMIN_GROUP_NAME);
        AccountMappingView actualMappingGcp = underTest.getMockAccountMapping(CloudConstants.GCP, REGION, credential, ADMIN_GROUP_NAME);
        AccountMappingView actualMappingNullProvider = underTest.getMockAccountMapping(null, REGION, credential, ADMIN_GROUP_NAME);

        assertThat(actualMappingAws).isNotNull();
        assertThat(actualMappingAws.getGroupMappings()).isEqualTo(MOCK_GROUP_MAPPINGS);
        assertThat(actualMappingAws.getUserMappings()).isEqualTo(MOCK_USER_MAPPINGS);

        assertThat(actualMappingAzure).isNotNull();
        assertThat(actualMappingAzure.getGroupMappings()).isEqualTo(MOCK_GROUP_MAPPINGS);
        assertThat(actualMappingAzure.getUserMappings()).isEqualTo(MOCK_USER_MAPPINGS);

        assertThat(actualMappingGcp).isNotNull();
        assertThat(actualMappingGcp.getGroupMappings()).isEqualTo(MOCK_GROUP_MAPPINGS);
        assertThat(actualMappingGcp.getUserMappings()).isEqualTo(MOCK_USER_MAPPINGS);

        assertThat(actualMappingNullProvider).isNull();
    }
}
