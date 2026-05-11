package com.sequenceiq.freeipa.flow.stack.modify.tags.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component
public class ModifyUserDefinedTagsStackHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsStackHandlerEvent>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsStackHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyUserDefinedTagsStackHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyUserDefinedTagsStackHandlerEvent> event) {
        LOGGER.warn("Modify user defined tags on FreeIPA stack failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_FREEIPA_STACK_PHASE", e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsStackHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String operationId = event.getData().getOperationId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            Stack stack = stackService.getStackById(resourceId);
            stackUpdater.updateUserDefinedTags(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.selector(), resourceId, operationId, userDefinedTags);
        } catch (Exception e) {
            LOGGER.warn("Modify user defined tags on FreeIPA stack failed.", e);
            return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_FREEIPA_STACK_PHASE", e, ERROR);
        }
    }
}
