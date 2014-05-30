package com.sequenceiq.provisioning.controller.json;

import com.sequenceiq.provisioning.domain.StatusRequest;

public class StatusRequestJson implements JsonEntity {

    private StatusRequest statusRequest;

    public StatusRequestJson() {

    }

    public StatusRequest getStatusRequest() {
        return statusRequest;
    }

    public void setStatusRequest(StatusRequest statusRequest) {
        this.statusRequest = statusRequest;
    }
}
