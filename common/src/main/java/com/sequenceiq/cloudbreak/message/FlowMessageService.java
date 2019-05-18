package com.sequenceiq.cloudbreak.message;

public interface FlowMessageService {
    void fireEventAndLog(Long stackId, String message, String eventType);

    void fireEventAndLog(Long stackId, Msg msgCode, String eventType, Object... args);

    void fireInstanceGroupEventAndLog(Long stackId, Msg msgCode, String eventType, String instanceGroup, Object... args);

    String message(Msg msgCode, Object... args);

}
