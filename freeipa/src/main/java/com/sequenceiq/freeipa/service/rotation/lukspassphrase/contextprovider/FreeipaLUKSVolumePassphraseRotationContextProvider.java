package com.sequenceiq.freeipa.service.rotation.lukspassphrase.contextprovider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.SALT_STATE_APPLY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaLUKSVolumePassphraseRotationContextProvider implements RotationContextProvider {

    private static final String TRIGGGER_LUKS_ROTATION_STATE = "rotateluks";

    private static final String FINALIZE_LUKS_ROTATION_STATE = "rotateluks/finalize";

    private static final String ROLLBACK_LUKS_ROTATION_STATE = "rotateluks/rollback";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        if (!AWS_NATIVE_GOV_VARIANT.variant().getValue().equals(stack.getPlatformvariant())) {
            throw new BadRequestException("LUKS passphrase rotation is only available on AWS Gov environments.");
        }

        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        contexts.put(CUSTOM_JOB, getCustomJobRotationContext(stack));
        contexts.put(SALT_STATE_APPLY, getSaltStateApplyRotationContext(stack));
        return contexts;
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.LUKS_VOLUME_PASSPHRASE;
    }

    private CustomJobRotationContext getCustomJobRotationContext(Stack stack) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stack.getEnvironmentCrn())
                .withPreValidateJob(() -> {
                    if (stack.getNotDeletedInstanceMetaDataSet().stream().anyMatch(Predicate.not(InstanceMetaData::isAvailable))) {
                        throw new CloudbreakRuntimeException("All instances of the stack need to be in an availabe state " +
                                "before starting the 'LUKS_VOLUME_PASSPHRASE' rotation.");
                    }
                })
                .build();
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContext(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getEnvironmentCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(stack.getNotDeletedInstanceMetaDataSet().stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of(TRIGGGER_LUKS_ROTATION_STATE))
                .withCleanupStates(List.of(FINALIZE_LUKS_ROTATION_STATE))
                .withRollbackStates(List.of(ROLLBACK_LUKS_ROTATION_STATE))
                .build();
    }
}
