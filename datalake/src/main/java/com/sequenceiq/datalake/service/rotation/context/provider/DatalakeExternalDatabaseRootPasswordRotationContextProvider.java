package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.DatalakeSecretType.DATALAKE_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.context.CloudbreakPollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.context.RedbeamsPollerRotationContext;

@Component
public class DatalakeExternalDatabaseRootPasswordRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(REDBEAMS_ROTATE_POLLING, new RedbeamsPollerRotationContext(resourceCrn, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        context.put(CLOUDBREAK_ROTATE_POLLING, new CloudbreakPollerRotationContext(resourceCrn, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD));
        return context;
    }

    @Override
    public SecretType getSecret() {
        return DATALAKE_DATABASE_ROOT_PASSWORD;
    }
}
