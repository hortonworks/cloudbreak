package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class DatalakeCMIntermediateCacertRotationContextProvider extends AbstractCMIntermediateCacertRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT;
    }
}
