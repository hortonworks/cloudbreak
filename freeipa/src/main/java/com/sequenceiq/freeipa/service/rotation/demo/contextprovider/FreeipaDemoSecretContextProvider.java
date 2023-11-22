package com.sequenceiq.freeipa.service.rotation.demo.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_DEMO_SECRET;

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
public class FreeipaDemoSecretContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaDemoSecretContextProvider.class);

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> LOGGER.info("This is a demo rotation, nothing will actually happen for resource {}.", resourceCrn))
                .build());
    }

    @Override
    public SecretType getSecret() {
        return FREEIPA_DEMO_SECRET;
    }
}