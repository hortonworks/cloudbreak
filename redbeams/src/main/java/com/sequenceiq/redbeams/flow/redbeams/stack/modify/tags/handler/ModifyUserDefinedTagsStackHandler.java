package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.handler;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;

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
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@Component
public class ModifyUserDefinedTagsStackHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsStackHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsStackHandler.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackUpdater dbStackUpdater;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyUserDefinedTagsStackHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyUserDefinedTagsStackHandlerEvent> event) {
        LOGGER.warn("Modify user defined tags on external database stack failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsStackHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            DBStack stack = dbStackService.getById(resourceId);
            dbStackUpdater.updateUserDefinedTags(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.selector(), resourceId, userDefinedTags);
        } catch (Exception e) {
            LOGGER.warn("Modify user defined tags on external database stack failed.", e);
            return new ModifyUserDefinedTagsFailedEvent(resourceId, e);
        }
    }
}
