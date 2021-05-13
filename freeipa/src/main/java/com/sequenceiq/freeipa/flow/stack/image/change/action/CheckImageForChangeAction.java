package com.sequenceiq.freeipa.flow.stack.image.change.action;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.flow.stack.provision.action.CheckImageAction;

@Component("CheckImageForChangeAction")
public class CheckImageForChangeAction extends CheckImageAction {
    @Override
    protected FlowEvent getFinishedEvent() {
        return ImageChangeEvents.IMAGE_COPY_FINISHED_EVENT;
    }

    @Override
    protected FlowEvent getRepeatEvent() {
        return ImageChangeEvents.IMAGE_COPY_CHECK_EVENT;
    }
}
