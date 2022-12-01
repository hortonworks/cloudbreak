package com.sequenceiq.environment.environment.dto.credential;

import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.AWS_KEY_BASED;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.AWS_ROLE_BASED;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.UNKNOWN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;

@Component
public class AwsCredentialDetailsConverter implements CloudPlatformAwareCredentialDetailsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialDetailsConverter.class);

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public CredentialDetails.Builder convertCredentialDetails(CredentialAttributes credentialAttributes, CredentialDetails.Builder builder) {
        return builder.withCredentialType(getAwsCredentialType(credentialAttributes.getAws()));
    }

    private CredentialType getAwsCredentialType(AwsCredentialAttributes awsCredentialAttributes) {
        CredentialType credentialType = UNKNOWN;
        if (awsCredentialAttributes != null) {
            if (awsCredentialAttributes.getRoleBased() != null) {
                credentialType = AWS_ROLE_BASED;
            } else if (awsCredentialAttributes.getKeyBased() != null) {
                credentialType = AWS_KEY_BASED;
            }
        } else {
            LOGGER.debug("Credential type cannot be determined as awsCredentialAttributes is null");
        }
        return credentialType;
    }
}
