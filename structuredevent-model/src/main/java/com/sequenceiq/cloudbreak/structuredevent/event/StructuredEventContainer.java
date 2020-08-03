package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        return flow != null ? flow : Collections.emptyList();
    }

    public void setFlow(List<StructuredFlowEvent> flow) {
        this.flow = flow;
    }

    public List<StructuredRestCallEvent> getRest() {
        return rest != null ? rest : Collections.emptyList();
    }

    public void setRest(List<StructuredRestCallEvent> rest) {
        this.rest = rest;
    }

    public List<StructuredNotificationEvent> getNotification() {
        return notification != null ? notification : Collections.emptyList();
    }

    public void setNotification(List<StructuredNotificationEvent> notification) {
        this.notification = notification;
    }
}
