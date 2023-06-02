package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.DatalakeSecretType.DATALAKE_CB_CM_ADMIN_PASSWORD;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.context.CloudbreakPollerRotationContext;

@Component
public class DatalakeCMAdminPasswordRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CLOUDBREAK_ROTATE_POLLING, new CloudbreakPollerRotationContext(resourceCrn, CLUSTER_CB_CM_ADMIN_PASSWORD));
    }

    @Override
    public SecretType getSecret() {
        return DATALAKE_CB_CM_ADMIN_PASSWORD;
    }
}
