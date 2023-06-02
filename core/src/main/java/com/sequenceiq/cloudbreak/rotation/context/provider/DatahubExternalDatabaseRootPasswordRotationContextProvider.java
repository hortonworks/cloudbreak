package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.context.RedbeamsPollerRotationContext;

@Component
public class DatahubExternalDatabaseRootPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private DatabaseRootPasswordSaltPillarGenerator databaseRootPasswordSaltPillarGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        contexts.put(REDBEAMS_ROTATE_POLLING, new RedbeamsPollerRotationContext(resourceCrn, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        contexts.put(SALT_PILLAR, new SaltPillarRotationContext(resourceCrn, databaseRootPasswordSaltPillarGenerator));
        return contexts;
    }

    @Override
    public SecretType getSecret() {
        return DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD;
    }
}
