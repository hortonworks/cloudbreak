package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsStackHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsStackHandlerEvent> {
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
        LOGGER.error("Modify user defined tags on stack failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_STACK_PHASE", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsStackHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            Stack stack = stackService.getById(resourceId);
            stackUpdater.updateUserDefinedTags(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.selector(), resourceId, userDefinedTags);
        } catch (Exception e) {
            return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_STACK_PHASE", e);
        }
    }
}
