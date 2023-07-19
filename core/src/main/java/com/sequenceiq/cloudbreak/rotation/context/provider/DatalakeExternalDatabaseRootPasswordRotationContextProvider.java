package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;

@Component
public class DatalakeExternalDatabaseRootPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private DatabaseRootPasswordSaltPillarGenerator databaseRootPasswordSaltPillarGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        contexts.put(CloudbreakSecretRotationStep.SALT_PILLAR, new SaltPillarRotationContext(resourceCrn, databaseRootPasswordSaltPillarGenerator));
        return contexts;
    }

    @Override
    public SecretType getSecret() {
        return DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
    }
}
