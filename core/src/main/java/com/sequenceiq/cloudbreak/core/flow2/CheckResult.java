package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class CheckResult {

    private final EventStatus status;

    private String message;

    public CheckResult(EventStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    private CheckResult(EventStatus status) {
        this.status = status;
    }

    public static CheckResult ok() {
        return new CheckResult(EventStatus.OK);
    }

    public static CheckResult failed(String message) {
        return new CheckResult(EventStatus.FAILED, message);
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
