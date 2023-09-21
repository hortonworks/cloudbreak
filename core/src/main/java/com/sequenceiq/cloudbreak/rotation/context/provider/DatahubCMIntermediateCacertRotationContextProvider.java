package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_INTERMEDIATE_CA_CERT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class DatahubCMIntermediateCacertRotationContextProvider extends AbstractCMIntermediateCacertRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return DATAHUB_CM_INTERMEDIATE_CA_CERT;
    }
}
