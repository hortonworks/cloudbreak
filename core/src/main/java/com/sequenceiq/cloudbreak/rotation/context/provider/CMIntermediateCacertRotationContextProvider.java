package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class CMIntermediateCacertRotationContextProvider extends AbstractCMHostCertRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return CM_INTERMEDIATE_CA_CERT;
    }

    @Override
    protected String getRotationTypeMessage() {
        return "CMCA rotation (alongside with private host certificates' rotation)";
    }
}
