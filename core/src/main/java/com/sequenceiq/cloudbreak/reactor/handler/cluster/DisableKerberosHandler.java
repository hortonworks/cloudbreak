package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class DisableKerberosHandler implements EventHandler<DisableKerberosRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableKerberosHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DisableKerberosRequest.class);
    }

    @Override
    public void accept(Event<DisableKerberosRequest> event) {
        DisableKerberosResult result = new DisableKerberosResult(event.getData());
        try {
            LOGGER.info("Received DisableKerberosRequest event: {}", event.getData());
            StackDto stackDto = stackDtoService.getById(event.getData().getResourceId());
            ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
            clusterApi.clusterSecurityService().disableSecurity();
            LOGGER.info("Finished disabling Security");
        } catch (Exception e) {
            LOGGER.warn("An error has occured during disabling security", e);
            if (!event.getData().isForced()) {
                LOGGER.debug("Ignoring error during disabling security because forced termination flag.");
                result = new DisableKerberosResult(e.getMessage(), e, event.getData());
            }
        }
        LOGGER.info("Sending out DisableKerberosResult: {}", result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        LOGGER.info("DisableKerberosResult has been sent");
    }
}
