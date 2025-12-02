package com.sequenceiq.freeipa.flow.stack.modify.proxy.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.proxy.ModifyProxyConfigOrchestratorService;

@Component
public class ModifyProxyConfigSaltStateApplyHandler extends ExceptionCatcherEventHandler<ModifyProxyConfigSaltStateApplyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigSaltStateApplyHandler.class);

    @Inject
    private ModifyProxyConfigOrchestratorService modifyProxyConfigOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyProxyConfigSaltStateApplyRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyProxyConfigSaltStateApplyRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new StackFailureEvent(ModifyProxyConfigEvent.MODIFY_PROXY_FAILED_EVENT.selector(), resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyProxyConfigSaltStateApplyRequest> event) {
        Long resourceId = event.getData().getResourceId();
        try {
            LOGGER.info("Applying salt state for proxy config modification on stack {}", resourceId);
            modifyProxyConfigOrchestratorService.applyModifyProxyState(resourceId);
            return new StackEvent(ModifyProxyConfigEvent.MODIFY_PROXY_SUCCESS_EVENT.selector(), resourceId);
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakRuntimeException("Failed to apply modify proxy orchestrator state: " + e.getMessage(), e);
        }
    }
}
