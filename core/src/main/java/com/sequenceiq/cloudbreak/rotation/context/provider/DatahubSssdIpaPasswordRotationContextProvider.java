package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatahubSssdIpaPasswordRotationContextProvider extends AbstractSssdIpaPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubSssdIpaPasswordRotationContextProvider.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackDtoService.getByCrn(resourceCrn);
        PollerRotationContext pollerRotationContext = new PollerRotationContext(resourceCrn, FREEIPA_KERBEROS_BIND_USER,
                Map.of(CLUSTER_NAME.name(), stackDto.getName()));
        return Map.of(CommonSecretRotationStep.FREEIPA_ROTATE_POLLING, pollerRotationContext,
                CloudbreakSecretRotationStep.SALT_PILLAR, new SaltPillarRotationContext(resourceCrn, this::getSssdIpaPillar));
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.DATAHUB_SSSD_IPA_PASSWORD;
    }

}
