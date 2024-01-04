package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ConfigurePolicyFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ConfigurePolicyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ConfigurePolicySuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ConfigurePolicyHandler implements EventHandler<ConfigurePolicyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurePolicyHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConfigurePolicyRequest.class);
    }

    @Override
    public void accept(Event<ConfigurePolicyRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configurePolicy(stackId);
            response = new ConfigurePolicySuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to public policy Cloudera Manager cluster: {}", e.getMessage());
            response = new ConfigurePolicyFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
