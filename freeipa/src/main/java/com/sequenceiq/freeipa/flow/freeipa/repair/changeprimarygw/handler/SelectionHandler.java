package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionSuccess;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SelectionHandler implements EventHandler<ChangePrimaryGatewaySelectionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectionHandler.class);

    @Inject
    private ChangePrimaryGatewayService changePrimaryGatewayService;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ChangePrimaryGatewaySelectionRequest.class);
    }

    @Override
    public void accept(Event<ChangePrimaryGatewaySelectionRequest> changePrimaryGatewaySelectionRequestEvent) {
        ChangePrimaryGatewaySelectionRequest request = changePrimaryGatewaySelectionRequestEvent.getData();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Optional<String> formerPrimaryGatewayInstanceId = changePrimaryGatewayService.getPrimaryGatewayInstanceId(stack);
            String newPrimaryGatewayInstanceId = changePrimaryGatewayService.selectNewPrimaryGatewayInstanceId(stack, request.getRepairInstanceIds());
            result = new ChangePrimaryGatewaySelectionSuccess(request.getResourceId(), formerPrimaryGatewayInstanceId, newPrimaryGatewayInstanceId);
        } catch (Exception e) {
            LOGGER.error("Failed to select the new primary gateway", e);
            result = new ChangePrimaryGatewayFailureEvent(request.getResourceId(), "Selecting primary gateway", Set.of(), Map.of(), e);
        }
        eventBus.notify(result.selector(), new Event<>(changePrimaryGatewaySelectionRequestEvent.getHeaders(), result));
    }
}
