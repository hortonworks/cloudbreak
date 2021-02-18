package com.sequenceiq.cloudbreak.service.identitymapping;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class GcpMockAccountMappingService {

    private static final String FIXED_SERVICE_ACCOUNT_ROLE = "mock-idbroker-admin-role@${projectId}.iam.gserviceaccount.com";

    private static final Map<String, String> MOCK_IDBROKER_USER_MAPPINGS = AccountMappingSubject.ALL_SPECIAL_USERS
            .stream()
            .map(user -> Map.entry(user, FIXED_SERVICE_ACCOUNT_ROLE))
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialConverter;

    public GcpMockAccountMappingService(CloudPlatformConnectors cloudPlatformConnectors, CredentialToCloudCredentialConverter credentialConverter) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialConverter = credentialConverter;
    }

    public Map<String, String> getGroupMappings(String region, Credential credential, String adminGroupName) {
        String projectId = getProjectId(region, credential);
        if (StringUtils.isNotEmpty(adminGroupName)) {
            return replaceProjectName(getGroupMappings(adminGroupName), projectId);
        } else {
            throw new CloudbreakServiceException(String.format("Failed to get group mappings because of missing adminGroupName for getProjectId: %s",
                    projectId));
        }
    }

    public Map<String, String> getUserMappings(String region, Credential credential) {
        String projectName = getProjectId(region, credential);
        return replaceProjectName(MOCK_IDBROKER_USER_MAPPINGS, projectName);
    }

    private String getProjectId(String region, Credential credential) {
        IdentityService identityService = getIdentityService(credential.cloudPlatform());
        return identityService.getAccountId(region, credentialConverter.convert(credential));
    }

    private IdentityService getIdentityService(String platform) {
        return cloudPlatformConnectors.get(Platform.platform(platform), Variant.variant(platform)).identityService();
    }

    private Map<String, String> replaceProjectName(Map<String, String> mappings, String projectId) {
        return mappings.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().replace("${projectId}", projectId)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> getGroupMappings(String adminGroupName) {
        return Map.ofEntries(
                Map.entry(adminGroupName, FIXED_SERVICE_ACCOUNT_ROLE)
        );
    }

}
