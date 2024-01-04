package com.sequenceiq.environment.environment.flow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.store.EnvironmentStatusUpdater;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("EnvironmentFillInMemoryStateStoreRestartAction")
public class EnvironmentFillInMemoryStateStoreRestartAction extends DefaultRestartAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentFillInMemoryStateStoreRestartAction.class);

    @Inject
    private EnvironmentService environmentService;

    @Override
    public void restart(RestartContext restartContext, Object payload) {
        LOGGER.debug("Restoring MDC context and InMemoryStateStore entry for flow: '{}', flow chain: '{}', event: '{}'", restartContext.getFlowId(),
                restartContext.getFlowChainId(), restartContext.getEvent());
        environmentService.getById(restartContext.getResourceId()).ifPresent(environment -> {
            EnvironmentStatusUpdater.update(environment.getId(), environment.getStatus());
            MDCBuilder.buildMdcContext(environment);
            MDCBuilder.addFlowId(restartContext.getFlowId());
            LOGGER.debug("MDC context and InMemoryStateStore entry have been restored for flow: '{}', flow chain: '{}', event: '{}'",
                    restartContext.getFlowId(), restartContext.getFlowChainId(), restartContext.getEvent());
            super.restart(restartContext, payload);
        });
    }
}
