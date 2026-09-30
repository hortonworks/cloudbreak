package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationSupport;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

abstract class ModifyUserDefinedTagsOnStacksHandler extends ExceptionCatcherEventHandler<EnvTagsModificationEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final StackPollerService stackPollerService;

    ModifyUserDefinedTagsOnStacksHandler(StackPollerService stackPollerService) {
        this.stackPollerService = stackPollerService;
    }

    protected abstract StackType stackType();

    protected abstract EnvironmentStatus failureStatus();

    protected abstract String nextSelector();

    protected abstract String failureLogMessage();

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        EnvTagsModificationEvent data = event.getData();
        try {
            stackPollerService.modifyUserDefinedTagsOnStacks(
                    data.getResourceId(),
                    data.getResourceCrn(),
                    stackType(),
                    data.getUserDefinedTags(),
                    data.getTagsToRemove());
        } catch (Exception e) {
            logger.warn(failureLogMessage(), e);
            return failureEvent(data, e);
        }
        return EnvTagsModificationSupport.nextEvent(data, nextSelector());
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        logger.warn(failureLogMessage(), e);
        return failureEvent(event.getData(), e);
    }

    private EnvTagsModificationFailureEvent failureEvent(EnvTagsModificationEvent data, Exception e) {
        return new EnvTagsModificationFailureEvent(
                data.getResourceId(), data.getResourceName(), data.getResourceCrn(), failureStatus(), e);
    }
}
