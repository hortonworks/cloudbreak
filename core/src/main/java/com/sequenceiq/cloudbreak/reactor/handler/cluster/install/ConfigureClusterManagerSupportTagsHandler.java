package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerSupportTagsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerSupportTagsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerSupportTagsSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ConfigureClusterManagerSupportTagsHandler extends ExceptionCatcherEventHandler<ConfigureClusterManagerSupportTagsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureClusterManagerSupportTagsHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConfigureClusterManagerSupportTagsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ConfigureClusterManagerSupportTagsRequest> event) {
        LOGGER.error("ConfigureClusterManagerSupportTagsHandler step failed with the following message: {}", e.getMessage());
        return new ConfigureClusterManagerSupportTagsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configureSupportTags(stackId);
            response = new ConfigureClusterManagerSupportTagsSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ConfigureClusterManagerSupportTagsHandler step failed with the following message: {}", e.getMessage());
            response = new ConfigureClusterManagerSupportTagsFailed(stackId, e);
        }
        return response;
    }
}
