package com.sequenceiq.cloudbreak.service.identitymapping;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class AwsMockAccountMappingService {

    private static final String FIXED_IAM_ROLE = "arn:${partition}:iam::${accountId}:role/mock-idbroker-admin-role";

    private static final Map<String, String> MOCK_IDBROKER_USER_MAPPINGS = AccountMappingSubject.ALL_SPECIAL_USERS
            .stream()
            .map(user -> Map.entry(user, FIXED_IAM_ROLE))
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public AwsMockAccountMappingService(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public Map<String, String> getGroupMappings(String region, CloudCredential credential, String adminGroupName) {
        String accountId = getAccountId(region, credential);
        String partition = getPartition(credential);
        if (StringUtils.isNotEmpty(adminGroupName)) {
            Map<String, String> mapping = replaceAccountId(getGroupMappings(adminGroupName), accountId);
            return replacePartition(mapping, partition);
        } else {
            throw new CloudbreakServiceException(String.format("Failed to get group mappings because of missing adminGroupName for accountId: %s",
                    accountId));
        }
    }

    public Map<String, String> getUserMappings(String region, CloudCredential credential) {
        String accountId = getAccountId(region, credential);
        String partition = getPartition(credential);
        Map<String, String> mapping = replaceAccountId(MOCK_IDBROKER_USER_MAPPINGS, accountId);
        return replacePartition(mapping, partition);
    }

    private String getPartition(CloudCredential credential) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(credential);
        return awsCredentialView.isGovernmentCloudEnabled() ? "aws-us-gov" : "aws";
    }

    private String getAccountId(String region, CloudCredential credential) {
        IdentityService identityService = getIdentityService(AWS);
        return identityService.getAccountId(region, credential);
    }

    private IdentityService getIdentityService(String platform) {
        return cloudPlatformConnectors.get(Platform.platform(platform), Variant.variant(platform)).identityService();
    }

    private Map<String, String> replaceAccountId(Map<String, String> mappings, String accountId) {
        return mappings.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().replace("${accountId}", accountId)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> replacePartition(Map<String, String> mappings, String partition) {
        return mappings.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().replace("${partition}", partition)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> getGroupMappings(String adminGroupName) {
        return Map.ofEntries(
                Map.entry(adminGroupName, FIXED_IAM_ROLE)
        );
    }

}
