package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredEventContainer {

    private List<StructuredFlowEvent> flow;

    private List<StructuredRestCallEvent> rest;

    private List<StructuredNotificationEvent> notification;

    public StructuredEventContainer() {
    }

    public StructuredEventContainer(List<StructuredFlowEvent> flow, List<StructuredRestCallEvent> rest, List<StructuredNotificationEvent> notification) {
        this.flow = flow;
        this.rest = rest;
        this.notification = notification;
    }

    public List<StructuredFlowEvent> getFlow() {
        return flow;
    }

    public void setFlow(List<StructuredFlowEvent> flow) {
        this.flow = flow;
    }

    public List<StructuredRestCallEvent> getRest() {
        return rest;
    }

    public void setRest(List<StructuredRestCallEvent> rest) {
        this.rest = rest;
    }

    public List<StructuredNotificationEvent> getNotification() {
        return notification;
    }

    public void setNotification(List<StructuredNotificationEvent> notification) {
        this.notification = notification;
    }
}
