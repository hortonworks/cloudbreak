package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DATABASE_ROOT_PASSWORD;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

@Component
public class DatalakeExternalDatabaseRootPasswordRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(REDBEAMS_ROTATE_POLLING, new PollerRotationContext(resourceCrn, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        context.put(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD));
        return context;
    }

    @Override
    public SecretType getSecret() {
        return DATALAKE_DATABASE_ROOT_PASSWORD;
    }
}
