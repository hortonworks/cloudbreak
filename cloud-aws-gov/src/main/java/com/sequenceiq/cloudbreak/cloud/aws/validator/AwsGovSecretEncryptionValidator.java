package com.sequenceiq.cloudbreak.cloud.aws.validator;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CommonSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AwsGovSecretEncryptionValidator extends CommonSecretEncryptionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGovSecretEncryptionValidator.class);

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        if (secretEncryptionEnabled(cloudStack)) {
            String accountId = ac.getCloudCredential().getAccountId();
            if (entitlementService.isSecretEncryptionEnabled(accountId)) {
                LOGGER.info("Secret encryption is available for platform variant " + ac.getCloudContext().getPlatformVariant());
            } else {
                throw new CloudConnectorException(String.format("Account '%s' is not entitled to use secret encryption.", accountId));
            }
        }
    }
}
