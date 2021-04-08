package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ValidateClusterLicenceFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ValidateClusterLicenceRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ValidateClusterLicenceSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ValidateClusterLicenceHandler extends ExceptionCatcherEventHandler<ValidateClusterLicenceRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateClusterLicenceHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateClusterLicenceRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateClusterLicenceRequest> event) {
        LOGGER.error("ValidateClusterLicenceHandler step failed with the following message: {}", e.getMessage());
        return new ValidateClusterLicenceFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateClusterLicenceRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.validateLicence(stackId);
            response = new ValidateClusterLicenceSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ValidateClusterLicenceHandler step failed with the following message: {}", e.getMessage());
            response = new ValidateClusterLicenceFailed(stackId, e);
        }
        return response;
    }
}
