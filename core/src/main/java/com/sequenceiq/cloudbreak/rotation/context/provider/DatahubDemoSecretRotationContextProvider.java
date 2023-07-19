package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_DEMO_SECRET;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.sdx.rotation.DatalakeMultiSecretType;

@Component
public class DatahubDemoSecretRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubDemoSecretRotationContextProvider.class);

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
                        .withResourceCrn(resourceCrn)
                        .withRotationJob(() -> LOGGER.info("This is a demo rotation, nothing will actually happen for resource {}.", resourceCrn))
                .build());
    }

    @Override
    public SecretType getSecret() {
        return DATAHUB_DEMO_SECRET;
    }

    @Override
    public Optional<MultiSecretType> getMultiSecret() {
        return Optional.of(DatalakeMultiSecretType.DEMO_MULTI_SECRET);
    }
}