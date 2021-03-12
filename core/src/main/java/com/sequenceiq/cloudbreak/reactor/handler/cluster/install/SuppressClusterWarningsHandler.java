package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SuppressClusterWarningsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SuppressClusterWarningsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SuppressClusterWarningsSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class SuppressClusterWarningsHandler extends ExceptionCatcherEventHandler<SuppressClusterWarningsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuppressClusterWarningsHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SuppressClusterWarningsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SuppressClusterWarningsRequest> event) {
        LOGGER.error("SuppressClusterWarningsHandler step failed with the following message: {}", e.getMessage());
        return new SuppressClusterWarningsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.suppressWarnings(stackId);
            response = new SuppressClusterWarningsSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("SuppressClusterWarningsHandler step failed with the following message: {}", e.getMessage());
            response = new SuppressClusterWarningsFailed(stackId, e);
        }
        return response;
    }
}
