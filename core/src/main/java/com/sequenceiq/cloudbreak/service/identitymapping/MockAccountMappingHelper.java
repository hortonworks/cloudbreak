package com.sequenceiq.cloudbreak.service.identitymapping;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;

@Component
public class MockAccountMappingHelper {
    @Inject
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Inject
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Inject
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public AccountMappingView getMockAccountMapping(String cloudPlatform, String region, Credential credential, String virtualGroup) {
        Map<String, String> groupMappings;
        Map<String, String> userMappings;
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        switch (cloudPlatform) {
            case AWS -> {
                groupMappings = awsMockAccountMappingService.getGroupMappings(region, cloudCredential, virtualGroup);
                userMappings = awsMockAccountMappingService.getUserMappings(region, cloudCredential);
            }
            case AZURE -> {
                groupMappings = azureMockAccountMappingService.getGroupMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                        cloudCredential, virtualGroup);
                userMappings = azureMockAccountMappingService.getUserMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                        cloudCredential);
            }
            case GCP -> {
                groupMappings = gcpMockAccountMappingService.getGroupMappings(region, cloudCredential, virtualGroup);
                userMappings = gcpMockAccountMappingService.getUserMappings(region, cloudCredential);
            }
            case null, default -> {
                groupMappings = null;
                userMappings = null;
            }
        }
        return (groupMappings != null && userMappings != null) ? new AccountMappingView(groupMappings, userMappings) : null;
    }
}
