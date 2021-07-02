package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class PrepareDatalakeResourceHandler extends ExceptionCatcherEventHandler<PrepareDatalakeResourceRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDatalakeResourceHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareDatalakeResourceRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareDatalakeResourceRequest> event) {
        LOGGER.error("PrepareDatalakeResourceHandler step failed with the following message: {}", e.getMessage());
        return new PrepareDatalakeResourceFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareDatalakeResourceRequest> event) {
        Long stackId = event.getData().getResourceId();
        LOGGER.debug("Deprecated due to datalakeresource entity retirement");
        return new PrepareDatalakeResourceSuccess(stackId);
    }
}
