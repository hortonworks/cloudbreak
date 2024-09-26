package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class CBLUKSVolumePassphraseRotationContextProvider implements RotationContextProvider {

    private static final String TRIGGGER_LUKS_ROTATION_STATE = "rotateluks";

    private static final String FINALIZE_LUKS_ROTATION_STATE = "rotateluks/finalize";

    private static final String ROLLBACK_LUKS_ROTATION_STATE = "rotateluks/rollback";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn));
        context.put(SALT_STATE_APPLY, getSaltStateApplyRotationContext(resourceCrn));
        return context;
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE;
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> {
                    if (stack.getNotDeletedInstanceMetaData().stream().anyMatch(Predicate.not(InstanceMetadataView::isReachable))) {
                        throw new CloudbreakRuntimeException("All instances of the stack need to be in a reachable state " +
                                "before starting the 'LUKS_VOLUME_PASSPHRASE' rotation.");
                    }
                })
                .build();
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContext(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(stack.getNotDeletedInstanceMetaData().stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of(TRIGGGER_LUKS_ROTATION_STATE))
                .withCleanupStates(List.of(FINALIZE_LUKS_ROTATION_STATE))
                .withRollbackStates(List.of(ROLLBACK_LUKS_ROTATION_STATE))
                .build();
    }
}
