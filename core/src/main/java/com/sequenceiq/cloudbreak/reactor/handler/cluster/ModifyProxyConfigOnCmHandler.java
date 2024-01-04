package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigOnCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.cloudbreak.service.proxy.ModifyProxyConfigService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyProxyConfigOnCmHandler extends ExceptionCatcherEventHandler<ModifyProxyConfigOnCmRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigOnCmHandler.class);

    @Inject
    private ModifyProxyConfigService modifyProxyConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyProxyConfigOnCmRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyProxyConfigOnCmRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new ModifyProxyConfigFailureResponse(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyProxyConfigOnCmRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            modifyProxyConfigService.updateClusterManager(stackId);
            return new ModifyProxyConfigSuccessResponse(stackId);
        } catch (Exception e) {
            LOGGER.warn("Failed to update cluster manager with proxy config settings", e);
            return new ModifyProxyConfigFailureResponse(stackId, e);
        }
    }
}
