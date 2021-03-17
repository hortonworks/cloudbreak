package com.sequenceiq.environment.environment.flow;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.store.EnvironmentStatusUpdater;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("EnvironmentFillInMemoryStateStoreRestartAction")
public class EnvironmentFillInMemoryStateStoreRestartAction extends DefaultRestartAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentFillInMemoryStateStoreRestartAction.class);

    @Inject
    private EnvironmentService environmentService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        LOGGER.debug("Restoring MDC context and InMemoryStateStore entry for flow: '{}', flow chain: '{}', event: '{}'", flowParameters.getFlowId(),
                flowChainId, event);
        Payload envPayload = (Payload) payload;
        Optional<Environment> environment = environmentService.getById(envPayload.getResourceId());
        environment.ifPresent(env -> restart(flowParameters, flowChainId, event, payload, env));
    }

    protected void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload, Environment environment) {
        EnvironmentStatusUpdater.update(environment.getId(), environment.getStatus());
        MDCBuilder.buildMdcContext(environment);
        MDCBuilder.addFlowId(flowParameters.getFlowId());
        LOGGER.debug("MDC context and InMemoryStateStore entry have been restored for flow: '{}', flow chain: '{}', event: '{}'", flowParameters.getFlowId(),
                flowChainId, event);
        super.restart(flowParameters, flowChainId, event, payload);
    }
}
