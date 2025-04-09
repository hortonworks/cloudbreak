package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.LDAP_BIND_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext.CustomJobRotationContextBuilder;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;

@Component
public class ClusterLdapBindPasswordContextProvider implements RotationContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 3;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = new HashMap<>();
        StackDto stack = stackDtoService.getByCrnWithResources(resourceCrn);

        result.put(FREEIPA_ROTATE_POLLING, new PollerRotationContext(resourceCrn, FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD,
                Map.of(CLUSTER_NAME.name(), stack.getName())));
        result.put(CUSTOM_JOB, getCustomJobRotationContext(stack.getResourceCrn(), stack));
        result.put(SALT_STATE_APPLY, getSaltStateApplyRotationContextBuilder(stack));
        return result;
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContextBuilder(StackDto stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY)
                .withStates(List.of("cloudera.manager.server-stop",
                        "cloudera.manager.rotate.rotate-ldap-secrets",
                        "cloudera.manager.server-start"))
                .withRollbackStates(List.of("cloudera.manager.server-stop",
                        "cloudera.manager.rotate.rotate-ldap-secrets",
                        "cloudera.manager.server-start"))
                .build();
    }

    private RotationContext getCustomJobRotationContext(String resourceCrn, StackDto stackDto) {
        CustomJobRotationContextBuilder customJobRotationContextBuilder = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> {
                    clusterHostServiceRunner.updateClusterConfigs(stackDto);
                })
                .withRollbackJob(() -> {
                    clusterHostServiceRunner.updateClusterConfigs(stackDto);
                });
        return customJobRotationContextBuilder.build();
    }

    @Override
    public SecretType getSecret() {
        return LDAP_BIND_PASSWORD;
    }

}
