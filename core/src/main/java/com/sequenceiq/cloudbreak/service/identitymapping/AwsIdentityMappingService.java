package com.sequenceiq.cloudbreak.service.identitymapping;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class AwsIdentityMappingService {

    private static final Map<String, String> MOCK_IDBROKER_GROUP_MAPPING = Map.ofEntries(
            Map.entry("admins", "arn:aws:iam::${accountId}:role/mock-idbroker-admin-role")
    );

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public Map<String, String> getIdentityGroupMapping(Credential credential) {
        IdentityService identityService = cloudPlatformConnectors.get(Platform.platform(credential.cloudPlatform()),
                Variant.variant(credential.cloudPlatform())).identityService();
        String accountId = identityService.getAccountId(credentialConverter.convert(credential));
        return MOCK_IDBROKER_GROUP_MAPPING.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().replace("${accountId}", accountId)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
}
