package com.sequenceiq.freeipa.service.rotation.saltpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaSaltPasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn));
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> preValidateJob(resourceCrn))
                .withRotationJob(() -> rotationJob(resourceCrn))
                .build();
    }

    private void preValidateJob(String resourceCrn) {
        Stack stack = getStack(resourceCrn);
        rotateSaltPasswordService.validateRotateSaltPassword(stack);
    }

    private void rotationJob(String resourceCrn) {
        Stack stack = getStack(resourceCrn);
        rotateSaltPasswordService.rotateSaltPassword(stack);
    }

    private Stack getStack(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        return stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(resourceCrn, environmentCrn.getAccountId());
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.SALT_PASSWORD;
    }
}
