package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_DEMO_SECRET;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;

@Component
public class DatalakeDemoSecretRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDemoSecretRotationContextProvider.class);

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> LOGGER.info("This is a demo rotation, nothing will actually happen for resource {}.", resourceCrn))
                .build());
    }

    @Override
    public SecretType getSecret() {
        return INTERNAL_DATALAKE_DEMO_SECRET;
    }
}