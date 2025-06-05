package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CBLUKSVolumePassphraseRotationContextProvider implements RotationContextProvider {

    private static final String TRIGGGER_LUKS_ROTATION_STATE = "rotateluks";

    private static final String FINALIZE_LUKS_ROTATION_STATE = "rotateluks/finalize";

    private static final String ROLLBACK_LUKS_ROTATION_STATE = "rotateluks/rollback";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        if (!AWS_NATIVE_GOV_VARIANT.variant().getValue().equals(stack.getPlatformVariant())) {
            throw new BadRequestException("LUKS passphrase rotation is only available on AWS Gov environments.");
        }

        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(CUSTOM_JOB, getCustomJobRotationContext(stack));
        context.put(SALT_STATE_APPLY, getSaltStateApplyRotationContext(stack));
        return context;
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE;
    }

    private CustomJobRotationContext getCustomJobRotationContext(StackDto stack) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withPreValidateJob(() -> {
                    DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                    if (!environment.isEnableSecretEncryption()) {
                        throw new SecretRotationException("LUKS passphrase rotation is only available on environments with secret encryption enabled.");
                    }
                    if (stack.getNotDeletedInstanceMetaData().stream().anyMatch(Predicate.not(InstanceMetadataView::isReachable))) {
                        throw new SecretRotationException("All instances of the stack need to be in a reachable state " +
                                "before starting the 'LUKS_VOLUME_PASSPHRASE' rotation.");
                    }
                })
                .build();
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContext(StackDto stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(stack.getNotDeletedInstanceMetaData().stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of(TRIGGGER_LUKS_ROTATION_STATE))
                .withCleanupStates(List.of(FINALIZE_LUKS_ROTATION_STATE))
                .withRollbackStates(List.of(ROLLBACK_LUKS_ROTATION_STATE))
                .build();
    }
}
