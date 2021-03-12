package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ConfigureClusterManagerManagementServicesHandler extends ExceptionCatcherEventHandler<ConfigureClusterManagerManagementServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureClusterManagerManagementServicesHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ConfigureClusterManagerManagementServicesRequest> event) {
        LOGGER.error("ConfigureClusterManagerManagementServicesHandler step failed with the following message: {}", e.getMessage());
        return new ConfigureClusterManagerManagementServicesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configureManagementServices(stackId);
            response = new ConfigureClusterManagerManagementServicesSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ConfigureClusterManagerManagementServicesHandler step failed with the following message: {}", e.getMessage());
            response = new ConfigureClusterManagerManagementServicesFailed(stackId, e);
        }
        return response;
    }
}
