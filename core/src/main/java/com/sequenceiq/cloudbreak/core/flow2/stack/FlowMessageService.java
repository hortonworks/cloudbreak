package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Service
public class FlowMessageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowMessageService.class);

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void fireEventAndLog(Long stackId, Msg msgCode, String eventType, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP].", msgCode);
        String message = messagesService.getMessage(msgCode.code(), Arrays.asList(args));
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message);
    }
}
