package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class CommonSecretEncryptionValidator implements Validator {

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        if (secretEncryptionEnabled(cloudStack)) {
            throw new CloudConnectorException("Secret encryption is not supported for platform variant" + getCloudPlatformVariantString(ac));
        }
    }

    protected boolean secretEncryptionEnabled(CloudStack cloudStack) {
        return Optional.ofNullable(cloudStack)
                .map(CloudStack::getParameters)
                .map(params -> params.get(SECRET_ENCRYPTION_ENABLED))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    protected String getCloudPlatformVariantString(AuthenticatedContext ac) {
        return Optional.ofNullable(ac)
                .map(AuthenticatedContext::getCloudContext)
                .map(CloudContext::getPlatformVariant)
                .map(CloudPlatformVariant::toString)
                .orElse("unknown");
    }
}
