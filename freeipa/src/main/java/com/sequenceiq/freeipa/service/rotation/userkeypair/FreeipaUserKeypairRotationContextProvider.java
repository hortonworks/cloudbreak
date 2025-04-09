package com.sequenceiq.freeipa.service.rotation.userkeypair;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.repository.StackAuthenticationRepository;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaUserKeypairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaUserKeypairRotationContextProvider.class);

    @Inject
    private CachedEnvironmentClientService environmentClientService;

    @Inject
    private StackAuthenticationRepository stackAuthenticationRepository;

    @Inject
    private StackService stackService;

    @Inject
    private UserKeyPairSaltStateRunRotationContextGenerator userKeyPairSaltStateRunRotationContextGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        DetailedEnvironmentResponse environment = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> environmentClientService.getByCrn(resourceCrn));

        boolean changedKeyPair = customerChangedTheKeyPair(stack.getStackAuthentication(), environment.getAuthentication());

        result.put(FreeIpaSecretRotationStep.SALT_STATE_RUN,
                userKeyPairSaltStateRunRotationContextGenerator.generate(changedKeyPair, stack.getResourceCrn(), stack, environment));
        result.put(CommonSecretRotationStep.CUSTOM_JOB, getCustomJobRotationContext(changedKeyPair, stack.getResourceCrn(), stack, environment));
        return result;
    }

    private CustomJobRotationContext getCustomJobRotationContext(boolean changedKeyPair, String resourceCrn, Stack stack,
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
        return FreeIpaSecretType.USER_KEYPAIR;
    }

}
