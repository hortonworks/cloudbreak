package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class SaltPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPasswordRotationContextProvider.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        Map<String, String> newSecretMap = Map.of(saltSecurityConfig.getSaltPasswordSecret(), PasswordUtil.generatePassword());
        return Map.of(VAULT, VaultRotationContext.builder()
                        .withResourceCrn(resourceCrn)
                        .withNewSecretMap(newSecretMap)
                        .withEntitySaverList(List.of(() -> saltSecurityConfigService.save(saltSecurityConfig)))
                        .withEntitySecretFieldUpdaterMap(Map.of(saltSecurityConfig.getSaltPasswordSecret(),
                                vaultSecretJson -> saltSecurityConfig.setSaltPasswordSecret(new SecretProxy(vaultSecretJson))))
                        .build(),
                CUSTOM_JOB, getCustomJobRotationContext(stack));
    }

    private CustomJobRotationContext getCustomJobRotationContext(StackDto stack) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withPreValidateJob(() -> rotateSaltPasswordValidator.validateRotateSaltPassword(stack))
                .withRotationJob(() -> {
                    try {
                        rotateSaltPasswordService.rotateSaltPassword(stack);
                    } catch (CloudbreakOrchestratorException e) {
                        throw new SecretRotationException(e);
                    }
                })
                .withPostValidateJob(() -> {
                    rotateSaltPasswordService.validatePasswordAfterRotation(stack);
                    secretRotationSaltService.validateSalt(stack);
                })
                .build();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.SALT_PASSWORD;
    }
}
