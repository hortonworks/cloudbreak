package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction;

@Component("CheckImageAfterUpdateAction")
public class CheckImageAfterUpdateAction extends CheckImageAction {

    @Override
    protected FlowEvent getFinishedEvent() {
        return StackImageUpdateEvent.IMAGE_COPY_FINISHED_EVENT;
    }

    @Override
    protected FlowEvent getRepeatEvent() {
        return StackImageUpdateEvent.IMAGE_COPY_CHECK_EVENT;
    }
}
