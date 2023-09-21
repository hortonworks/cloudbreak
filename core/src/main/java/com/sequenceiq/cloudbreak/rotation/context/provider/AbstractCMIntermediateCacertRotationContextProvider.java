package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

public abstract class AbstractCMIntermediateCacertRotationContextProvider implements RotationContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 5;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private StackDtoService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        return Map.of(SALT_STATE_APPLY, getSaltStateRotationContext(stackDto),
                CUSTOM_JOB, getCustomJobRotationContext(stackDto));
    }

    private SaltStateApplyRotationContext getSaltStateRotationContext(StackDto stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<String> states = Lists.newArrayList();
        List<String> cleanupStates = Lists.newArrayList();
        if (stack.getCluster().getAutoTlsEnabled()) {
            states.addAll(List.of("cloudera.manager.server-stop", "cloudera.manager.rotate.cmca-renewal", "cloudera.manager.server-start"));
            cleanupStates.add("cloudera.manager.rotate.cmca-renewal-cleanup");
        }
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withStates(states)
                .withCleanupStates(cleanupStates)
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY)
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(StackDto stackDto) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stackDto.getResourceCrn())
                .withRotationJob(() -> waitForClouderaManagerToStartup(stackDto))
                .build();
    }

    private void waitForClouderaManagerToStartup(StackDto stack) {
        try {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.clusterSetupService().waitForServer(false);
        } catch (Exception e) {
            throw new SecretRotationException(e);
        }
    }
}
