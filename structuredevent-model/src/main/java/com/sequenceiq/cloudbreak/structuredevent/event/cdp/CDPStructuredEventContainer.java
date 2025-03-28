package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredEventContainer {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CDPStructuredFlowEvent> flow = Collections.emptyList();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CDPStructuredRestCallEvent> rest = Collections.emptyList();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CDPStructuredNotificationEvent> notification = Collections.emptyList();

    public CDPStructuredEventContainer() {
    }

    public CDPStructuredEventContainer(
            List<CDPStructuredFlowEvent> flow,
            List<CDPStructuredRestCallEvent> rest,
            List<CDPStructuredNotificationEvent> notification) {
        this.flow = flow;
        this.rest = rest;
        this.notification = notification;
    }

    public List<CDPStructuredFlowEvent> getFlow() {
        return flow;
    }

    public void setFlow(List<CDPStructuredFlowEvent> flow) {
        this.flow = flow;
    }

    public List<CDPStructuredRestCallEvent> getRest() {
        return rest;
    }

    public void setRest(List<CDPStructuredRestCallEvent> rest) {
        this.rest = rest;
    }

    public List<CDPStructuredNotificationEvent> getNotification() {
        return notification;
    }

    public void setNotification(List<CDPStructuredNotificationEvent> notification) {
        this.notification = notification;
    }
}
