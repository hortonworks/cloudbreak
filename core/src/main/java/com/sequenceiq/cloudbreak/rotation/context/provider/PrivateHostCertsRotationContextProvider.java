package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.PRIVATE_HOST_CERTS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class PrivateHostCertsRotationContextProvider extends AbstractCMHostCertRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return PRIVATE_HOST_CERTS;
    }

    @Override
    protected String getRotationTypeMessage() {
        return "Private host certificates' rotation";
    }
}
