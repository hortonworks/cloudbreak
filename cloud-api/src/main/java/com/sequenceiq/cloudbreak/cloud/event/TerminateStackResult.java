package com.sequenceiq.cloudbreak.cloud.event;

public class TerminateStackResult {
    private String resultMsg;

    public TerminateStackResult(String resultMsg) {
        this.resultMsg = resultMsg;
    }

    public String getResultMsg() {
        return resultMsg;
    }
}
