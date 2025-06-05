package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.StackAuthenticationRepository;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

@Component
public class UserKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserKeyPairRotationContextProvider.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private StackAuthenticationRepository stackAuthenticationRepository;

    @Inject
    private UserKeyPairSaltStateRunRotationContextGenerator userKeyPairSaltStateRunRotationContextGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resourceCrn);
        DetailedEnvironmentResponse environment = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> environmentClientService.getByCrn(stack.getEnvironmentCrn()));

        boolean changedKeyPair = customerChangedTheKeyPair(stack.getStackAuthentication(), environment.getAuthentication());

        result.put(CloudbreakSecretRotationStep.SALT_STATE_RUN,
                userKeyPairSaltStateRunRotationContextGenerator.generate(changedKeyPair, stack.getResourceCrn(), stack, environment));
        result.put(CommonSecretRotationStep.CUSTOM_JOB, getCustomJobRotationContext(changedKeyPair, stack.getResourceCrn(), stack, environment));
        return result;
    }

    private CustomJobRotationContext getCustomJobRotationContext(boolean changedKeyPair, String resourceCrn, StackDto stack,
            DetailedEnvironmentResponse environment) {
        CustomJobRotationContext.CustomJobRotationContextBuilder customJobRotationContextBuilder = CustomJobRotationContext.builder();
        customJobRotationContextBuilder.withResourceCrn(resourceCrn);
        if (changedKeyPair) {
            customJobRotationContextBuilder.withRotationJob(() -> stackAuthenticationRepository
                    .save(updateAuthentication(stack.getStackAuthentication(), environment.getAuthentication())));
            customJobRotationContextBuilder.withRollbackJob(() -> stackAuthenticationRepository
                    .save(stack.getStackAuthentication()));
        }
        return customJobRotationContextBuilder.build();
    }

    private StackAuthentication updateAuthentication(StackAuthentication stackAuthentication, EnvironmentAuthenticationResponse envAuthentication) {
        stackAuthentication.setPublicKeyId(envAuthentication.getPublicKeyId());
        stackAuthentication.setPublicKey(envAuthentication.getPublicKey());
        stackAuthentication.setLoginUserName(envAuthentication.getLoginUserName());
        return stackAuthentication;
    }

    private boolean customerChangedTheKeyPair(StackAuthentication old, EnvironmentAuthenticationResponse actual) {
        if (StringUtils.isNotEmpty(old.getPublicKeyId()) && StringUtils.isNotEmpty(actual.getPublicKeyId())) {
            return !StringUtils.equals(old.getPublicKeyId(), actual.getPublicKeyId());
        } else if (StringUtils.isNotEmpty(old.getPublicKey()) && StringUtils.isNotEmpty(actual.getPublicKey())) {
            return !StringUtils.equals(old.getPublicKey(), actual.getPublicKey());
        }
        return false;
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.USER_KEYPAIR;
    }

}
