package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.sdx.rotation.DatalakeMultiSecretType;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeCMServiceSharedDatabaseRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB));
        return context;
    }

    @Override
    public Optional<MultiSecretType> getMultiSecret() {
        return Optional.of(DatalakeMultiSecretType.CM_SERVICE_SHARED_DB);
    }

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.DATALAKE_CM_SERVICE_SHARED_DB;
    }
}
