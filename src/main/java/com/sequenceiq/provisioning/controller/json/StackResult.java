package com.sequenceiq.provisioning.controller.json;

public class StackResult {

    private String status;

    public StackResult(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
