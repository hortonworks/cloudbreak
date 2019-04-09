package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

@Service
public class CloudbreakFlowMessageService implements FlowMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowMessageService.class);

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void fireEventAndLog(Long stackId, String message, NotificationEventType eventType) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message);
    }

    @Override
    public void fireEventAndLog(Long stackId, Msg msgCode, NotificationEventType eventType, Object... args) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message(msgCode, args));
    }

    @Override
    public void fireInstanceGroupEventAndLog(Long stackId, Msg msgCode, NotificationEventType eventType, String instanceGroup, Object... args) {
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stackId, eventType, message(msgCode, args), instanceGroup);
    }

    @Override
    public String message(Msg msgCode, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP].", msgCode);
        return messagesService.getMessage(msgCode.code(), Arrays.asList(args));
    }
}
