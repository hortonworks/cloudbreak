package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_INTERMEDIATE_CA_CERT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

@Component
public class DatalakeCMIntermediateCacertRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT));
    }

    @Override
    public SecretType getSecret() {
        return DATALAKE_CM_INTERMEDIATE_CA_CERT;
    }
}
