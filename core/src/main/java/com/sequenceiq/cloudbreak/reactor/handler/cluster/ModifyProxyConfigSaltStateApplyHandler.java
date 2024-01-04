package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.cloudbreak.service.proxy.ModifyProxyConfigService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyProxyConfigSaltStateApplyHandler extends ExceptionCatcherEventHandler<ModifyProxyConfigSaltStateApplyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigSaltStateApplyHandler.class);

    @Inject
    private ModifyProxyConfigService modifyProxyConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyProxyConfigSaltStateApplyRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyProxyConfigSaltStateApplyRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new ModifyProxyConfigFailureResponse(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyProxyConfigSaltStateApplyRequest> event) {
        ModifyProxyConfigSaltStateApplyRequest eventData = event.getData();
        Long stackId = eventData.getResourceId();
        String previousProxyConfigCrn = eventData.getPreviousProxyConfigCrn();
        try {
            LOGGER.info("Applying salt state for proxy config modification on stack {}", stackId);
            modifyProxyConfigService.applyModifyProxyState(stackId);
            return new ModifyProxyConfigRequest(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_ON_CM.event(), stackId, previousProxyConfigCrn);
        } catch (Exception e) {
            LOGGER.warn("Failed to modify proxy config", e);
            return new ModifyProxyConfigFailureResponse(stackId, e);
        }
    }
}
