package com.sequenceiq.provisioning.controller.json;

public class ProvisionResultJson {

    private String status;

    public ProvisionResultJson(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
