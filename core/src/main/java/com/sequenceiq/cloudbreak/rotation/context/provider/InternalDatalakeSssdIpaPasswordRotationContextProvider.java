package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;

@Component
public class InternalDatalakeSssdIpaPasswordRotationContextProvider extends AbstractSssdIpaPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalDatalakeSssdIpaPasswordRotationContextProvider.class);

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CloudbreakSecretRotationStep.SALT_PILLAR, new SaltPillarRotationContext(resourceCrn, this::getSssdIpaPillar));
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.INTERNAL_DATALAKE_SSSD_IPA_PASSWORD;
    }

}
