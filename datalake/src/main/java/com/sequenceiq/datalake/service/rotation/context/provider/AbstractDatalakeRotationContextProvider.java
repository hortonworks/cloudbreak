package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

public abstract class AbstractDatalakeRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, getPollingTypes().get(RotationSource.CLOUDBREAK)));
    }
}
