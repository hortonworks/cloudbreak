package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.bus.Event;
@Component
public class ClusterInstallHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInstallHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().buildAmbariCluster(event.getData());
        LOGGER.info("Cluster created. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, ProvisioningContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleClusterCreationFailure(data);
    }
}
