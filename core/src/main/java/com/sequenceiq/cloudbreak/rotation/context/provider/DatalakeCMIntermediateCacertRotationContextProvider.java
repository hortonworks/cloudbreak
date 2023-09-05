package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

@Component
public class DatalakeCMIntermediateCacertRotationContextProvider extends AbstractCMIntermediateCacertRotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(SALT_STATE_APPLY, getSaltStateRotationContext(resourceCrn).build());
    }

    @Override
    public SecretType getSecret() {
        return INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT;
    }
}
