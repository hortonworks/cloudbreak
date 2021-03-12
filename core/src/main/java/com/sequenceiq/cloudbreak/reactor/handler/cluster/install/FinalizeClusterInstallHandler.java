package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class FinalizeClusterInstallHandler extends ExceptionCatcherEventHandler<FinalizeClusterInstallRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizeClusterInstallHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinalizeClusterInstallRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinalizeClusterInstallRequest> event) {
        LOGGER.error("ClusterInstallSuccessHandler step failed with the following message: {}", e.getMessage());
        return new FinalizeClusterInstallFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.finalizeClusterInstall(stackId);
            response = new FinalizeClusterInstallSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ClusterInstallSuccessHandler step failed with the following message: {}", e.getMessage());
            response = new FinalizeClusterInstallFailed(stackId, e);
        }
        return response;
    }
}
