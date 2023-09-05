package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

public abstract class AbstractCMIntermediateCacertRotationContextProvider implements RotationContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 5;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private StackDtoService stackService;

    protected SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder getSaltStateRotationContext(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withStates(List.of("cloudera.manager.rotate.cmca-renewal"))
                .withCleanupStates(List.of("cloudera.manager.rotate.cmca-renewal-cleanup"))
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY);
    }
}
