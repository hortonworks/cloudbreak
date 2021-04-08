package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterManagerConfigureKerberosHandler extends ExceptionCatcherEventHandler<ClusterManagerConfigureKerberosRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerConfigureKerberosHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerConfigureKerberosRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterManagerConfigureKerberosRequest> event) {
        LOGGER.error("ClusterManagerConfigureKerberosHandler step failed with the following message: {}", e.getMessage());
        return new ClusterManagerConfigureKerberosFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterManagerConfigureKerberosRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configureKerberos(stackId);
            response = new ClusterManagerConfigureKerberosSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ClusterManagerConfigureKerberosHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerConfigureKerberosFailed(stackId, e);
        }
        return response;
    }
}
