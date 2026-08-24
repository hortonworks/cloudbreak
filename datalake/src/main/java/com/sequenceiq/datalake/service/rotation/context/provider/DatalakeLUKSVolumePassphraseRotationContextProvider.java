package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeLUKSVolumePassphraseRotationContextProvider extends DatalakeConditionalRotationContextProvider {

    @Inject
    private SdxService sdxService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.LUKS_VOLUME_PASSPHRASE;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE);
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn));
        context.put(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, getPollingTypes().get(RotationSource.CLOUDBREAK)));
        return context;
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> {
                    String environmentCrn = sdxService.getEnvironmentCrnByResourceCrn(resourceCrn)
                            .orElseThrow(() -> new SecretRotationException("Could not find environment crn for resourceCrn: " + resourceCrn));
                    DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
                    if (!environment.isEnableSecretEncryption()) {
                        throw new SecretRotationException("Stack encryption key rotation is only available on environments with secret encryption enabled.");
                    }
                })
                .build();
    }

    @Override
    protected Function<SdxCluster, Boolean> getConditionalRotationFunction() {
        return sdxCluster -> true;
    }
}
