package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredEventContainer {

    private List<CDPStructuredFlowEvent> flow;

    private List<CDPStructuredRestCallEvent> rest;

    private List<CDPStructuredNotificationEvent> notification;

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
        return flow != null ? flow : Collections.emptyList();
    }

    public void setFlow(List<CDPStructuredFlowEvent> flow) {
        this.flow = flow;
    }

    public List<CDPStructuredRestCallEvent> getRest() {
        return rest != null ? rest : Collections.emptyList();
    }

    public void setRest(List<CDPStructuredRestCallEvent> rest) {
        this.rest = rest;
    }

    public List<CDPStructuredNotificationEvent> getNotification() {
        return notification != null ? notification : Collections.emptyList();
    }

    public void setNotification(List<CDPStructuredNotificationEvent> notification) {
        this.notification = notification;
    }
}
