package com.sequenceiq.cloudbreak.cloud.aws.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AwsGovSecretEncryptionValidator extends CommonSecretEncryptionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGovSecretEncryptionValidator.class);

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        LOGGER.info("Secret encryption is always enabled for platform variant " + getCloudPlatformVariantString(ac));
    }
}
