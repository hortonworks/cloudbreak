package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class DatahubCMIntermediateCacertRotationContextProvider extends AbstractCMIntermediateCacertRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return CM_INTERMEDIATE_CA_CERT;
    }
}
