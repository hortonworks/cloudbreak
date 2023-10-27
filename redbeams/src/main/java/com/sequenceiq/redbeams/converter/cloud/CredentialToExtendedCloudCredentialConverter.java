package com.sequenceiq.redbeams.converter.cloud;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.redbeams.dto.Credential;

@Component
public class CredentialToExtendedCloudCredentialConverter {

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private EntitlementService entitlementService;

    public ExtendedCloudCredential convert(Credential credential, String cloudPlatform) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        return new ExtendedCloudCredential(
                cloudCredential,
                cloudPlatform,
                "",
                cloudCredential.getAccountId(),
                entitlementService.getEntitlements(cloudCredential.getAccountId()));
    }
}
