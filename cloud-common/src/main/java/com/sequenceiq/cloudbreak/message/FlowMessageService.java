package com.sequenceiq.cloudbreak.message;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;

public interface FlowMessageService {
    void fireEventAndLog(Long stackId, String message, NotificationEventType eventType);

    void fireEventAndLog(Long stackId, Msg msgCode, NotificationEventType eventType, Object... args);

    void fireInstanceGroupEventAndLog(Long stackId, Msg msgCode, NotificationEventType eventType, String instanceGroup, Object... args);

    String message(Msg msgCode, Object... args);

}
