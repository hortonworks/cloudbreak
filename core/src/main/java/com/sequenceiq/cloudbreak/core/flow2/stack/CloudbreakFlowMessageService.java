package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@Service
public class CloudbreakFlowMessageService implements FlowMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowMessageService.class);

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void fireEventAndLog(Long stackId, String message, String eventType) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message);
    }

    public void fireEventAndLog(Long stackId, Msg msgCode, String eventType, Object... args) {
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message(msgCode, args));
    }

    public void fireInstanceGroupEventAndLog(Long stackId, Msg msgCode, String eventType, String instanceGroup, Object... args) {
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stackId, eventType, message(msgCode, args), instanceGroup);
    }

    public String message(Msg msgCode, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP].", msgCode);
        return messagesService.getMessage(msgCode.code(), Arrays.asList(args));
    }
}
