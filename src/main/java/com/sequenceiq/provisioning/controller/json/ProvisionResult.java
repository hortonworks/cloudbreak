package com.sequenceiq.provisioning.controller.json;

public class ProvisionResult {

    private String status;

    public ProvisionResult(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
