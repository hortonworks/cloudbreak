package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.StackAuthenticationRepository;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext.CustomJobRotationContextBuilder;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContextGenerator;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

@Component
public class UserKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserKeyPairRotationContextProvider.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StackAuthenticationRepository stackAuthenticationRepository;

    @Inject
    private SaltRunOrchestratorStateRotationContextGenerator saltRunOrchestratorStateRotationContextGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceId) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resourceId);
        DetailedEnvironmentResponse environment = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> environmentClientService.getByCrn(stack.getEnvironmentCrn()));

        boolean changedKeyPair = customerChangedTheKeyPair(stack.getStackAuthentication(), environment.getAuthentication());

        result.put(CloudbreakSecretRotationStep.SALT_STATE_RUN,
                saltRunOrchestratorStateRotationContextGenerator.generate(changedKeyPair, stack.getResourceCrn(), stack, environment));
        result.put(CommonSecretRotationStep.CUSTOM_JOB, getCustomJobRotationContext(changedKeyPair, stack.getResourceCrn(), stack, environment));
        return result;
    }

    private CustomJobRotationContext getCustomJobRotationContext(boolean changedKeyPair, String resourceCrn, StackDto stack,
            DetailedEnvironmentResponse environment) {
        CustomJobRotationContextBuilder customJobRotationContextBuilder = CustomJobRotationContext.builder();
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
        return !old.getPublicKeyId().equals(actual.getPublicKeyId());
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.USER_KEYPAIR;
    }

}
